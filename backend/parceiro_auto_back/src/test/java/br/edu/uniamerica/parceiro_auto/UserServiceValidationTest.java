package br.edu.uniamerica.parceiro_auto;

import br.edu.uniamerica.parceiro_auto.entity.User;
import br.edu.uniamerica.parceiro_auto.exception.BusinessRuleException;
import br.edu.uniamerica.parceiro_auto.repository.UserRepository;
import br.edu.uniamerica.parceiro_auto.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

// Isola as validacoes; os testes de hash real continuam em UserServiceTest.
class UserServiceValidationTest {
    private final UserRepository repository = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final UserService service = new UserService(repository, encoder);

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void rejectsInvalidLoginBeforeCreatingOrAuthenticating(String login) {
        // Act & Assert
        assertThatThrownBy(() -> service.createUser(login, "senha"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.authenticate(login, "senha"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository, encoder);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void rejectsInvalidPasswordBeforeCreatingOrAuthenticating(String password) {
        // Act & Assert
        assertThatThrownBy(() -> service.createUser("aluno", password))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.authenticate("aluno", password))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository, encoder);
    }

    @Test
    void rejectsDuplicateLoginWithoutEncodingOrSaving() {
        // Arrange
        when(repository.findByLogin("aluno")).thenReturn(new User());

        // Act & Assert
        assertThatThrownBy(() -> service.createUser(" aluno ", "senha"))
                .isInstanceOf(BusinessRuleException.class);
        verify(repository, never()).save(any());
        verifyNoInteractions(encoder);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {" ", " Nome do aluno "})
    void savesProvidedNameOrUsesLoginAsFallback(String name) {
        // Arrange
        when(encoder.encode("senha")).thenReturn("hash");
        when(repository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        // Act
        User result = service.createUser(" aluno ", "senha", name);

        // Assert: o nome vazio recebe o login, e um nome informado perde apenas as bordas.
        String expected = name == null || name.isBlank() ? "aluno" : "Nome do aluno";
        assertThat(result.getName()).isEqualTo(expected);
        assertThat(result.getLogin()).isEqualTo("aluno");
        assertThat(result.getPassword()).isEqualTo("hash");
        verify(encoder).encode("senha");
    }

    @Test
    void returnsNullWhenLoginDoesNotExistWithoutCheckingPassword() {
        // Arrange
        when(repository.findByLogin("aluno")).thenReturn(null);

        // Act
        User result = service.authenticate(" aluno ", "senha");

        // Assert
        assertThat(result).isNull();
        verify(repository).findByLogin("aluno");
        verifyNoInteractions(encoder);
    }

    @Test
    void findsUserByIdAndDoesNotQueryForNullId() {
        // Arrange
        User expected = new User();
        when(repository.findById(1L)).thenReturn(Optional.of(expected));

        // Act
        var found = service.findById(1L);
        var missing = service.findById(null);

        // Assert
        assertThat(found).containsSame(expected);
        assertThat(missing).isEmpty();
        verify(repository, never()).findById(null);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void returnsNullForMissingLoginWithoutQuerying(String login) {
        // Act
        User result = service.findByLogin(login);

        // Assert
        assertThat(result).isNull();
        verifyNoInteractions(repository);
    }

    @Test
    void trimsLoginBeforeLookingUpUser() {
        // Arrange
        User expected = new User();
        when(repository.findByLogin("aluno")).thenReturn(expected);

        // Act
        User result = service.findByLogin(" aluno ");

        // Assert
        assertThat(result).isSameAs(expected);
        verify(repository).findByLogin("aluno");
    }
}
