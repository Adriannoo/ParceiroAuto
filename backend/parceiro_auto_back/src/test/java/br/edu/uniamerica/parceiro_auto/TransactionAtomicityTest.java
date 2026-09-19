package br.edu.uniamerica.parceiro_auto;

import br.edu.uniamerica.parceiro_auto.controller.dto.transaction.TransactionRequestDTO;
import br.edu.uniamerica.parceiro_auto.entity.*;
import br.edu.uniamerica.parceiro_auto.entity.enums.*;
import br.edu.uniamerica.parceiro_auto.repository.*;
import br.edu.uniamerica.parceiro_auto.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;

// Usa persistencia real em H2 e falhas apos a escrita, sem transacao envolvendo o teste.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:atomicity;NON_KEYWORDS=TRANSACTION,VALUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "spring.jpa.open-in-view=false", "integrations.brasil-api.url=http://127.0.0.1:1"
})
class TransactionAtomicityTest {
    @Autowired TransactionApplicationService applicationService;
    @Autowired CompanyRepository companies;
    @Autowired BankAccountRepository accounts;
    @Autowired TransactionCategoryRepository categories;
    @Autowired TransactionRepository transactions;
    @Autowired RecurrenceRuleRepository rules;
    @Autowired TransactionCategoryService categoryService;
    @Autowired CompanyService companyService;
    @MockitoSpyBean RecurrenceRuleService recurrenceService;
    @MockitoSpyBean TransactionService transactionService;
    private Long companyId;
    private Long accountId;
    private Long categoryId;

    @Test
    void readingInactiveCategoriesDoesNotRecreateThem() {
        var company = companies.findById(companyId).orElseThrow();
        categoryService.deactivateCategory(company, categoryId);
        long count = categories.count();
        assertThat(categoryService.findActiveByCompany(company)).isEmpty();
        assertThat(categoryService.findActiveByCompany(company)).isEmpty();
        assertThat(categories.count()).isEqualTo(count);
        assertThat(categories.findById(categoryId).orElseThrow().isActive()).isFalse();
    }

    @Test
    void newCompanyReceivesDefaultCategoriesAtCreation() {
        var dto = new br.edu.uniamerica.parceiro_auto.controller.dto.company.CompanyRequestDTO(
                "11222333000181", "Teste", "Teste", null, LegalNature.LTDA,
                TaxRegime.SIMPLES_NACIONAL, CompanySize.ME, "85800-000", "Rua", "1",
                null, "Centro", "Cascavel", "PR", "45999999999", "teste@example.com", true);
        var company = companyService.createCompany(dto);
        assertThat(categoryService.findActiveByCompany(company)).hasSize(10);
    }

    @BeforeEach
    void prepareData() {
        Company company = new Company();
        company.setCnpj(UUID.randomUUID().toString().replace("-", "").substring(0, 14));
        company.setLegalName("Teste");
        company.setTradeName("Teste");
        company.setTaxRegime(TaxRegime.SIMPLES_NACIONAL);
        company.setSize(CompanySize.ME);
        company.setLegalNature(LegalNature.LTDA);
        company.setPostalCode("00000-000");
        company.setStreet("Rua");
        company.setStreetNumber("1");
        company.setNeighborhood("Centro");
        company.setCity("Cidade");
        company.setState("PR");
        company.setPhone("45999999999");
        company.setEmail("teste@example.com");
        company = companies.saveAndFlush(company);
        companyId = company.getId();
        BankAccount account = new BankAccount();
        account.setCompany(company);
        account.setBankName("Banco");
        account.setBranch("1234");
        account.setAccountNumber("123456");
        account.setAccountType("CORRENTE");
        account.setBalance(new BigDecimal("100.00"));
        accountId = accounts.saveAndFlush(account).getId();
        TransactionCategory category = new TransactionCategory();
        category.setCompany(company);
        category.setName("Teste");
        category.setType(TransactionType.ENTRADA);
        category.setActive(true);
        categoryId = categories.saveAndFlush(category).getId();
    }

    @Test
    void rollsBackCreationWhenRecurrenceFails() {
        long previousTransactions = transactions.count();
        long previousRules = rules.count();
        failAfterSavingRecurrence();
        assertThatThrownBy(() -> applicationService.create(request("10.00")))
                .isInstanceOf(IllegalStateException.class);
        assertBalance("100.00");
        assertThat(transactions.count()).isEqualTo(previousTransactions);
        assertThat(rules.count()).isEqualTo(previousRules);
    }

    @Test
    void rollsBackUpdateWhenRecurrenceFails() {
        Long id = applicationService.create(request("10.00")).id();
        failAfterSavingRecurrence();
        assertThatThrownBy(() -> applicationService.update(id, request("20.00")))
                .isInstanceOf(IllegalStateException.class);
        assertBalance("110.00");
        assertThat(transactions.findById(id).orElseThrow().getValue()).isEqualByComparingTo("10.00");
        assertThat(rules.findByTransaction(transactions.findById(id).orElseThrow())).isPresent();
    }

    @Test
    void rollsBackDeletionIncludingRecurrenceAndBalance() {
        Long id = applicationService.create(request("10.00")).id();
        doAnswer(invocation -> {
            invocation.callRealMethod();
            transactions.flush();
            throw new IllegalStateException("Falha simulada apos excluir");
        }).when(transactionService).deleteTransaction(any(Transaction.class));
        assertThatThrownBy(() -> applicationService.delete(id)).isInstanceOf(IllegalStateException.class);
        assertBalance("110.00");
        assertThat(transactions.findById(id)).isPresent();
        assertThat(rules.findByTransaction(transactions.findById(id).orElseThrow())).isPresent();
    }

    @Test
    void commitsCompleteLifecycle() {
        Long id = applicationService.create(request("10.00")).id();
        assertBalance("110.00");
        applicationService.update(id, request("20.00"));
        assertBalance("120.00");
        var transaction = transactions.findById(id).orElseThrow();
        Long ruleId = rules.findByTransaction(transaction).orElseThrow().getId();
        applicationService.delete(id);
        assertBalance("100.00");
        assertThat(transactions.findById(id)).isEmpty();
        assertThat(rules.findById(ruleId)).isEmpty();
    }

    private void failAfterSavingRecurrence() {
        doAnswer(invocation -> {
            invocation.callRealMethod();
            rules.flush();
            throw new IllegalStateException("Falha simulada apos salvar recorrencia");
        }).when(recurrenceService).saveForTransaction(any(Transaction.class), any(), any());
    }

    private TransactionRequestDTO request(String value) {
        return new TransactionRequestDTO(companyId, accountId, categoryId, TransactionType.ENTRADA,
                "Teste", new BigDecimal(value), TransactionMethod.PIX, LocalDate.now(),
                RecurrenceFrequency.MONTHLY, null);
    }

    private void assertBalance(String expected) {
        assertThat(accounts.findById(accountId).orElseThrow().getBalance()).isEqualByComparingTo(expected);
    }
}
