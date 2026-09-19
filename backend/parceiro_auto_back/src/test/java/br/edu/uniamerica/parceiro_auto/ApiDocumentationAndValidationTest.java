package br.edu.uniamerica.parceiro_auto;

import br.edu.uniamerica.parceiro_auto.controller.dto.transaction.TransactionRequestDTO;
import br.edu.uniamerica.parceiro_auto.entity.BankAccount;
import br.edu.uniamerica.parceiro_auto.entity.Company;
import br.edu.uniamerica.parceiro_auto.entity.enums.TransactionMethod;
import br.edu.uniamerica.parceiro_auto.entity.enums.TransactionType;
import br.edu.uniamerica.parceiro_auto.service.*;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:api-validation;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false",
        "integrations.brasil-api.url=http://127.0.0.1:1"
})
@AutoConfigureMockMvc
class ApiDocumentationAndValidationTest {
    @Autowired MockMvc mvc;
    @Autowired Validator validator;
    @MockitoBean UserService userService;
    @MockitoBean CompanyService companyService;
    @MockitoBean BankAccountService bankAccountService;
    @MockitoBean TransactionService transactionService;
    @MockitoBean TransactionCategoryService transactionCategoryService;
    @MockitoBean RecurrenceRuleService recurrenceRuleService;
    @MockitoBean TransactionApplicationService transactionApplicationService;

    // Confere erros de leitura que acontecem antes da Bean Validation.
    @ParameterizedTest
    @ValueSource(strings = {"{", "{\"type\":\"INVALIDO\"}", "{\"date\":\"data-invalida\"}"})
    void rejectsUnreadableJson(String body) throws Exception {
        mvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").exists());
        verifyNoInteractions(transactionApplicationService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/transactions/abc", "/api/transactions/company/1/last",
            "/api/transactions/company/1/last?limit=abc"})
    void rejectsInvalidParameters(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isBadRequest());
        verifyNoInteractions(transactionApplicationService, transactionService, companyService);
    }

    @Test
    void hidesDatabaseDetails() throws Exception {
        when(transactionService.findById(1L))
                .thenThrow(new DataIntegrityViolationException("SQL secreto constraint fk_empresa"));
        mvc.perform(get("/api/transactions/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(
                        "Operacao nao permitida: existem dados duplicados ou registros vinculados"));
    }

    @Test
    void preservesFrameworkHttpStatuses() throws Exception {
        mvc.perform(get("/api/rota-inexistente")).andExpect(status().isNotFound());
        mvc.perform(patch("/api/transactions/1")).andExpect(status().isMethodNotAllowed());
    }

    // A empresa da conta vem da URL, sem precisar repetir o campo no JSON.
    @Test
    void createsAccountWithoutCompanyInBody() throws Exception {
        Company company = new Company();
        company.setId(1L);
        BankAccount account = new BankAccount();
        account.setId(2L);
        account.setCompany(company);
        when(companyService.findById(1L)).thenReturn(Optional.of(company));
        when(bankAccountService.createBankAccount(company, "Banco", "1234", "123456", "CORRENTE", false))
                .thenReturn(account);

        mvc.perform(post("/api/bank-accounts/company/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bankName":"Banco","branch":"1234","accountNumber":"123456",
                                 "accountType":"CORRENTE","defaultAccount":false}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dados.companyId").value(1));
    }

    @ParameterizedTest
    @CsvSource({"12,123456", "12345,123456", "abcd,123456", "1234,123", "1234,12345678901234"})
    void rejectsAccountFormatsBeforeServices(String branch, String accountNumber) throws Exception {
        String body = """
                {"bankName":"Banco","branch":"%s","accountNumber":"%s","accountType":"CORRENTE"}
                """.formatted(branch, accountNumber);
        mvc.perform(post("/api/bank-accounts/company/1")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(companyService, bankAccountService);
    }

    @ParameterizedTest
    @CsvSource({
            "POST, /api/users", "POST, /api/users/login",
            "POST, /api/companies", "PUT, /api/companies/1",
            "POST, /api/bank-accounts/company/1", "PUT, /api/bank-accounts/company/1/accounts/1",
            "POST, /api/transactions", "PUT, /api/transactions/1"
    })
    void rejectsInvalidBodiesBeforeCallingServices(String method, String path) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), path)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(userService, companyService, bankAccountService,
                transactionService, transactionCategoryService, transactionApplicationService);
    }

    @Test
    void requiresCompanyOnlyOnCreationAndKeepsDateOptional() {
        var dto = new TransactionRequestDTO(null, 1L, 1L, TransactionType.ENTRADA,
                "Serviço", new BigDecimal("150.50"), TransactionMethod.PIX, null, null, null);
        assertThat(validator.validate(dto)).isEmpty();
        assertThat(validator.validate(dto, TransactionRequestDTO.Create.class))
                .extracting(v -> v.getPropertyPath().toString()).containsExactly("companyId");
    }

    @Test
    void rejectsExcessMonetaryPrecision() {
        var dto = new TransactionRequestDTO(1L, 1L, 1L, TransactionType.ENTRADA,
                "Serviço", new BigDecimal("150.501"), TransactionMethod.PIX, null, null, null);
        assertThat(validator.validate(dto)).extracting(v -> v.getPropertyPath().toString())
                .containsExactly("value");
    }

    @Test
    void servesOpenApiWithDocumentedEndpoints() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("ParceiroAuto API"))
                .andExpect(jsonPath("$.paths['/api/transactions'].post.summary").exists())
                .andExpect(jsonPath("$.paths['/api/transactions'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/transactions/{id}'].delete.responses['204']").exists())
                .andExpect(jsonPath("$.paths['/api/companies'].get").exists())
                .andExpect(jsonPath("$.paths['/api/bank-accounts/company/{companyId}'].post").exists())
                .andExpect(jsonPath("$.paths['/api/users/login'].post").exists());
    }

    @Test
    void documentsCategoryAndRecurrenceSuccessStatuses() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/transaction-categories/company/{companyId}'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/transaction-categories/company/{companyId}/{categoryId}'].delete.responses['204']").exists())
                .andExpect(jsonPath("$.paths['/api/recurrence-rules/{id}'].delete.responses['204']").exists());
    }

    @Test
    void servesSwaggerUi() throws Exception {
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }
}
