package br.edu.uniamerica.parceiro_auto;

import br.edu.uniamerica.parceiro_auto.entity.enums.*;
import br.edu.uniamerica.parceiro_auto.repository.*;
import br.edu.uniamerica.parceiro_auto.service.TransactionService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

// Executa somente quando um banco separado de teste for informado pelo terminal.
@EnabledIfSystemProperty(named = "test.postgres.url", matches = ".+")
@SpringBootTest(properties = {"spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate",
        "integrations.brasil-api.url=http://127.0.0.1:1"})
class PostgresPersistenceTest {
    private static final String SCHEMA = "test_" + UUID.randomUUID().toString().replace("-", "");
    @Autowired Flyway flyway;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionService service;
    @Autowired CompanyRepository companies;
    @Autowired BankAccountRepository accounts;
    @Autowired TransactionCategoryRepository categories;
    @Autowired TransactionRepository transactions;
    @Autowired PlatformTransactionManager transactionManager;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getProperty("test.postgres.url"));
        registry.add("spring.datasource.username", () -> System.getProperty("test.postgres.user", "postgres"));
        registry.add("spring.datasource.password", () -> System.getProperty("test.postgres.password", ""));
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.datasource.hikari.schema", () -> SCHEMA);
        registry.add("spring.flyway.schemas", () -> SCHEMA);
        registry.add("spring.flyway.default-schema", () -> SCHEMA);
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> SCHEMA);
    }

    @Test
    void migratesEmptySchemaAndDoesNotRepeatMigrations() {
        // Ignora o registro interno de criacao do schema e confere as migrations versionadas.
        assertThat(flyway.info().applied())
                .filteredOn(migration -> migration.getVersion() != null)
                .extracting(migration -> migration.getVersion().toString())
                .containsExactly("1", "2", "3");
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    @Test
    void limitsAndOrdersTransactionsInPostgres() {
        long company = createCompany("11111111111111");
        long account = createAccount(company);
        long category = createCategory(company);
        LocalDate date = LocalDate.of(2026, 9, 18);
        long oldId = createTransaction(company, account, category, date.minusDays(1));
        long firstId = createTransaction(company, account, category, date);
        long lastId = createTransaction(company, account, category, date);

        var result = service.findLastByCompany(companies.findById(company).orElseThrow(), 2);
        assertThat(result).extracting(transaction -> transaction.getId()).containsExactly(lastId, firstId);
        assertThat(result).extracting(transaction -> transaction.getId()).doesNotContain(oldId);
    }

    // Confere no banco que vinculos invalidos nao deixam alteracoes de saldo ou cadastro.
    @Test
    void rejectsForeignRelationsAndPreservesPersistedBalances() {
        long company = createCompany("22222222222222");
        long otherCompany = createCompany("33333333333333");
        long account = createAccount(company);
        long otherAccount = createAccount(otherCompany);
        long category = createCategory(company);
        long otherCategory = createCategory(otherCompany);

        assertThatThrownBy(() -> createTransaction(company, otherAccount, category, LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> createTransaction(company, account, otherCategory, LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(balance(account)).isEqualByComparingTo("100.00");
        assertThat(balance(otherAccount)).isEqualByComparingTo("100.00");

        long transactionId = createTransaction(company, account, category, LocalDate.now());
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        assertThatThrownBy(() -> tx.execute(status -> service.updateTransaction(
                transactions.findById(transactionId).orElseThrow(),
                accounts.findById(otherAccount).orElseThrow(), categories.findById(category).orElseThrow(),
                TransactionType.ENTRADA, "Teste", BigDecimal.TEN, TransactionMethod.PIX)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(balance(account)).isEqualByComparingTo("110.00");
        assertThat(balance(otherAccount)).isEqualByComparingTo("100.00");
        assertThat(jdbc.queryForObject("SELECT fk_id_bank_account FROM transaction WHERE id = ?", Long.class, transactionId))
                .isEqualTo(account);

        // Uma falha depois da escrita deve desfazer o saldo e a nova movimentacao.
        assertThatThrownBy(() -> tx.execute(status -> {
            createTransaction(company, account, category, LocalDate.now());
            throw new IllegalStateException("Falha simulada para verificar rollback");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(balance(account)).isEqualByComparingTo("110.00");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM transaction WHERE fk_id_company = ?", Integer.class, company))
                .isEqualTo(1);
    }

    private long createTransaction(long company, long account, long category, LocalDate date) {
        return new TransactionTemplate(transactionManager).execute(status -> service.createTransaction(
                companies.findById(company).orElseThrow(), accounts.findById(account).orElseThrow(),
                categories.findById(category).orElseThrow(), TransactionType.ENTRADA,
                "Teste", BigDecimal.TEN, TransactionMethod.PIX, date).getId());
    }

    private long createCompany(String cnpj) {
        return jdbc.queryForObject("INSERT INTO company(cnpj, legal_name, trade_name) VALUES (?, 'Teste', 'Teste') RETURNING id", Long.class, cnpj);
    }

    private long createAccount(long company) {
        return jdbc.queryForObject("""
                INSERT INTO bank_account(bank_name, branch, account_number, account_type, balance, default_account, fk_id_company)
                VALUES ('Banco', '1234', '123456', 'CORRENTE', 100, false, ?) RETURNING id
                """, Long.class, company);
    }

    private long createCategory(long company) {
        return jdbc.queryForObject("""
                INSERT INTO transaction_category(name, fk_id_company, type, active)
                VALUES ('Teste', ?, 'ENTRADA', true) RETURNING id
                """, Long.class, company);
    }

    private BigDecimal balance(long account) {
        return jdbc.queryForObject("SELECT balance FROM bank_account WHERE id = ?", BigDecimal.class, account);
    }
}
