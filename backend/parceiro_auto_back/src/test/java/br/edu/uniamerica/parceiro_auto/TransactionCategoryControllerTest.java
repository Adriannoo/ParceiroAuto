package br.edu.uniamerica.parceiro_auto;

import br.edu.uniamerica.parceiro_auto.controller.TransactionCategoryController;
import br.edu.uniamerica.parceiro_auto.entity.Company;
import br.edu.uniamerica.parceiro_auto.entity.TransactionCategory;
import br.edu.uniamerica.parceiro_auto.entity.enums.TransactionType;
import br.edu.uniamerica.parceiro_auto.exception.ResourceNotFoundException;
import br.edu.uniamerica.parceiro_auto.service.CompanyAccessService;
import br.edu.uniamerica.parceiro_auto.service.TransactionCategoryService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionCategoryController.class)
class TransactionCategoryControllerTest {

    private static final Long USER_ID = 42L;
    private static final Long COMPANY_ID = 7L;
    private static final Long CATEGORY_ID = 11L;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TransactionCategoryService categoryService;

    @MockitoBean
    private CompanyAccessService accessService;

    @Test
    void listsOnlyActiveCategoriesByDefaultAfterCheckingCompanyAccess() throws Exception {
        // Arrange
        Company company = company();
        when(accessService.requireCompany(eq(COMPANY_ID), any(HttpServletRequest.class))).thenReturn(company);
        when(categoryService.findActiveByCompany(company)).thenReturn(List.of(category(true)));

        // Act
        var result = mvc.perform(get("/api/transaction-categories/company/{companyId}", COMPANY_ID)
                .session(session()));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.length()").value(1))
                .andExpect(jsonPath("$.dados[0].id").value(CATEGORY_ID))
                .andExpect(jsonPath("$.dados[0].companyId").value(COMPANY_ID))
                .andExpect(jsonPath("$.dados[0].active").value(true));
        verify(accessService).requireCompany(eq(COMPANY_ID), any(HttpServletRequest.class));
        verify(categoryService).findActiveByCompany(company);
        verifyNoMoreInteractions(accessService, categoryService);
    }

    @Test
    void includesInactiveCategoriesOnlyWhenRequested() throws Exception {
        // Arrange
        Company company = company();
        when(accessService.requireCompany(eq(COMPANY_ID), any(HttpServletRequest.class))).thenReturn(company);
        when(categoryService.findAllByCompany(company)).thenReturn(List.of(category(true), inactiveCategory()));

        // Act
        var result = mvc.perform(get("/api/transaction-categories/company/{companyId}", COMPANY_ID)
                .param("includeInactive", "true").session(session()));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.length()").value(2))
                .andExpect(jsonPath("$.dados[0].active").value(true))
                .andExpect(jsonPath("$.dados[1].active").value(false));
        verify(categoryService).findAllByCompany(company);
        verify(categoryService, never()).findActiveByCompany(any());
    }

    @Test
    void createsCategoryAndReturnsCreatedResponse() throws Exception {
        // Arrange
        Company company = company();
        when(accessService.requireCompany(eq(COMPANY_ID), any(HttpServletRequest.class))).thenReturn(company);
        when(categoryService.createCategory(company, "Servicos", TransactionType.ENTRADA))
                .thenReturn(category(true));

        // Act
        var result = mvc.perform(post("/api/transaction-categories/company/{companyId}", COMPANY_ID)
                .session(session()).contentType(MediaType.APPLICATION_JSON).content(validJson()));

        // Assert
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensagem").value("Categoria criada com sucesso!"))
                .andExpect(jsonPath("$.dados.id").value(CATEGORY_ID))
                .andExpect(jsonPath("$.dados.name").value("Servicos"))
                .andExpect(jsonPath("$.dados.type").value("ENTRADA"));
        verify(accessService).requireCompany(eq(COMPANY_ID), any(HttpServletRequest.class));
        verify(categoryService).createCategory(company, "Servicos", TransactionType.ENTRADA);
        verifyNoMoreInteractions(accessService, categoryService);
    }

    @Test
    void rejectsInvalidCategoryBodyWith400BeforeCallingService() throws Exception {
        // Arrange
        String invalidJson = "{\"name\":\" \"}";

        // Act
        var result = mvc.perform(post("/api/transaction-categories/company/{companyId}", COMPANY_ID)
                .session(session()).contentType(MediaType.APPLICATION_JSON).content(invalidJson));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Erro de validacao"))
                .andExpect(jsonPath("$.dados.name").exists())
                .andExpect(jsonPath("$.dados.type").exists());
        verifyNoInteractions(accessService, categoryService);
    }

