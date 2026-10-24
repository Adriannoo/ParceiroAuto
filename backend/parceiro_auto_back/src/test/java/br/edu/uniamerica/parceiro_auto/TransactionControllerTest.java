package br.edu.uniamerica.parceiro_auto;

import br.edu.uniamerica.parceiro_auto.controller.TransactionController;
import br.edu.uniamerica.parceiro_auto.controller.dto.transaction.TransactionRequestDTO;
import br.edu.uniamerica.parceiro_auto.controller.dto.transaction.TransactionResponseDTO;
import br.edu.uniamerica.parceiro_auto.entity.BankAccount;
import br.edu.uniamerica.parceiro_auto.entity.Company;
import br.edu.uniamerica.parceiro_auto.entity.RecurrenceRule;
import br.edu.uniamerica.parceiro_auto.entity.Transaction;
import br.edu.uniamerica.parceiro_auto.entity.TransactionCategory;
import br.edu.uniamerica.parceiro_auto.entity.enums.RecurrenceFrequency;
import br.edu.uniamerica.parceiro_auto.entity.enums.TransactionMethod;
import br.edu.uniamerica.parceiro_auto.entity.enums.TransactionType;
import br.edu.uniamerica.parceiro_auto.exception.ResourceNotFoundException;
import br.edu.uniamerica.parceiro_auto.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Testa rotas, validacao e acesso sem iniciar banco de dados.
@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    private static final Long USER_ID = 42L;
    private static final Long COMPANY_ID = 7L;
    private static final Long TRANSACTION_ID = 20L;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TransactionService service;

    @MockitoBean
    private BankAccountService bankAccountService;

    @MockitoBean
    private TransactionApplicationService applicationService;

    @MockitoBean
    private RecurrenceRuleService recurrenceService;

    @MockitoBean
    private CompanyAccessService accessService;

    @Test
    void listsOnlyAuthenticatedUserTransactions() throws Exception {
        // Arrange
        List<Transaction> transactions = List.of(transaction());
        when(accessService.currentUserId(any(HttpServletRequest.class))).thenReturn(USER_ID);
        when(service.findAllByUser(USER_ID)).thenReturn(transactions);
        when(applicationService.toResponseDTOs(transactions)).thenReturn(List.of(response()));

        // Act
        var result = mvc.perform(get("/api/transactions").session(session()));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.length()").value(1))
                .andExpect(jsonPath("$.dados[0].id").value(TRANSACTION_ID))
                .andExpect(jsonPath("$.dados[0].value").value(150.50));
        verify(service).findAllByUser(USER_ID);
        verify(applicationService).toResponseDTOs(transactions);
    }

    @Test
    void findsTransactionAndIncludesItsRecurrence() throws Exception {
        // Arrange
        Transaction transaction = transaction();
        RecurrenceRule rule = new RecurrenceRule();
        rule.setFrequency(RecurrenceFrequency.MONTHLY);
        rule.setEndDate(LocalDate.of(2027, 12, 31));
        when(service.findById(TRANSACTION_ID)).thenReturn(transaction);
        when(recurrenceService.findByTransaction(transaction)).thenReturn(rule);

        // Act
        var result = mvc.perform(get("/api/transactions/{id}", TRANSACTION_ID).session(session()));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.id").value(TRANSACTION_ID))
                .andExpect(jsonPath("$.dados.companyId").value(COMPANY_ID))
                .andExpect(jsonPath("$.dados.bankAccountId").value(3))
                .andExpect(jsonPath("$.dados.transactionCategoryId").value(4))
                .andExpect(jsonPath("$.dados.recurrenceFrequency").value("MONTHLY"))
                .andExpect(jsonPath("$.dados.recurrenceEndDate").value("2027-12-31"));
        verify(accessService).requireAccess(eq(transaction.getCompany()), any(HttpServletRequest.class));
        verify(recurrenceService).findByTransaction(transaction);
    }

    @Test
    void createsTransactionAfterCheckingCompanyAccess() throws Exception {
        // Arrange
        when(applicationService.create(any(TransactionRequestDTO.class))).thenReturn(response());

        // Act
        var result = mvc.perform(post("/api/transactions").session(session())
                .contentType(MediaType.APPLICATION_JSON).content(validJson()));

        // Assert
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.dados.id").value(TRANSACTION_ID))
                .andExpect(jsonPath("$.dados.description").value("Pagamento de servico"))
                .andExpect(jsonPath("$.dados.method").value("PIX"));
        var order = inOrder(accessService, applicationService);
        order.verify(accessService).requireAccess(eq(COMPANY_ID), any(HttpServletRequest.class));
        order.verify(applicationService).create(argThat(dto ->
                COMPANY_ID.equals(dto.companyId()) && Long.valueOf(3).equals(dto.bankAccountId())
                        && Long.valueOf(4).equals(dto.transactionCategoryId())
                        && new BigDecimal("150.50").compareTo(dto.value()) == 0));
    }

    @Test
    void listsBankAccountTransactionsAfterCheckingAccess() throws Exception {
        // Arrange
        BankAccount bankAccount = transaction().getBankAccount();
        List<Transaction> transactions = List.of(transaction());
        when(bankAccountService.findById(3L)).thenReturn(bankAccount);
        when(service.findByBankAccount(bankAccount)).thenReturn(transactions);
        when(applicationService.toResponseDTOs(transactions)).thenReturn(List.of(response()));

        // Act
        var result = mvc.perform(get("/api/transactions/bank-account/{id}", 3L).session(session()));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.dados[0].bankAccountId").value(3));
        var order = inOrder(accessService, service);
        order.verify(accessService).requireAccess(eq(bankAccount.getCompany()), any(HttpServletRequest.class));
        order.verify(service).findByBankAccount(bankAccount);
        verify(applicationService).toResponseDTOs(transactions);
    }

    @Test
    void listsCompanyTransactionsAfterCheckingAccess() throws Exception {
        // Arrange
        Company company = company();
        List<Transaction> transactions = List.of(transaction());
        when(accessService.requireCompany(eq(COMPANY_ID), any(HttpServletRequest.class))).thenReturn(company);
        when(service.findByCompany(company)).thenReturn(transactions);
        when(applicationService.toResponseDTOs(transactions)).thenReturn(List.of(response()));

        // Act
        var result = mvc.perform(get("/api/transactions/company/{id}", COMPANY_ID).session(session()));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.dados[0].companyId").value(COMPANY_ID));
        verify(service).findByCompany(company);
        verify(applicationService).toResponseDTOs(transactions);
    }

    @Test
    void forwardsLimitWhenListingLastCompanyTransactions() throws Exception {
        // Arrange
        Company company = company();
        List<Transaction> transactions = List.of(transaction());
        when(accessService.requireCompany(eq(COMPANY_ID), any(HttpServletRequest.class))).thenReturn(company);
        when(service.findLastByCompany(company, 5)).thenReturn(transactions);
        when(applicationService.toResponseDTOs(transactions)).thenReturn(List.of(response()));

        // Act
        var result = mvc.perform(get("/api/transactions/company/{id}/last", COMPANY_ID)
                .param("limit", "5").session(session()));

        // Assert
        result.andExpect(status().isOk()).andExpect(jsonPath("$.dados[0].id").value(TRANSACTION_ID));
        verify(service).findLastByCompany(company, 5);
        verify(applicationService).toResponseDTOs(transactions);
    }

    @Test
    void updatesTransactionWithoutRequiringCompanyIdInBody() throws Exception {
        // Arrange
        Transaction transaction = transaction();
        when(service.findById(TRANSACTION_ID)).thenReturn(transaction);
        when(applicationService.update(eq(TRANSACTION_ID), any(TransactionRequestDTO.class))).thenReturn(response());
        String jsonWithoutCompany = validJson().replace("\"companyId\":7,", "");

        // Act
        var result = mvc.perform(put("/api/transactions/{id}", TRANSACTION_ID).session(session())
                .contentType(MediaType.APPLICATION_JSON).content(jsonWithoutCompany));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.id").value(TRANSACTION_ID))
                .andExpect(jsonPath("$.dados.companyId").value(COMPANY_ID));
        var order = inOrder(accessService, applicationService);
        order.verify(accessService).requireAccess(eq(transaction.getCompany()), any(HttpServletRequest.class));
        order.verify(applicationService).update(eq(TRANSACTION_ID), argThat(dto ->
                dto.companyId() == null && "Pagamento de servico".equals(dto.description())));
    }

    @Test
    void deletesTransactionAfterCheckingAccessAndReturnsNoContent() throws Exception {
        // Arrange
        Transaction transaction = transaction();
        when(service.findById(TRANSACTION_ID)).thenReturn(transaction);

        // Act
        var result = mvc.perform(delete("/api/transactions/{id}", TRANSACTION_ID).session(session()));

        // Assert
        result.andExpect(status().isNoContent()).andExpect(content().string(""));
        var order = inOrder(accessService, applicationService);
        order.verify(accessService).requireAccess(eq(transaction.getCompany()), any(HttpServletRequest.class));
        order.verify(applicationService).delete(TRANSACTION_ID);
    }

    @Test
    void rejectsMissingCompanyIdOnCreationBeforeCallingServices() throws Exception {
        // Arrange
        String jsonWithoutCompany = validJson().replace("\"companyId\":7,", "");

        // Act
        var result = mvc.perform(post("/api/transactions").session(session())
                .contentType(MediaType.APPLICATION_JSON).content(jsonWithoutCompany));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.dados.companyId").exists());
        verifyNoInteractions(accessService, applicationService, service);
    }

    @Test
    void rejectsInvalidUpdateFieldsBeforeCallingServices() throws Exception {
        // Arrange
        String invalidJson = """
                {"bankAccountId":0,"transactionCategoryId":-1,"type":"ENTRADA",
                 "description":" ","value":0,"method":"PIX"}
                """;

        // Act
        var result = mvc.perform(put("/api/transactions/{id}", TRANSACTION_ID).session(session())
                .contentType(MediaType.APPLICATION_JSON).content(invalidJson));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.dados.bankAccountId").exists())
                .andExpect(jsonPath("$.dados.transactionCategoryId").exists())
                .andExpect(jsonPath("$.dados.description").exists())
                .andExpect(jsonPath("$.dados.value").exists());
        verifyNoInteractions(accessService, applicationService, service);
    }

    @Test
    void rejectsInvalidPaymentMethodBeforeCallingServices() throws Exception {
        // Arrange
        String invalidJson = validJson().replace("\"PIX\"", "\"INVALIDO\"");

        // Act
        var result = mvc.perform(post("/api/transactions").session(session())
                .contentType(MediaType.APPLICATION_JSON).content(invalidJson));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Requisicao invalida: confira o JSON e os parametros informados"));
        verifyNoInteractions(accessService, applicationService);
    }

    @Test
    void returns404WhenTransactionDoesNotExist() throws Exception {
        // Arrange
        when(service.findById(TRANSACTION_ID))
                .thenThrow(new ResourceNotFoundException("Transacao nao encontrada"));

        // Act
        var result = mvc.perform(get("/api/transactions/{id}", TRANSACTION_ID).session(session()));

        // Assert
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Transacao nao encontrada"));
        verifyNoInteractions(accessService, recurrenceService, applicationService);
    }

    @Test
    void rejectsCreationWhenUserCannotAccessCompany() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("Empresa nao encontrada"))
                .when(accessService).requireAccess(eq(COMPANY_ID), any(HttpServletRequest.class));

        // Act
        var result = mvc.perform(post("/api/transactions").session(session())
                .contentType(MediaType.APPLICATION_JSON).content(validJson()));

        // Assert
        result.andExpect(status().isNotFound()).andExpect(jsonPath("$.mensagem").value("Empresa nao encontrada"));
        verifyNoInteractions(applicationService);
    }

    @Test
    void rejectsDeletionWhenUserCannotAccessTransactionCompany() throws Exception {
        // Arrange
        Transaction transaction = transaction();
        when(service.findById(TRANSACTION_ID)).thenReturn(transaction);
        doThrow(new ResourceNotFoundException("Empresa nao encontrada"))
                .when(accessService).requireAccess(eq(transaction.getCompany()), any(HttpServletRequest.class));

        // Act
        var result = mvc.perform(delete("/api/transactions/{id}", TRANSACTION_ID).session(session()));

        // Assert
        result.andExpect(status().isNotFound()).andExpect(jsonPath("$.mensagem").value("Empresa nao encontrada"));
        verifyNoInteractions(applicationService);
    }

    @Test
    void rejectsRequestsWithoutSessionBeforeCallingServices() throws Exception {
        // Act
        var result = mvc.perform(get("/api/transactions"));

        // Assert
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Entre para acessar o sistema"));
        verifyNoInteractions(service, applicationService, bankAccountService, recurrenceService, accessService);
    }

    @Test
    void rejectsMissingLimitBeforeCallingServices() throws Exception {
        // Act
        var result = mvc.perform(get("/api/transactions/company/{id}/last", COMPANY_ID).session(session()));

        // Assert
        result.andExpect(status().isBadRequest());
        verifyNoInteractions(service, applicationService, accessService);
    }

    @Test
    void returns400WhenServiceRejectsLimitOutsideAllowedRange() throws Exception {
        // Arrange
        Company company = company();
        when(accessService.requireCompany(eq(COMPANY_ID), any(HttpServletRequest.class))).thenReturn(company);
        when(service.findLastByCompany(company, 0)).thenThrow(new IllegalArgumentException("Limite invalido"));

        // Act
        var result = mvc.perform(get("/api/transactions/company/{id}/last", COMPANY_ID)
                .session(session()).param("limit", "0"));

        // Assert
        result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensagem").value("Limite invalido"));
        verify(service).findLastByCompany(company, 0);
        verifyNoInteractions(applicationService);
    }

    private static MockHttpSession session() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", USER_ID);
        return session;
    }

    private static Company company() {
        Company company = new Company();
        company.setId(COMPANY_ID);
        return company;
    }

    private static Transaction transaction() {
        Company company = company();
        BankAccount bankAccount = new BankAccount();
        bankAccount.setId(3L);
        bankAccount.setCompany(company);
        TransactionCategory category = new TransactionCategory();
        category.setId(4L);
        Transaction transaction = new Transaction();
        transaction.setId(TRANSACTION_ID);
        transaction.setCompany(company);
        transaction.setBankAccount(bankAccount);
        transaction.setTransactionCategory(category);
        transaction.setType(TransactionType.ENTRADA);
        transaction.setDescription("Pagamento de servico");
        transaction.setValue(new BigDecimal("150.50"));
        transaction.setDate(LocalDate.of(2026, 10, 22));
        transaction.setMethod(TransactionMethod.PIX);
        return transaction;
    }

    private static TransactionResponseDTO response() {
        return new TransactionResponseDTO(TRANSACTION_ID, COMPANY_ID, 3L, 4L,
                TransactionType.ENTRADA, "Pagamento de servico", new BigDecimal("150.50"),
                LocalDate.of(2026, 10, 22), TransactionMethod.PIX, null, null);
    }

    private static String validJson() {
        return """
                {"companyId":7,"bankAccountId":3,"transactionCategoryId":4,
                 "type":"ENTRADA","description":"Pagamento de servico",
                 "value":150.50,"method":"PIX"}
                """;
    }
}
