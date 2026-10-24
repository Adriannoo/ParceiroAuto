package br.edu.uniamerica.parceiro_auto;

import br.edu.uniamerica.parceiro_auto.entity.*;
import br.edu.uniamerica.parceiro_auto.entity.enums.*;
import br.edu.uniamerica.parceiro_auto.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

// Carrega somente JPA com H2. Cada teste tem sua transacao desfeita ao terminar.
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:company-repository;NON_KEYWORDS=TRANSACTION,VALUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false"
})
// Mantem o H2 acima, incluindo o ajuste das palavras reservadas.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FinancialRepositoryTest {
    @Autowired private CompanyRepository companies;
    @Autowired private BankAccountRepository accounts;
    @Autowired private TransactionCategoryRepository categories;
    @Autowired private TransactionRepository transactions;
    @Autowired private RecurrenceRuleRepository rules;
    @Autowired private UserRepository users;
    @Autowired private UserCompanyRepository memberships;

    @Test
    void findsAccountsAndDefaultOnlyWithinRequestedCompany() {
        // Arrange: duas empresas podem possuir contas com os mesmos dados.
        Company company = company("11222333000181");
        Company other = company("11444777000161");
        BankAccount expected = account(company, true);
        account(other, true);

        // Act
        var result = accounts.findByCompany(company);
        var defaultAccount = accounts.findByCompanyAndDefaultAccountTrue(company);
        var duplicate = accounts.findByCompanyAndBankNameAndBranchAndAccountNumber(company, "Banco", "1234", "12345");

        // Assert
        assertThat(result).containsExactly(expected);
        assertThat(defaultAccount).isEqualTo(expected);
        assertThat(duplicate).isEqualTo(expected);
    }

    @Test
    void ordersCategoriesAndFiltersInactiveOnesWithinCompany() {
        // Arrange
        Company company = company("11222333000181");
        Company other = company("11444777000161");
        TransactionCategory inactive = category(company, "Antiga", false);
        TransactionCategory active = category(company, "Servicos", true);
        category(other, "Servicos", true);

        // Act
        var all = categories.findByCompanyOrderByName(company);
        var available = categories.findByCompanyAndActiveTrueOrderByName(company);
        var byName = categories.findByCompanyAndNameIgnoreCase(company, "SERVICOS");

        // Assert
        assertThat(all).containsExactly(inactive, active);
        assertThat(available).containsExactly(active);
        assertThat(byName).contains(active);
    }

    @Test
    void ordersAndLimitsTransactionsAndFiltersDateAndCategory() {
        // Arrange
        Company company = company("11222333000181");
        BankAccount account = account(company, false);
        TransactionCategory category = category(company, "Servicos", true);
        LocalDate date = LocalDate.of(2026, 10, 1);
        Transaction old = transaction(company, account, category, date.minusDays(1));
        Transaction first = transaction(company, account, category, date);
        Transaction last = transaction(company, account, category, date);

        // Act
        var limited = transactions.findByCompanyOrderByDateDescIdDesc(company, PageRequest.of(0, 2));
        var byAccount = transactions.findByBankAccountOrderByDateDescIdDesc(account);
        var inDateRange = transactions.findByCompanyAndDateBetween(company, date, date);
        var byCategory = transactions.findByCompanyAndTransactionCategory(company, category);

        // Assert: em datas iguais, o maior ID aparece primeiro.
        assertThat(limited).containsExactly(last, first);
        assertThat(byAccount).containsExactly(last, first, old);
        assertThat(inDateRange).containsExactlyInAnyOrder(first, last);
        assertThat(byCategory).containsExactlyInAnyOrder(old, first, last);
    }

    @Test
    void loadsRecurrencesInBatchWithoutIncludingOtherCompanies() {
        // Arrange
        Company company = company("11222333000181");
        Company other = company("11444777000161");
        Transaction transaction = transaction(company, account(company, false), category(company, "Servicos", true), LocalDate.now());
        Transaction otherTransaction = transaction(other, account(other, false), category(other, "Servicos", true), LocalDate.now());
        RecurrenceRule expected = rule(transaction);
        rule(otherTransaction);

        // Act
        var batch = rules.findByTransactionIn(List.of(transaction));
        var byCompany = rules.findByTransactionCompany(company);
        var byTransaction = rules.findByTransaction(transaction);

        // Assert
        assertThat(batch).containsExactly(expected);
        assertThat(byCompany).containsExactly(expected);
        assertThat(byTransaction).contains(expected);
    }

    @Test
    void restrictsUserTransactionsToMembershipAndFindsLogin() {
        // Arrange
        Company company = company("11222333000181");
        Company other = company("11444777000161");
        User user = new User();
        user.setLogin("aluno");
        user.setPassword("hash-de-teste");
        users.saveAndFlush(user);
        UserCompany membership = new UserCompany();
        membership.setUser(user);
        membership.setCompany(company);
        membership.setRole(UserCompanyRole.OWNER);
        memberships.saveAndFlush(membership);
        Transaction expected = transaction(company, account(company, false), category(company, "Servicos", true), LocalDate.now());
        transaction(other, account(other, false), category(other, "Servicos", true), LocalDate.now());

        // Act
        var result = transactions.findAllByCompany_UserCompanies_User_IdOrderByDateDescIdDesc(user.getId());

        // Assert
        assertThat(result).containsExactly(expected);
        assertThat(users.findByLogin("aluno")).isEqualTo(user);
        assertThat(memberships.existsByCompany_IdAndUser_Id(other.getId(), user.getId())).isFalse();
    }

    private Company company(String cnpj) {
        Company company = new Company();
        company.setCnpj(cnpj);
        company.setLegalName("Empresa teste");
        company.setTradeName("Empresa teste");
        company.setLegalNature(LegalNature.LTDA);
        company.setTaxRegime(TaxRegime.SIMPLES_NACIONAL);
        company.setSize(CompanySize.ME);
        company.setPostalCode("85801-000");
        company.setStreet("Rua");
        company.setStreetNumber("1");
        company.setNeighborhood("Centro");
        company.setCity("Cascavel");
        company.setState("PR");
        company.setPhone("45999999999");
        company.setEmail("teste@example.com");
        return companies.saveAndFlush(company);
    }

    private BankAccount account(Company company, boolean isDefault) {
        BankAccount account = new BankAccount();
        account.setCompany(company);
        account.setBankName("Banco");
        account.setBranch("1234");
        account.setAccountNumber("12345");
        account.setAccountType("Corrente");
        account.setDefaultAccount(isDefault);
        return accounts.saveAndFlush(account);
    }

    private TransactionCategory category(Company company, String name, boolean active) {
        TransactionCategory category = new TransactionCategory();
        category.setCompany(company);
        category.setName(name);
        category.setType(TransactionType.ENTRADA);
        category.setActive(active);
        return categories.saveAndFlush(category);
    }

    private Transaction transaction(Company company, BankAccount account, TransactionCategory category, LocalDate date) {
        Transaction transaction = new Transaction();
        transaction.setCompany(company);
        transaction.setBankAccount(account);
        transaction.setTransactionCategory(category);
        transaction.setDescription("Venda");
        transaction.setType(TransactionType.ENTRADA);
        transaction.setValue(BigDecimal.TEN);
        transaction.setMethod(TransactionMethod.PIX);
        transaction.setDate(date);
        return transactions.saveAndFlush(transaction);
    }

    private RecurrenceRule rule(Transaction transaction) {
        RecurrenceRule rule = new RecurrenceRule();
        rule.setTransaction(transaction);
        rule.setMethod(TransactionMethod.PIX);
        rule.setFrequency(RecurrenceFrequency.MONTHLY);
        rule.setStartDate(transaction.getDate());
        rule.setLastExecution(transaction.getDate());
        return rules.saveAndFlush(rule);
    }
}