    @Test
    void returns404WhenUserCannotAccessCompanyBeforeListingCategories() throws Exception {
        // Arrange
        when(accessService.requireCompany(eq(COMPANY_ID), any(HttpServletRequest.class)))
                .thenThrow(new ResourceNotFoundException("Empresa nao encontrada"));

        // Act
        var result = mvc.perform(get("/api/transaction-categories/company/{companyId}", COMPANY_ID)
                .session(session()));

        // Assert
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Empresa nao encontrada"));
        verifyNoInteractions(categoryService);
    }

    @Test
    void deactivatesCategoryAndReturnsNoContent() throws Exception {
        // Arrange
        Company company = company();
        when(accessService.requireCompany(eq(COMPANY_ID), any(HttpServletRequest.class))).thenReturn(company);

        // Act
        var result = mvc.perform(delete(
                "/api/transaction-categories/company/{companyId}/{categoryId}", COMPANY_ID, CATEGORY_ID)
                .session(session()));

        // Assert
        result.andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(accessService).requireCompany(eq(COMPANY_ID), any(HttpServletRequest.class));
        verify(categoryService).deactivateCategory(company, CATEGORY_ID);
        verifyNoMoreInteractions(accessService, categoryService);
    }

    @Test
    void returns404AndDoesNotDeactivateWhenUserCannotAccessCompany() throws Exception {
        // Arrange
        when(accessService.requireCompany(eq(COMPANY_ID), any(HttpServletRequest.class)))
                .thenThrow(new ResourceNotFoundException("Empresa nao encontrada"));

        // Act
        var result = mvc.perform(delete(
                "/api/transaction-categories/company/{companyId}/{categoryId}", COMPANY_ID, CATEGORY_ID)
                .session(session()));

        // Assert
        result.andExpect(status().isNotFound());
        verifyNoInteractions(categoryService);
    }

    @Test
    void updatesCategoryAndReturnsChangedData() throws Exception {
        // Arrange
        Company company = company();
        TransactionCategory updated = category(true);
        updated.setName("Nova categoria");
        updated.setType(TransactionType.SAIDA);
        when(accessService.requireCompany(eq(COMPANY_ID), any(HttpServletRequest.class))).thenReturn(company);
        when(categoryService.updateCategory(company, CATEGORY_ID, "Nova categoria", TransactionType.SAIDA)
        ).thenReturn(updated);

        // Act
        var result = mvc.perform(put(
                "/api/transaction-categories/company/{companyId}/{categoryId}", COMPANY_ID, CATEGORY_ID)
                .session(session()).contentType(MediaType.APPLICATION_JSON).content(
                        "{\"name\":\"Nova categoria\",\"type\":\"SAIDA\"}"));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.id").value(CATEGORY_ID))
                .andExpect(jsonPath("$.dados.name").value("Nova categoria"))
                .andExpect(jsonPath("$.dados.type").value("SAIDA"));
        verify(categoryService).updateCategory(company, CATEGORY_ID, "Nova categoria", TransactionType.SAIDA);
    }

    @Test
    void returns400WhenCategoryUpdateViolatesBusinessRule() throws Exception {
        // Arrange
        Company company = company();
        when(accessService.requireCompany(eq(COMPANY_ID), any(HttpServletRequest.class))).thenReturn(company);
        when(categoryService.updateCategory(company, CATEGORY_ID, "Servicos", TransactionType.SAIDA))
                .thenThrow(new IllegalArgumentException("Ja existe uma categoria com esse nome."));

        // Act
        var result = mvc.perform(put(
                "/api/transaction-categories/company/{companyId}/{categoryId}", COMPANY_ID, CATEGORY_ID)
                .session(session()).contentType(MediaType.APPLICATION_JSON).content(
                        "{\"name\":\"Servicos\",\"type\":\"SAIDA\"}"));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Ja existe uma categoria com esse nome."));
        verify(categoryService).updateCategory(company, CATEGORY_ID, "Servicos", TransactionType.SAIDA);
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

    private static TransactionCategory category(boolean active) {
        TransactionCategory category = new TransactionCategory();
        category.setId(CATEGORY_ID);
        category.setCompany(company());
        category.setName("Servicos");
        category.setType(TransactionType.ENTRADA);
        category.setActive(active);
        return category;
    }

    private static TransactionCategory inactiveCategory() {
        TransactionCategory category = category(false);
        category.setId(12L);
        category.setName("Antiga");
        category.setType(TransactionType.SAIDA);
        return category;
    }

    private static String validJson() {
        return """
                {"name":"Servicos","type":"ENTRADA"}
                """;
    }
}
