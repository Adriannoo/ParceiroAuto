package br.edu.uniamerica.parceiro_auto;

import br.edu.uniamerica.parceiro_auto.controller.dto.transaction.TransactionRequestDTO;
import br.edu.uniamerica.parceiro_auto.entity.*;
import br.edu.uniamerica.parceiro_auto.entity.enums.*;
import br.edu.uniamerica.parceiro_auto.exception.ResourceNotFoundException;
import br.edu.uniamerica.parceiro_auto.repository.RecurrenceRuleRepository;
import br.edu.uniamerica.parceiro_auto.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

// Verifica a coordenacao dos services; o rollback real permanece em TransactionAtomicityTest.
class TransactionApplicationServiceTest {
    private final TransactionService transactions = mock(TransactionService.class);
    private final CompanyService companies = mock(CompanyService.class);
    private final BankAccountService accounts = mock(BankAccountService.class);
    private final TransactionCategoryService categories = mock(TransactionCategoryService.class);
    private final RecurrenceRuleService recurrences = mock(RecurrenceRuleService.class);
    private final RecurrenceRuleRepository repository = mock(RecurrenceRuleRepository.class);
    private final TransactionApplicationService service = new TransactionApplicationService(
            transactions, companies, accounts, categories, recurrences, repository);

    @Test
    void createsTransactionWithProvidedDateAndSavesRecurrence() {
        // Arrange
        Transaction expected = transaction();
        LocalDate date = LocalDate.of(2026, 10, 1);
        when(companies.findById(1L)).thenReturn(Optional.of(expected.getCompany()));
        when(accounts.findById(2L)).thenReturn(expected.getBankAccount());
        when(categories.findById(3L)).thenReturn(expected.getTransactionCategory());
        when(transactions.createTransaction(expected.getCompany(), expected.getBankAccount(), expected.getTransactionCategory(),
                TransactionType.ENTRADA, "Venda", BigDecimal.TEN, TransactionMethod.PIX, date)).thenReturn(expected);

        // Act
        var result = service.create(dto(date, RecurrenceFrequency.MONTHLY, date.plusMonths(2)));

        // Assert
        assertThat(result.id()).isEqualTo(20L);
        assertThat(result.companyId()).isEqualTo(1L);
        var order = inOrder(transactions, recurrences);
        order.verify(transactions).createTransaction(expected.getCompany(), expected.getBankAccount(), expected.getTransactionCategory(),
                TransactionType.ENTRADA, "Venda", BigDecimal.TEN, TransactionMethod.PIX, date);
        order.verify(recurrences).saveForTransaction(expected, RecurrenceFrequency.MONTHLY, date.plusMonths(2));
    }

    @Test
    void rejectsCreationWhenCompanyDoesNotExist() {
        // Arrange
        when(companies.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.create(dto(null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Empresa nao encontrada");
        verifyNoInteractions(accounts, categories, transactions, recurrences);
    }

    @Test
    void updatesTransactionWithExplicitDateAndPreservesCompany() {
        // Arrange
        Transaction existing = transaction();
        LocalDate date = LocalDate.of(2026, 10, 2);
        when(transactions.findById(20L)).thenReturn(existing);
        when(accounts.findById(2L)).thenReturn(existing.getBankAccount());
        when(categories.findById(3L)).thenReturn(existing.getTransactionCategory());
        when(transactions.updateTransaction(existing, existing.getBankAccount(), existing.getTransactionCategory(),
                TransactionType.ENTRADA, "Venda", BigDecimal.TEN, TransactionMethod.PIX, date)).thenReturn(existing);

        // Act
        var result = service.update(20L, dto(date, null, null));

        // Assert
        assertThat(result.companyId()).isEqualTo(1L);
        verify(transactions).updateTransaction(existing, existing.getBankAccount(), existing.getTransactionCategory(),
                TransactionType.ENTRADA, "Venda", BigDecimal.TEN, TransactionMethod.PIX, date);
        verify(recurrences).saveForTransaction(existing, null, null);
        verifyNoInteractions(companies);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rejectsEndDateWithoutFrequencyBeforeChangingData(boolean updating) {
        // Arrange: testa a mesma regra na criacao e na atualizacao.
        LocalDate date = LocalDate.of(2026, 10, 1);
        TransactionRequestDTO invalid = dto(date, null, date.plusDays(1));
        if (updating) when(transactions.findById(20L)).thenReturn(transaction());

        // Act & Assert
        assertThatThrownBy(() -> {
            if (updating) service.update(20L, invalid);
            else service.create(invalid);
        }).isInstanceOfSatisfying(ResponseStatusException.class,
                error -> assertThat(error.getStatusCode().value()).isEqualTo(400));
        verify(transactions, never()).updateTransaction(any(), any(), any(), any(), any(), any(), any(), any());
        verifyNoInteractions(accounts, categories, recurrences);
    }

    @Test
    void rejectsEndDateBeforeTransactionDateBeforeLookingUpCompany() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 10, 1);

        // Act & Assert
        assertThatThrownBy(() -> service.create(dto(date, RecurrenceFrequency.DAILY, date.minusDays(1))))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getReason()).isEqualTo("A data final nao pode ser anterior a data da transacao"));
        verifyNoInteractions(transactions, companies, accounts, categories, recurrences);
    }

    private static TransactionRequestDTO dto(LocalDate date, RecurrenceFrequency frequency, LocalDate end) {
        return new TransactionRequestDTO(1L, 2L, 3L, TransactionType.ENTRADA, "Venda", BigDecimal.TEN,
                TransactionMethod.PIX, date, frequency, end);
    }

    private static Transaction transaction() {
        Company company = new Company();
        company.setId(1L);
        BankAccount account = new BankAccount();
        account.setId(2L);
        TransactionCategory category = new TransactionCategory();
        category.setId(3L);
        Transaction transaction = new Transaction();
        transaction.setId(20L);
        transaction.setCompany(company);
        transaction.setBankAccount(account);
        transaction.setTransactionCategory(category);
        transaction.setDate(LocalDate.of(2026, 10, 1));
        return transaction;
    }
}
