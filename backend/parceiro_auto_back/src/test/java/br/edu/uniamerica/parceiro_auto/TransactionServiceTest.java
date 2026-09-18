package br.edu.uniamerica.parceiro_auto;

import br.edu.uniamerica.parceiro_auto.entity.*;
import br.edu.uniamerica.parceiro_auto.entity.enums.*;
import br.edu.uniamerica.parceiro_auto.repository.TransactionRepository;
import br.edu.uniamerica.parceiro_auto.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionServiceTest {
    private final TransactionRepository repository = mock(TransactionRepository.class);
    private final TransactionService service = new TransactionService(repository);
    private Company company;
    private BankAccount account;
    private TransactionCategory category;

    @BeforeEach
    void setup() {
        company = new Company();
        company.setId(1L);
        account = new BankAccount();
        account.setCompany(company);
        account.setBalance(new BigDecimal("100.00"));
        category = new TransactionCategory();
        category.setCompany(company);
        category.setType(TransactionType.ENTRADA);
        category.setActive(true);
    }

    // Rejeita empresa, conta ou categoria sem vinculo valido antes de mexer no saldo.
    @ParameterizedTest
    @ValueSource(strings = {"account", "category", "companyId", "accountCompany", "categoryCompany", "inactive"})
    void rejectsInvalidRelationsOnCreation(String invalid) {
        invalidate(invalid);
        assertThatThrownBy(() -> service.createTransaction(company, account, category,
                TransactionType.ENTRADA, "Teste", BigDecimal.TEN, TransactionMethod.PIX))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"account", "category", "companyId", "accountCompany", "categoryCompany", "inactive"})
    void rejectsInvalidRelationsOnUpdate(String invalid) {
        BankAccount originalAccount = new BankAccount();
        originalAccount.setCompany(company);
        originalAccount.setBalance(new BigDecimal("200.00"));
        Transaction transaction = new Transaction();
        transaction.setCompany(company);
        transaction.setBankAccount(originalAccount);
        transaction.setValue(BigDecimal.TEN);
        transaction.setType(TransactionType.ENTRADA);
        transaction.setDate(LocalDate.now());
        invalidate(invalid);

        assertThatThrownBy(() -> service.updateTransaction(transaction, account, category,
                TransactionType.ENTRADA, "Teste", BigDecimal.TEN, TransactionMethod.PIX))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(originalAccount.getBalance()).isEqualByComparingTo("200.00");
        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 101})
    void rejectsInvalidLimits(int limit) {
        assertThatThrownBy(() -> service.findLastByCompany(company, limit))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode().value()).isEqualTo(400));
        verifyNoInteractions(repository);
    }

    @Test
    void requestsOnlyTheFirstPage() {
        when(repository.findByCompanyOrderByDateDescIdDesc(company, PageRequest.of(0, 2)))
                .thenReturn(List.of());
        assertThat(service.findLastByCompany(company, 2)).isEmpty();
        verify(repository).findByCompanyOrderByDateDescIdDesc(company, PageRequest.of(0, 2));
        verifyNoMoreInteractions(repository);
    }

    private void invalidate(String invalid) {
        Company other = new Company();
        other.setId(2L);
        switch (invalid) {
            case "account" -> account.setCompany(other);
            case "category" -> category.setCompany(other);
            case "companyId" -> company.setId(null);
            case "accountCompany" -> account.setCompany(null);
            case "categoryCompany" -> category.setCompany(null);
            case "inactive" -> category.setActive(false);
            default -> throw new IllegalArgumentException(invalid);
        }
    }
}
