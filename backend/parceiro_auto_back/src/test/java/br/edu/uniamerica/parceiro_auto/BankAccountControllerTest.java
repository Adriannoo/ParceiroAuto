package br.edu.uniamerica.parceiro_auto;

import br.edu.uniamerica.parceiro_auto.controller.BankAccountController;
import br.edu.uniamerica.parceiro_auto.entity.BankAccount;
import br.edu.uniamerica.parceiro_auto.entity.Company;
import br.edu.uniamerica.parceiro_auto.exception.BusinessRuleException;
import br.edu.uniamerica.parceiro_auto.exception.ResourceNotFoundException;
import br.edu.uniamerica.parceiro_auto.service.BankAccountService;
import br.edu.uniamerica.parceiro_auto.service.CompanyAccessService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// O Spring carrega as rotas e validacoes; os services sao mocks.
@WebMvcTest(BankAccountController.class)
class BankAccountControllerTest {
    private static final String BASE = "/api/bank-accounts/company/7";
    private static final String JSON = """
            {"bankName":"Banco","branch":"1234","accountNumber":"12345",
             "accountType":"Corrente","defaultAccount":false}
            """;

    @Autowired private MockMvc mvc;
    @MockitoBean private BankAccountService service;
    @MockitoBean private CompanyAccessService access;

    @Test
    void createsAccountForAuthorizedCompany() throws Exception {
        // Arrange
        Company company = company(7L);
        when(access.requireCompany(eq(7L), any(HttpServletRequest.class))).thenReturn(company);
        when(service.createBankAccount(company, "Banco", "1234", "12345", "Corrente", false)).thenReturn(account(company));

        // Act
        var result = mvc.perform(post(BASE).session(session()).contentType(MediaType.APPLICATION_JSON).content(JSON));

        // Assert
        result.andExpect(status().isCreated()).andExpect(jsonPath("$.dados.companyId").value(7))
                .andExpect(jsonPath("$.dados.bankName").value("Banco"));
        verify(service).createBankAccount(company, "Banco", "1234", "12345", "Corrente", false);
    }

    @Test
    void listsCompanyAccounts() throws Exception {
        // Arrange
        Company company = company(7L);
        when(access.requireCompany(eq(7L), any(HttpServletRequest.class))).thenReturn(company);
        when(service.findByCompany(company)).thenReturn(List.of(account(company)));

        // Act
        var result = mvc.perform(get(BASE).session(session()));

        // Assert
        result.andExpect(status().isOk()).andExpect(jsonPath("$.dados.length()").value(1))
                .andExpect(jsonPath("$.dados[0].accountNumber").value("12345"));
    }

    @Test
    void returnsDefaultAccountWhenOneExists() throws Exception {
        // Arrange
        Company company = company(7L);
        BankAccount account = account(company);
        account.setDefaultAccount(true);
        when(access.requireCompany(eq(7L), any(HttpServletRequest.class))).thenReturn(company);
        when(service.findDefaultByCompany(company)).thenReturn(account);

        // Act
        var result = mvc.perform(get(BASE + "/default").session(session()));

        // Assert
        result.andExpect(status().isOk()).andExpect(jsonPath("$.dados.defaultAccount").value(true));
    }

    @Test
    void returns404WhenCompanyHasNoDefaultAccount() throws Exception {
        // Arrange
        when(access.requireCompany(eq(7L), any(HttpServletRequest.class))).thenReturn(company(7L));

        // Act
        var result = mvc.perform(get(BASE + "/default").session(session()));

        // Assert
        result.andExpect(status().isNotFound()).andExpect(jsonPath("$.mensagem").value("Nenhuma conta padrao encontrada!"));
    }

    @Test
    void findsAccountBelongingToCompany() throws Exception {
        // Arrange
        Company company = company(7L);
        when(access.requireCompany(eq(7L), any(HttpServletRequest.class))).thenReturn(company);
        when(service.findById(1L)).thenReturn(account(company));

        // Act
        var result = mvc.perform(get(BASE + "/accounts/1").session(session()));

        // Assert
        result.andExpect(status().isOk()).andExpect(jsonPath("$.dados.id").value(1));
    }

    @Test
    void rejectsAccountBelongingToDifferentCompany() throws Exception {
        // Arrange
        when(access.requireCompany(eq(7L), any(HttpServletRequest.class))).thenReturn(company(7L));
        when(service.findById(1L)).thenReturn(account(company(99L)));

        // Act
        var result = mvc.perform(get(BASE + "/accounts/1").session(session()));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A conta bancaria nao pertence a empresa informada!"));
    }

