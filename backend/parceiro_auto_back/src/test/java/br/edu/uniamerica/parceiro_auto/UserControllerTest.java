package br.edu.uniamerica.parceiro_auto;

import br.edu.uniamerica.parceiro_auto.controller.UserController;
import br.edu.uniamerica.parceiro_auto.entity.User;
import br.edu.uniamerica.parceiro_auto.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private UserService service;

    @Test
    void findsUserByIdWithoutExposingPassword() throws Exception {
        // Arrange
        when(service.findById(1L)).thenReturn(Optional.of(user()));

        // Act
        var result = mvc.perform(get("/api/users/1").session(session()));

        // Assert
        result.andExpect(status().isOk()).andExpect(jsonPath("$.dados.id").value(1))
                .andExpect(jsonPath("$.dados.password").doesNotExist());
    }

    @Test
    void returns404WhenUserIdDoesNotExist() throws Exception {
        // Arrange
        when(service.findById(1L)).thenReturn(Optional.empty());

        // Act
        var result = mvc.perform(get("/api/users/1").session(session()));

        // Assert
        result.andExpect(status().isNotFound());
    }

    @Test
    void findsUserByLogin() throws Exception {
        // Arrange
        when(service.findByLogin("aluno")).thenReturn(user());

        // Act
        var result = mvc.perform(get("/api/users").param("login", "aluno").session(session()));

        // Assert
        result.andExpect(status().isOk()).andExpect(jsonPath("$.dados.login").value("aluno"));
        verify(service).findByLogin("aluno");
    }

    @Test
    void returns404WhenLoginDoesNotExist() throws Exception {
        // Act
        var result = mvc.perform(get("/api/users").param("login", "ausente").session(session()));

        // Assert
        result.andExpect(status().isNotFound());
        verify(service).findByLogin("ausente");
    }

    @Test
    void returns401WhenSessionUserNoLongerExists() throws Exception {
        // Arrange
        when(service.findById(1L)).thenReturn(Optional.empty());

        // Act
        var result = mvc.perform(get("/api/users/me").session(session()));

        // Assert
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Sessao expirada. Entre novamente."));
    }

    @Test
    void authenticatesAndReplacesPreviousSession() throws Exception {
        // Arrange
        MockHttpSession previous = session();
        when(service.authenticate("aluno", "senha123")).thenReturn(user());

        // Act
        var result = mvc.perform(post("/api/users/login").session(previous)
                .contentType(MediaType.APPLICATION_JSON).content("{\"login\":\"aluno\",\"password\":\"senha123\"}"));

        // Assert
        var request = result.andExpect(status().isOk()).andExpect(jsonPath("$.dados.id").value(1))
                .andExpect(jsonPath("$.dados.password").doesNotExist()).andReturn().getRequest();
        assertThat(previous.isInvalid()).isTrue();
        assertThat(request.getSession(false).getAttribute("userId")).isEqualTo(1L);
        assertThat(request.getSession(false).getMaxInactiveInterval()).isEqualTo(1800);
    }

    @Test
    void rejectsInvalidCredentialsWithoutCreatingSession() throws Exception {
        // Act
        var result = mvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"aluno\",\"password\":\"senha123\"}"));

        // Assert
        var request = result.andExpect(status().isUnauthorized()).andReturn().getRequest();
        assertThat(request.getSession(false)).isNull();
    }

    @Test
    void createsUserAndStartsSession() throws Exception {
        // Arrange
        when(service.createUser("aluno", "senha123", "Aluno")).thenReturn(user());

        // Act
        var result = mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"aluno\",\"password\":\"senha123\",\"name\":\"Aluno\"}"));

        // Assert
        var request = result.andExpect(status().isCreated()).andReturn().getRequest();
        assertThat(request.getSession(false).getAttribute("userId")).isEqualTo(1L);
        verify(service).createUser("aluno", "senha123", "Aluno");
    }

    @Test
    void rejectsInvalidRegistrationWithoutCallingService() throws Exception {
        // Act
        var result = mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("{}"));

        // Assert
        result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.dados.login").exists());
        verifyNoInteractions(service);
    }

    @Test
    void invalidatesSessionOnLogout() throws Exception {
        // Arrange
        MockHttpSession session = session();

        // Act
        var result = mvc.perform(post("/api/users/logout").session(session));

        // Assert
        result.andExpect(status().isNoContent()).andExpect(content().string(""));
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void rejectsSessionWithoutUserId() throws Exception {
        // Act
        var result = mvc.perform(get("/api/users/me").session(new MockHttpSession()));

        // Assert
        result.andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void handlesMissingSessionDefensivelyWhenCalledDirectly() {
        // Arrange: HTTP normalmente bloqueia antes; aqui verificamos a protecao do metodo.
        UserController controller = new UserController(service);
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(service.findById(null)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> controller.currentUser(request))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode().value()).isEqualTo(401));
        assertThat(controller.logout(request).getStatusCode().value()).isEqualTo(204);
        assertThat(request.getSession(false)).isNull();
    }

    private static MockHttpSession session() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);
        return session;
    }

    private static User user() {
        User user = new User();
        user.setId(1L);
        user.setLogin("aluno");
        user.setName("Aluno");
        user.setPassword("hash-que-nao-deve-aparecer");
        return user;
    }
}
