package br.edu.uniamerica.parceiro_auto;

import br.edu.uniamerica.parceiro_auto.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Inicia pelo main real e usa somente o banco H2 deste teste.
@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS, properties = {
        "spring.datasource.url=jdbc:h2:mem:authentication;NON_KEYWORDS=TRANSACTION,VALUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "integrations.brasil-api.url=http://127.0.0.1:1"
})
@AutoConfigureMockMvc
class UserAuthenticationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;

    @Test
    void registersAuthenticatesRestoresAndEndsServerSession() throws Exception {
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/companies")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/users/login").header("Origin", "https://outro-site.example")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        String body = """
                {"login":"aluno@example.com","password":" senha de teste ","name":"Aluno"}
                """;
        var registration = mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dados.name").value("Aluno"))
                .andExpect(jsonPath("$.dados.password").doesNotExist()).andReturn();
        assertThat(users.findByLogin("aluno@example.com").getPassword()).isNotEqualTo(" senha de teste ");
        var session = (MockHttpSession) registration.getRequest().getSession(false);
        mvc.perform(get("/api/users/me").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dados.login").value("aluno@example.com"));
        mvc.perform(get("/api/companies").session(session)).andExpect(status().isOk());
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"aluno@example.com\",\"password\":\"errada\"}"))
                .andExpect(status().isUnauthorized());
        var login = mvc.perform(post("/api/users/login").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        var renewedSession = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session.isInvalid()).isTrue();
        assertThat(renewedSession.getId()).isNotEqualTo(session.getId());
        mvc.perform(post("/api/users/logout").session(renewedSession)).andExpect(status().isNoContent());
        assertThat(renewedSession.isInvalid()).isTrue();
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void usersOnlySeeAndAccessCompaniesTheyCreated() throws Exception {
        var ownerResponse = mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"owner@example.com","password":"senha123","name":"Dono"}
                                """))
                .andExpect(status().isCreated()).andReturn();
        var ownerSession = (MockHttpSession) ownerResponse.getRequest().getSession(false);

        String companyJson = """
                {"cnpj":"11222333000181","legalName":"Oficina Proprietaria Ltda","tradeName":"Oficina Proprietaria",
                "legalNature":"LTDA","taxRegime":"SIMPLES_NACIONAL","size":"ME","postalCode":"85801-000",
                "street":"Avenida Brasil","streetNumber":"123","neighborhood":"Centro","city":"Cascavel",
                "state":"PR","phone":"(45) 3030-1234","email":"oficina@example.com","active":true}
                """;
        var companyResponse = mvc.perform(post("/api/companies").session(ownerSession)
                        .contentType(MediaType.APPLICATION_JSON).content(companyJson))
                .andExpect(status().isCreated()).andReturn();
        Number companyIdValue = com.jayway.jsonpath.JsonPath.read(companyResponse.getResponse().getContentAsString(), "$.dados.id");
        long companyId = companyIdValue.longValue();

        mvc.perform(get("/api/companies").session(ownerSession))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dados.length()").value(1));

        var otherResponse = mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"other@example.com","password":"senha123","name":"Outra pessoa"}
                                """))
                .andExpect(status().isCreated()).andReturn();
        var otherSession = (MockHttpSession) otherResponse.getRequest().getSession(false);

        mvc.perform(get("/api/companies").session(otherSession))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dados").isEmpty());
        mvc.perform(get("/api/companies/{id}", companyId).session(otherSession))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/transaction-categories/company/{id}", companyId).session(otherSession))
                .andExpect(status().isNotFound());
    }
}
