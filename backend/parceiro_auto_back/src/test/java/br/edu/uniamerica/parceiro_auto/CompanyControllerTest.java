package br.edu.uniamerica.parceiro_auto;

import br.edu.uniamerica.parceiro_auto.controller.CompanyController;
import br.edu.uniamerica.parceiro_auto.controller.dto.company.CompanyLookupResponseDTO;
import br.edu.uniamerica.parceiro_auto.controller.dto.company.CompanyRequestDTO;
import br.edu.uniamerica.parceiro_auto.entity.Company;
import br.edu.uniamerica.parceiro_auto.exception.BusinessRuleException;
import br.edu.uniamerica.parceiro_auto.exception.ResourceNotFoundException;
import br.edu.uniamerica.parceiro_auto.service.CompanyAccessService;
import br.edu.uniamerica.parceiro_auto.service.CompanyService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CompanyController.class)
class CompanyControllerTest {

    private static final Long USER_ID = 42L;
    private static final Long COMPANY_ID = 7L;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CompanyService companyService;

    @MockitoBean
    private CompanyAccessService companyAccessService;

    @Test
    void createsCompanyAndAssociatesItWithAuthenticatedUser() throws Exception {
        // Arrange
        Company company = company();
        when(companyAccessService.currentUserId(any(HttpServletRequest.class))).thenReturn(USER_ID);
        when(companyService.createCompany(any(CompanyRequestDTO.class), eq(USER_ID))).thenReturn(company);

        // Act & Assert
        mvc.perform(post("/api/companies")
                        .session(session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCompanyJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensagem").value("Empresa criada com sucesso!"))
                .andExpect(jsonPath("$.dados.id").value(COMPANY_ID))
                .andExpect(jsonPath("$.dados.cnpj").value("11222333000181"));

        verify(companyService).createCompany(argThat(dto -> "11.222.333/0001-81".equals(dto.cnpj())), eq(USER_ID));
        verify(companyAccessService).currentUserId(any(HttpServletRequest.class));
        verifyNoMoreInteractions(companyService, companyAccessService);
    }

    @Test
    void returns400ForInvalidRegistrationWithoutCallingServices() throws Exception {
        // Act & Assert
        mvc.perform(post("/api/companies")
                        .session(session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cnpj\":\"11222333000181\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Erro de validacao"))
                .andExpect(jsonPath("$.dados.legalName").exists());

        verifyNoInteractions(companyService, companyAccessService);
    }

    @Test
    void returns409WhenCompanyCnpjAlreadyExists() throws Exception {
        // Arrange
        when(companyAccessService.currentUserId(any(HttpServletRequest.class))).thenReturn(USER_ID);
        when(companyService.createCompany(any(CompanyRequestDTO.class), eq(USER_ID)))
                .thenThrow(new BusinessRuleException("Ja existe uma empresa com esse CNPJ"));

        // Act & Assert
        mvc.perform(post("/api/companies")
                        .session(session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCompanyJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Ja existe uma empresa com esse CNPJ"));

        verify(companyAccessService).currentUserId(any(HttpServletRequest.class));
        verify(companyService).createCompany(any(CompanyRequestDTO.class), eq(USER_ID));
        verifyNoMoreInteractions(companyService, companyAccessService);
    }

    @Test
    void listsOnlyCompaniesBelongingToAuthenticatedUser() throws Exception {
        // Arrange
        when(companyAccessService.currentUserId(any(HttpServletRequest.class))).thenReturn(USER_ID);
        when(companyService.findAllByUser(USER_ID)).thenReturn(List.of(company()));

        // Act & Assert
        mvc.perform(get("/api/companies").session(session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.length()").value(1))
                .andExpect(jsonPath("$.dados[0].id").value(COMPANY_ID))
                .andExpect(jsonPath("$.dados[0].legalName").value("Oficina Exemplo Ltda"));

        verify(companyAccessService).currentUserId(any(HttpServletRequest.class));
        verify(companyService).findAllByUser(USER_ID);
        verifyNoMoreInteractions(companyService, companyAccessService);
    }

    @Test
    void findsCnpjAmongAuthenticatedUserCompanies() throws Exception {
        // Arrange
        when(companyAccessService.currentUserId(any(HttpServletRequest.class))).thenReturn(USER_ID);
        when(companyService.findByCnpjAndUser("11222333000181", USER_ID)).thenReturn(company());

        // Act & Assert
        mvc.perform(get("/api/companies").param("cnpj", "11222333000181").session(session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.id").value(COMPANY_ID))
                .andExpect(jsonPath("$.dados.cnpj").value("11222333000181"));

        verify(companyAccessService).currentUserId(any(HttpServletRequest.class));
        verify(companyService).findByCnpjAndUser("11222333000181", USER_ID);
        verifyNoMoreInteractions(companyService, companyAccessService);
    }

    @Test
    void returns404WhenCnpjDoesNotBelongToAuthenticatedUser() throws Exception {
        // Arrange
        when(companyAccessService.currentUserId(any(HttpServletRequest.class))).thenReturn(USER_ID);
        when(companyService.findByCnpjAndUser("11222333000181", USER_ID)).thenReturn(null);

        // Act & Assert
        mvc.perform(get("/api/companies").param("cnpj", "11222333000181").session(session()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Empresa nao encontrada"));

        verify(companyAccessService).currentUserId(any(HttpServletRequest.class));
        verify(companyService).findByCnpjAndUser("11222333000181", USER_ID);
        verifyNoMoreInteractions(companyService, companyAccessService);
    }

    @Test
    void looksUpCnpjAndReturnsMappedExternalData() throws Exception {
        // Arrange
        CompanyLookupResponseDTO company = new CompanyLookupResponseDTO(
                "11222333000181", "Oficina Exemplo Ltda", "Oficina Exemplo", "85801-000",
                "Avenida Brasil", "123", "Centro", "Cascavel", "PR", "(45) 3030-1234",
                "oficina@example.com"
        );
        when(companyService.lookupByCnpj("11222333000181")).thenReturn(company);

        // Act & Assert
        mvc.perform(get("/api/companies/lookup/{cnpj}", "11222333000181").session(session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.cnpj").value("11222333000181"))
                .andExpect(jsonPath("$.dados.legalName").value("Oficina Exemplo Ltda"));

        verify(companyService).lookupByCnpj("11222333000181");
        verifyNoMoreInteractions(companyService);
        verifyNoInteractions(companyAccessService);
    }

    @Test
    void returnsCompanyAfterVerifyingCurrentUserAccess() throws Exception {
        // Arrange
        when(companyAccessService.requireCompany(eq(COMPANY_ID), any(HttpServletRequest.class)))
                .thenReturn(company());

        // Act & Assert
        mvc.perform(get("/api/companies/{id}", COMPANY_ID).session(session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.id").value(COMPANY_ID))
                .andExpect(jsonPath("$.dados.tradeName").value("Oficina Exemplo"));

        verify(companyAccessService).requireCompany(eq(COMPANY_ID), any(HttpServletRequest.class));
        verifyNoInteractions(companyService);
        verifyNoMoreInteractions(companyAccessService);
    }

    @Test
    void returns404WhenCurrentUserCannotAccessCompany() throws Exception {
        // Arrange
        when(companyAccessService.requireCompany(eq(COMPANY_ID), any(HttpServletRequest.class)))
                .thenThrow(new ResourceNotFoundException("Empresa nao encontrada"));

        // Act & Assert
        mvc.perform(get("/api/companies/{id}", COMPANY_ID).session(session()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Empresa nao encontrada"));

        verify(companyAccessService).requireCompany(eq(COMPANY_ID), any(HttpServletRequest.class));
        verifyNoInteractions(companyService);
        verifyNoMoreInteractions(companyAccessService);
    }

    @Test
    void updatesCompanyAfterVerifyingCurrentUserAccess() throws Exception {
        // Arrange
        Company company = company();
        when(companyAccessService.requireCompany(eq(COMPANY_ID), any(HttpServletRequest.class))).thenReturn(company);
        when(companyService.updateCompany(eq(company), any(CompanyRequestDTO.class))).thenReturn(company);

        // Act & Assert
        mvc.perform(put("/api/companies/{id}", COMPANY_ID)
                        .session(session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCompanyJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.id").value(COMPANY_ID));

        verify(companyAccessService).requireCompany(eq(COMPANY_ID), any(HttpServletRequest.class));
        verify(companyService).updateCompany(eq(company), any(CompanyRequestDTO.class));
        verifyNoMoreInteractions(companyService, companyAccessService);
    }

    @Test
    void deletesCompanyOnlyAfterVerifyingCurrentUserAccess() throws Exception {
        // Arrange
        when(companyAccessService.requireCompany(eq(COMPANY_ID), any(HttpServletRequest.class))).thenReturn(company());

        // Act & Assert
        mvc.perform(delete("/api/companies/{id}", COMPANY_ID).session(session()))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string("Empresa deletada com sucesso!"));

        verify(companyAccessService).requireCompany(eq(COMPANY_ID), any(HttpServletRequest.class));
        verify(companyService).deleteCompany(COMPANY_ID);
        verifyNoMoreInteractions(companyService, companyAccessService);
    }

    private static MockHttpSession session() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", USER_ID);
        return session;
    }

    private static Company company() {
        Company company = new Company();
        company.setId(COMPANY_ID);
        company.setCnpj("11222333000181");
        company.setLegalName("Oficina Exemplo Ltda");
        company.setTradeName("Oficina Exemplo");
        company.setStateRegistration("123456");
        company.setLegalNature(br.edu.uniamerica.parceiro_auto.entity.enums.LegalNature.LTDA);
        company.setTaxRegime(br.edu.uniamerica.parceiro_auto.entity.enums.TaxRegime.SIMPLES_NACIONAL);
        company.setSize(br.edu.uniamerica.parceiro_auto.entity.enums.CompanySize.ME);
        company.setPostalCode("85801-000");
        company.setStreet("Avenida Brasil");
        company.setStreetNumber("123");
        company.setNeighborhood("Centro");
        company.setCity("Cascavel");
        company.setState("PR");
        company.setPhone("(45) 3030-1234");
        company.setEmail("oficina@example.com");
        company.setActive(true);
        return company;
    }

    private static String validCompanyJson() {
        return """
                {
                  "cnpj": "11.222.333/0001-81",
                  "legalName": "Oficina Exemplo Ltda",
                  "tradeName": "Oficina Exemplo",
                  "stateRegistration": "123456",
                  "legalNature": "LTDA",
                  "taxRegime": "SIMPLES_NACIONAL",
                  "size": "ME",
                  "postalCode": "85801-000",
                  "street": "Avenida Brasil",
                  "streetNumber": "123",
                  "neighborhood": "Centro",
                  "city": "Cascavel",
                  "state": "PR",
                  "phone": "(45) 3030-1234",
                  "email": "oficina@example.com",
                  "active": true
                }
                """;
    }
}
