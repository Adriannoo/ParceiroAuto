package br.edu.uniamerica.parceiro_auto;

import br.edu.uniamerica.parceiro_auto.controller.dto.TransactionRequestDTO;
import br.edu.uniamerica.parceiro_auto.entity.enums.TransactionMethod;
import br.edu.uniamerica.parceiro_auto.entity.enums.TransactionType;
import br.edu.uniamerica.parceiro_auto.service.*;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:api-validation;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false"
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

    @ParameterizedTest
    @CsvSource({
            "POST, /api/users", "POST, /api/users/login",
            "POST, /api/companies", "PUT, /api/companies/1",
            "POST, /api/bank-accounts", "PUT, /api/bank-accounts/1",
            "POST, /api/transactions", "PUT, /api/transactions/1"
    })
    void rejectsInvalidBodiesBeforeCallingServices(String method, String path) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), path)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(userService, companyService, bankAccountService,
                transactionService, transactionCategoryService);
    }

    @Test
    void requiresCompanyOnlyOnCreationAndKeepsDateOptional() {
        var dto = new TransactionRequestDTO(null, 1L, 1L, TransactionType.ENTRADA,
                "Serviço", new BigDecimal("150.50"), TransactionMethod.PIX, null);
        assertThat(validator.validate(dto)).isEmpty();
        assertThat(validator.validate(dto, TransactionRequestDTO.Create.class))
                .extracting(v -> v.getPropertyPath().toString()).containsExactly("companyId");
    }

    @Test
    void rejectsExcessMonetaryPrecision() {
        var dto = new TransactionRequestDTO(1L, 1L, 1L, TransactionType.ENTRADA,
                "Serviço", new BigDecimal("150.501"), TransactionMethod.PIX, null);
        assertThat(validator.validate(dto)).extracting(v -> v.getPropertyPath().toString())
                .containsExactly("value");
    }

    @Test
    void servesOpenApiWithDocumentedEndpoints() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("ParceiroAuto API"))
                .andExpect(jsonPath("$.paths['/api/transactions'].post.summary").value("Criar movimentação"))
                .andExpect(jsonPath("$.paths['/api/transactions'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/transactions/{id}'].delete.responses['204']").exists())
                .andExpect(jsonPath("$.paths['/api/companies'].get").exists())
                .andExpect(jsonPath("$.paths['/api/bank-accounts'].post").exists())
                .andExpect(jsonPath("$.paths['/api/users/login'].post").exists());
    }

    @Test
    void servesSwaggerUi() throws Exception {
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }
}
