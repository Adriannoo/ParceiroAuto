package br.edu.uniamerica.parceiro_auto;

import br.edu.uniamerica.parceiro_auto.entity.*;
import br.edu.uniamerica.parceiro_auto.entity.enums.*;
import br.edu.uniamerica.parceiro_auto.exception.ResourceNotFoundException;
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
import java.util.Optional;

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

    @Test
    void createsIncomeAndIncreasesAccountBalance() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 10, 20);
        when(repository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            saved.setId(31L);
            return saved;
        });

        // Act
        Transaction result = service.createTransaction(company, account, category, TransactionType.ENTRADA,
                "  Venda  ", new BigDecimal("25.50"), TransactionMethod.PIX, date);

        // Assert
        assertThat(result.getId()).isEqualTo(31L);
        assertThat(result.getCompany()).isSameAs(company);
        assertThat(result.getBankAccount()).isSameAs(account);
        assertThat(result.getTransactionCategory()).isSameAs(category);
        assertThat(result.getDescription()).isEqualTo("Venda");
        assertThat(result.getValue()).isEqualByComparingTo("25.50");
        assertThat(result.getDate()).isEqualTo(date);
        assertThat(result.getMethod()).isEqualTo(TransactionMethod.PIX);
        assertThat(account.getBalance()).isEqualByComparingTo("125.50");
        verify(repository).save(result);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void createsExpenseWithCurrentDateAndDecreasesAccountBalance() {
        // Arrange
        category.setType(TransactionType.SAIDA);
        when(repository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Transaction result = service.createTransaction(company, account, category, TransactionType.SAIDA,
                "Compra", new BigDecimal("30.00"), TransactionMethod.DINHEIRO);

        // Assert
        assertThat(result.getDate()).isEqualTo(LocalDate.now());
        assertThat(result.getType()).isEqualTo(TransactionType.SAIDA);
        assertThat(account.getBalance()).isEqualByComparingTo("70.00");
        verify(repository).save(result);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void createsFirstTransactionFromZeroWhenAccountBalanceIsNull() {
        // Arrange
        account.setBalance(null);
        when(repository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Transaction result = service.createTransaction(company, account, category, TransactionType.ENTRADA,
                "Saldo inicial", new BigDecimal("18.00"), TransactionMethod.PIX, LocalDate.now());

        // Assert
        assertThat(result.getValue()).isEqualByComparingTo("18.00");
        assertThat(account.getBalance()).isEqualByComparingTo("18.00");
        verify(repository).save(result);
        verifyNoMoreInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"company", "account", "category", "type", "description", "nullValue",
            "zeroValue", "method", "date"})
    void rejectsMissingOrInvalidCreationFieldsWithoutSaving(String invalidField) {
        // Arrange
        Company requestedCompany = company;
        BankAccount requestedAccount = account;
        TransactionCategory requestedCategory = category;
        TransactionType requestedType = TransactionType.ENTRADA;
        String description = "Venda";
        BigDecimal value = BigDecimal.TEN;
        TransactionMethod method = TransactionMethod.PIX;
        LocalDate date = LocalDate.now();
        switch (invalidField) {
            case "company" -> requestedCompany = null;
            case "account" -> requestedAccount = null;
            case "category" -> requestedCategory = null;
            case "type" -> requestedType = null;
            case "description" -> description = " ";
            case "nullValue" -> value = null;
            case "zeroValue" -> value = BigDecimal.ZERO;
            case "method" -> method = null;
            case "date" -> date = null;
            default -> throw new IllegalArgumentException(invalidField);
        }
        Company finalCompany = requestedCompany;
        BankAccount finalAccount = requestedAccount;
        TransactionCategory finalCategory = requestedCategory;
        TransactionType finalType = requestedType;
        String finalDescription = description;
        BigDecimal finalValue = value;
        TransactionMethod finalMethod = method;
        LocalDate finalDate = date;

        // Act & Assert
        assertThatThrownBy(() -> service.createTransaction(finalCompany, finalAccount, finalCategory,
                finalType, finalDescription, finalValue, finalMethod, finalDate))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsCategoryWhoseTypeDoesNotMatchTransactionWithoutChangingBalance() {
        // Arrange
        category.setType(TransactionType.SAIDA);

        // Act & Assert
        assertThatThrownBy(() -> service.createTransaction(company, account, category, TransactionType.ENTRADA,
                "Venda", BigDecimal.TEN, TransactionMethod.PIX, LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A categoria selecionada não corresponde ao tipo da transação");
        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
        verifyNoInteractions(repository);
    }

    @Test
    void findsTransactionById() {
        // Arrange
        Transaction expected = transaction(TransactionType.ENTRADA, new BigDecimal("15.00"), account);
        when(repository.findById(20L)).thenReturn(Optional.of(expected));

        // Act
        Transaction result = service.findById(20L);

        // Assert
        assertThat(result).isSameAs(expected);
        verify(repository).findById(20L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void rejectsNullOrUnknownTransactionId() {
        // Arrange
        when(repository.findById(20L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.findById(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.findById(20L)).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Transacao nao encontrada!");
        verify(repository).findById(20L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void delegatesTransactionListsToRepository() {
        // Arrange
        List<Transaction> expected = List.of(transaction(TransactionType.ENTRADA, BigDecimal.TEN, account));
        when(repository.findAllByOrderByDateDescIdDesc()).thenReturn(expected);
        when(repository.findAllByCompany_UserCompanies_User_IdOrderByDateDescIdDesc(42L)).thenReturn(expected);
        when(repository.findByBankAccountOrderByDateDescIdDesc(account)).thenReturn(expected);
        when(repository.findByCompanyOrderByDateDescIdDesc(company)).thenReturn(expected);

        // Act
        List<Transaction> all = service.findAll();
        List<Transaction> byUser = service.findAllByUser(42L);
        List<Transaction> byAccount = service.findByBankAccount(account);
        List<Transaction> byCompany = service.findByCompany(company);

        // Assert
        assertThat(all).containsExactlyElementsOf(expected);
        assertThat(byUser).containsExactlyElementsOf(expected);
        assertThat(byAccount).containsExactlyElementsOf(expected);
        assertThat(byCompany).containsExactlyElementsOf(expected);
        verify(repository).findAllByOrderByDateDescIdDesc();
        verify(repository).findAllByCompany_UserCompanies_User_IdOrderByDateDescIdDesc(42L);
        verify(repository).findByBankAccountOrderByDateDescIdDesc(account);
        verify(repository).findByCompanyOrderByDateDescIdDesc(company);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void rejectsNullAccountOrCompanyWhenListingTransactions() {
        // Act & Assert
        assertThatThrownBy(() -> service.findByBankAccount(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.findByCompany(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.findLastByCompany(null, 5)).isInstanceOf(IllegalArgumentException.class);
        company.setId(null);
        assertThatThrownBy(() -> service.findLastByCompany(company, 5)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void updatesTransactionAndReversesOldBalanceBeforeApplyingNewEffect() {
        // Arrange
        BankAccount newAccount = new BankAccount();
        newAccount.setCompany(company);
        newAccount.setBalance(new BigDecimal("50.00"));
        TransactionCategory expenseCategory = new TransactionCategory();
        expenseCategory.setCompany(company);
        expenseCategory.setActive(true);
        expenseCategory.setType(TransactionType.SAIDA);
        Transaction existing = transaction(TransactionType.ENTRADA, new BigDecimal("25.00"), account);
        existing.setDate(LocalDate.of(2026, 10, 10));
        when(repository.save(existing)).thenReturn(existing);

        // Act
        Transaction result = service.updateTransaction(existing, newAccount, expenseCategory, TransactionType.SAIDA,
                "  Compra atualizada  ", new BigDecimal("10.00"), TransactionMethod.DINHEIRO,
                LocalDate.of(2026, 10, 21));

        // Assert
        assertThat(result).isSameAs(existing);
        assertThat(result.getBankAccount()).isSameAs(newAccount);
        assertThat(result.getTransactionCategory()).isSameAs(expenseCategory);
        assertThat(result.getDescription()).isEqualTo("Compra atualizada");
        assertThat(result.getType()).isEqualTo(TransactionType.SAIDA);
        assertThat(result.getValue()).isEqualByComparingTo("10.00");
        assertThat(result.getDate()).isEqualTo(LocalDate.of(2026, 10, 21));
        assertThat(account.getBalance()).isEqualByComparingTo("75.00");
        assertThat(newAccount.getBalance()).isEqualByComparingTo("40.00");
        verify(repository).save(existing);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void keepsOriginalDateWhenUpdatingThroughOverloadWithoutDate() {
        // Arrange
        Transaction existing = transaction(TransactionType.ENTRADA, new BigDecimal("10.00"), account);
        LocalDate originalDate = LocalDate.of(2026, 9, 1);
        existing.setDate(originalDate);
        when(repository.save(existing)).thenReturn(existing);

        // Act
        Transaction result = service.updateTransaction(existing, account, category, TransactionType.ENTRADA,
                "Descricao atualizada", new BigDecimal("12.00"), TransactionMethod.PIX);

        // Assert
        assertThat(result.getDate()).isEqualTo(originalDate);
        assertThat(account.getBalance()).isEqualByComparingTo("102.00");
        verify(repository).save(existing);
    }

    @Test
    void rejectsUpdateWithMismatchedCategoryBeforeChangingBalances() {
        // Arrange
        Transaction existing = transaction(TransactionType.ENTRADA, BigDecimal.TEN, account);
        TransactionCategory mismatchedCategory = new TransactionCategory();
        mismatchedCategory.setCompany(company);
        mismatchedCategory.setActive(true);
        mismatchedCategory.setType(TransactionType.SAIDA);

        // Act & Assert
        assertThatThrownBy(() -> service.updateTransaction(existing, account, mismatchedCategory,
                TransactionType.ENTRADA, "Atualizada", BigDecimal.ONE, TransactionMethod.PIX))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsNullTransactionOnUpdateOrDeletion() {
        // Act & Assert
        assertThatThrownBy(() -> service.updateTransaction(null, account, category, TransactionType.ENTRADA,
                "Atualizada", BigDecimal.TEN, TransactionMethod.PIX))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.deleteTransaction(null)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void deletesIncomeAndReversesItsBalanceEffect() {
        // Arrange
        Transaction existing = transaction(TransactionType.ENTRADA, new BigDecimal("25.00"), account);

        // Act
        service.deleteTransaction(existing);

        // Assert
        assertThat(account.getBalance()).isEqualByComparingTo("75.00");
        verify(repository).delete(existing);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void deletesExpenseAndUsesZeroWhenAccountBalanceIsNull() {
        // Arrange
        account.setBalance(null);
        Transaction existing = transaction(TransactionType.SAIDA, new BigDecimal("25.00"), account);

        // Act
        service.deleteTransaction(existing);

        // Assert
        assertThat(account.getBalance()).isEqualByComparingTo("25.00");
        verify(repository).delete(existing);
        verifyNoMoreInteractions(repository);
    }

    private Transaction transaction(TransactionType type, BigDecimal value, BankAccount bankAccount) {
        Transaction transaction = new Transaction();
        transaction.setId(20L);
        transaction.setCompany(company);
        transaction.setBankAccount(bankAccount);
        transaction.setTransactionCategory(category);
        transaction.setType(type);
        transaction.setDescription("Movimentacao de teste");
        transaction.setValue(value);
        transaction.setDate(LocalDate.of(2026, 10, 20));
        transaction.setMethod(TransactionMethod.PIX);
        return transaction;
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
