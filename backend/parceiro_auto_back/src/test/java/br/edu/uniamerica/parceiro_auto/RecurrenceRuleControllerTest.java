package br.edu.uniamerica.parceiro_auto;

import br.edu.uniamerica.parceiro_auto.controller.RecurrenceRuleController;
import br.edu.uniamerica.parceiro_auto.controller.dto.recurrence.RecurringTransactionResponseDTO;
import br.edu.uniamerica.parceiro_auto.entity.Company;
import br.edu.uniamerica.parceiro_auto.entity.RecurrenceRule;
import br.edu.uniamerica.parceiro_auto.entity.Transaction;
import br.edu.uniamerica.parceiro_auto.entity.enums.RecurrenceFrequency;
import br.edu.uniamerica.parceiro_auto.entity.enums.TransactionType;
import br.edu.uniamerica.parceiro_auto.exception.ResourceNotFoundException;
import br.edu.uniamerica.parceiro_auto.service.CompanyAccessService;
import br.edu.uniamerica.parceiro_auto.service.RecurrenceRuleService;
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

// Carrega apenas a camada web, mantendo os services simulados.
@WebMvcTest(RecurrenceRuleController.class)
class RecurrenceRuleControllerTest {

    private static final Long COMPANY_ID = 7L;
    private static final Long RULE_ID = 10L;
    private static final LocalDate END_DATE = LocalDate.of(2027, 12, 31);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RecurrenceRuleService service;

    @MockitoBean
    private CompanyAccessService accessService;

    @Test
    void listsCompanyRecurrencesAfterCheckingAccess() throws Exception {
        // Arrange
        Company company = company();
        when(accessService.requireCompany(eq(COMPANY_ID), any(HttpServletRequest.class))).thenReturn(company);
        when(service.findByCompany(company)).thenReturn(List.of(response()));

        // Act
        var result = mvc.perform(get("/api/recurrence-rules/company/{id}", COMPANY_ID).session(session()));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.length()").value(1))
                .andExpect(jsonPath("$.dados[0].recurrenceRuleId").value(RULE_ID))
                .andExpect(jsonPath("$.dados[0].companyId").value(COMPANY_ID))
                .andExpect(jsonPath("$.dados[0].frequency").value("MONTHLY"));
        var order = inOrder(accessService, service);
        order.verify(accessService).requireCompany(eq(COMPANY_ID), any(HttpServletRequest.class));
        order.verify(service).findByCompany(company);
    }

    @Test
    void listsNextRecurrencesUsingDefaultLimitOfThree() throws Exception {
        // Arrange
        Company company = company();
        when(accessService.requireCompany(eq(COMPANY_ID), any(HttpServletRequest.class))).thenReturn(company);
        when(service.findNextByCompany(company, 3)).thenReturn(List.of(response()));

        // Act
        var result = mvc.perform(get("/api/recurrence-rules/company/{id}/next", COMPANY_ID).session(session()));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.dados[0].nextDate").value("2027-01-15"));
        verify(service).findNextByCompany(company, 3);
    }

    @Test
    void forwardsRequestedLimitAndReturnsEmptyListWhenThereAreNoRecurrences() throws Exception {
        // Arrange
        Company company = company();
        when(accessService.requireCompany(eq(COMPANY_ID), any(HttpServletRequest.class))).thenReturn(company);
        when(service.findNextByCompany(company, 5)).thenReturn(List.of());

        // Act
        var result = mvc.perform(get("/api/recurrence-rules/company/{id}/next", COMPANY_ID)
                .param("limit", "5").session(session()));

        // Assert
        result.andExpect(status().isOk()).andExpect(jsonPath("$.dados").isEmpty());
        verify(service).findNextByCompany(company, 5);
    }

    @Test
    void updatesRecurrenceAfterCheckingCompanyAccess() throws Exception {
        // Arrange
        RecurrenceRule rule = rule();
        when(service.findById(RULE_ID)).thenReturn(rule);
        when(service.update(RULE_ID, RecurrenceFrequency.MONTHLY, END_DATE)).thenReturn(response());

        // Act
        var result = mvc.perform(put("/api/recurrence-rules/{id}", RULE_ID).session(session())
                .contentType(MediaType.APPLICATION_JSON).content(validJson()));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.recurrenceRuleId").value(RULE_ID))
                .andExpect(jsonPath("$.dados.frequency").value("MONTHLY"))
                .andExpect(jsonPath("$.dados.endDate").value("2027-12-31"));
        var order = inOrder(accessService, service);
        order.verify(service).findById(RULE_ID);
        order.verify(accessService).requireAccess(eq(rule.getTransaction().getCompany()), any(HttpServletRequest.class));
        order.verify(service).update(RULE_ID, RecurrenceFrequency.MONTHLY, END_DATE);
    }

    @Test
    void rejectsMissingFrequencyBeforeCallingServices() throws Exception {
        // Arrange
        String invalidJson = "{}";

        // Act
        var result = mvc.perform(put("/api/recurrence-rules/{id}", RULE_ID).session(session())
                .contentType(MediaType.APPLICATION_JSON).content(invalidJson));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Erro de validacao"))
                .andExpect(jsonPath("$.dados.frequency").exists());
        verifyNoInteractions(service, accessService);
    }