    @Test
    void definesDefaultAccountAfterResolvingCompanyAndAccount() throws Exception {
        // Arrange
        Company company = company(7L);
        BankAccount account = account(company);
        account.setDefaultAccount(true);
        when(access.requireCompany(eq(7L), any(HttpServletRequest.class))).thenReturn(company);
        when(service.findById(1L)).thenReturn(account);
        when(service.defineDefaultAccount(company, account)).thenReturn(account);

        // Act
        var result = mvc.perform(patch(BASE + "/accounts/1/default").session(session()));

        // Assert
        result.andExpect(status().isOk()).andExpect(jsonPath("$.dados.defaultAccount").value(true));
        verify(service).defineDefaultAccount(company, account);
    }

    @Test
    void updatesAccountWithRequestFields() throws Exception {
        // Arrange
        Company company = company(7L);
        BankAccount account = account(company);
        when(access.requireCompany(eq(7L), any(HttpServletRequest.class))).thenReturn(company);
        when(service.findById(1L)).thenReturn(account);
        when(service.updateBankAccount(company, account, "Banco", "1234", "12345", "Corrente", false)).thenReturn(account);

        // Act
        var result = mvc.perform(put(BASE + "/accounts/1").session(session()).contentType(MediaType.APPLICATION_JSON).content(JSON));

        // Assert
        result.andExpect(status().isOk()).andExpect(jsonPath("$.dados.id").value(1));
        verify(service).updateBankAccount(company, account, "Banco", "1234", "12345", "Corrente", false);
    }

    @Test
    void deletesAccountAndReturnsEmptyBody() throws Exception {
        // Arrange
        Company company = company(7L);
        BankAccount account = account(company);
        when(access.requireCompany(eq(7L), any(HttpServletRequest.class))).thenReturn(company);
        when(service.findById(1L)).thenReturn(account);

        // Act
        var result = mvc.perform(delete(BASE + "/accounts/1").session(session()));

        // Assert
        result.andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(service).deleteBankAccount(company, account);
    }

    @Test
    void rejectsInvalidDtoBeforeCallingServices() throws Exception {
        // Act
        var result = mvc.perform(post(BASE).session(session()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"defaultAccount\":false}"));

        // Assert
        result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.dados.bankName").exists())
                .andExpect(jsonPath("$.dados.branch").exists()).andExpect(jsonPath("$.dados.accountNumber").exists());
        verifyNoInteractions(service, access);
    }

    @Test
    void returns409WhenAccountAlreadyExists() throws Exception {
        // Arrange
        Company company = company(7L);
        when(access.requireCompany(eq(7L), any(HttpServletRequest.class))).thenReturn(company);
        when(service.createBankAccount(company, "Banco", "1234", "12345", "Corrente", false))
                .thenThrow(new BusinessRuleException("Conta duplicada"));

        // Act
        var result = mvc.perform(post(BASE).session(session()).contentType(MediaType.APPLICATION_JSON).content(JSON));

        // Assert
        result.andExpect(status().isConflict()).andExpect(jsonPath("$.mensagem").value("Conta duplicada"));
    }

    @Test
    void rejectsCompanyWithoutAccessBeforeConsultingAccounts() throws Exception {
        // Arrange
        when(access.requireCompany(eq(7L), any(HttpServletRequest.class)))
                .thenThrow(new ResourceNotFoundException("Empresa nao encontrada"));

        // Act
        var result = mvc.perform(get(BASE).session(session()));

        // Assert
        result.andExpect(status().isNotFound());
        verifyNoInteractions(service);
    }

    @Test
    void requiresSessionBeforeConsultingServices() throws Exception {
        // Act
        var result = mvc.perform(get(BASE));

        // Assert
        result.andExpect(status().isUnauthorized());
        verifyNoInteractions(service, access);
    }

    @Test
    void hidesInternalDetailsWhenAnUnexpectedFailureOccurs() throws Exception {
        // Arrange: uma falha interna nao deve aparecer no JSON devolvido ao cliente.
        when(access.requireCompany(eq(7L), any(HttpServletRequest.class)))
                .thenThrow(new IllegalStateException("Detalhe interno confidencial"));

        // Act
        var result = mvc.perform(get(BASE).session(session()));

        // Assert
        result.andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.mensagem").value("Erro interno no servidor"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("confidencial"))));
        verifyNoInteractions(service);
    }

    private static MockHttpSession session() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 42L);
        return session;
    }

    private static Company company(Long id) {
        Company company = new Company();
        company.setId(id);
        return company;
    }

    private static BankAccount account(Company company) {
        BankAccount account = new BankAccount();
        account.setId(1L);
        account.setCompany(company);
        account.setBankName("Banco");
        account.setBranch("1234");
        account.setAccountNumber("12345");
        account.setAccountType("Corrente");
        return account;
    }
}