    @Test
    void returns404WhenRecurrenceDoesNotExistBeforeUpdate() throws Exception {
        // Arrange
        when(service.findById(RULE_ID)).thenReturn(null);

        // Act
        var result = mvc.perform(put("/api/recurrence-rules/{id}", RULE_ID).session(session())
                .contentType(MediaType.APPLICATION_JSON).content(validJson()));

        // Assert
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Recorrencia nao encontrada"));
        verify(service, never()).update(anyLong(), any(), any());
        verifyNoInteractions(accessService);
    }

    @Test
    void returns404WhenRecurrenceDisappearsDuringUpdate() throws Exception {
        // Arrange
        when(service.findById(RULE_ID)).thenReturn(rule());
        when(service.update(RULE_ID, RecurrenceFrequency.MONTHLY, END_DATE)).thenReturn(null);

        // Act
        var result = mvc.perform(put("/api/recurrence-rules/{id}", RULE_ID).session(session())
                .contentType(MediaType.APPLICATION_JSON).content(validJson()));

        // Assert
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Recorrencia nao encontrada"));
        verify(service).update(RULE_ID, RecurrenceFrequency.MONTHLY, END_DATE);
    }

    @Test
    void returns400WhenServiceRejectsEndDate() throws Exception {
        // Arrange
        when(service.findById(RULE_ID)).thenReturn(rule());
        when(service.update(RULE_ID, RecurrenceFrequency.MONTHLY, END_DATE))
                .thenThrow(new IllegalArgumentException("Data final invalida"));

        // Act
        var result = mvc.perform(put("/api/recurrence-rules/{id}", RULE_ID).session(session())
                .contentType(MediaType.APPLICATION_JSON).content(validJson()));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Data final invalida"));
        verify(service).update(RULE_ID, RecurrenceFrequency.MONTHLY, END_DATE);
    }

    @Test
    void deletesRecurrenceAndReturnsNoContentAfterCheckingAccess() throws Exception {
        // Arrange
        RecurrenceRule rule = rule();
        when(service.findById(RULE_ID)).thenReturn(rule);
        when(service.delete(RULE_ID)).thenReturn(true);

        // Act
        var result = mvc.perform(delete("/api/recurrence-rules/{id}", RULE_ID).session(session()));

        // Assert
        result.andExpect(status().isNoContent()).andExpect(content().string(""));
        var order = inOrder(accessService, service);
        order.verify(service).findById(RULE_ID);
        order.verify(accessService).requireAccess(eq(rule.getTransaction().getCompany()), any(HttpServletRequest.class));
        order.verify(service).delete(RULE_ID);
    }

    @Test
    void returns404WithoutDeletingWhenRecurrenceDoesNotExist() throws Exception {
        // Arrange
        when(service.findById(RULE_ID)).thenReturn(null);

        // Act
        var result = mvc.perform(delete("/api/recurrence-rules/{id}", RULE_ID).session(session()));

        // Assert
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Recorrencia nao encontrada"));
        verify(service, never()).delete(anyLong());
        verifyNoInteractions(accessService);
    }

    @Test
    void returns404WhenRecurrenceDisappearsDuringDeletion() throws Exception {
        // Arrange
        when(service.findById(RULE_ID)).thenReturn(rule());
        when(service.delete(RULE_ID)).thenReturn(false);

        // Act
        var result = mvc.perform(delete("/api/recurrence-rules/{id}", RULE_ID).session(session()));

        // Assert
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Recorrencia nao encontrada"));
        verify(service).delete(RULE_ID);
    }

    @Test
    void rejectsDeletionWhenUserCannotAccessCompany() throws Exception {
        // Arrange
        RecurrenceRule rule = rule();
        when(service.findById(RULE_ID)).thenReturn(rule);
        doThrow(new ResourceNotFoundException("Empresa nao encontrada"))
                .when(accessService).requireAccess(eq(rule.getTransaction().getCompany()), any(HttpServletRequest.class));

        // Act
        var result = mvc.perform(delete("/api/recurrence-rules/{id}", RULE_ID).session(session()));

        // Assert
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Empresa nao encontrada"));
        verify(service, never()).delete(anyLong());
    }

    @Test
    void rejectsRequestsWithoutSessionBeforeCallingServices() throws Exception {
        // Act
        var result = mvc.perform(get("/api/recurrence-rules/company/{id}", COMPANY_ID));

        // Assert
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Entre para acessar o sistema"));
        verifyNoInteractions(service, accessService);
    }

    @Test
    void rejectsNonNumericLimitBeforeCallingServices() throws Exception {
        // Act
        var result = mvc.perform(get("/api/recurrence-rules/company/{id}/next", COMPANY_ID)
                .session(session()).param("limit", "abc"));

        // Assert
        result.andExpect(status().isBadRequest());
        verifyNoInteractions(service, accessService);
    }

    private static MockHttpSession session() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 42L);
        return session;
    }

    private static Company company() {
        Company company = new Company();
        company.setId(COMPANY_ID);
        return company;
    }

    private static RecurrenceRule rule() {
        Transaction transaction = new Transaction();
        transaction.setCompany(company());
        RecurrenceRule rule = new RecurrenceRule();
        rule.setId(RULE_ID);
        rule.setTransaction(transaction);
        return rule;
    }

    private static RecurringTransactionResponseDTO response() {
        return new RecurringTransactionResponseDTO(RULE_ID, 20L, COMPANY_ID, "Mensalidade",
                new BigDecimal("150.00"), TransactionType.SAIDA, RecurrenceFrequency.MONTHLY,
                LocalDate.of(2027, 1, 15), END_DATE);
    }

    private static String validJson() {
        return """
                {"frequency":"MONTHLY", "endDate":"2027-12-31"}
                """;
    }
}
