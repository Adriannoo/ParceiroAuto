package br.edu.uniamerica.parceiro_auto;

import br.edu.uniamerica.parceiro_auto.config.PasswordConfig;
import br.edu.uniamerica.parceiro_auto.entity.User;
import br.edu.uniamerica.parceiro_auto.repository.UserRepository;
import br.edu.uniamerica.parceiro_auto.service.UserService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {
    private final UserRepository repository = mock(UserRepository.class);
    private final org.springframework.security.crypto.password.PasswordEncoder encoder = new PasswordConfig().passwordEncoder();
    private final UserService service = new UserService(repository, encoder);

    @Test
    void storesSaltedHashAndAuthenticatesWithoutPlaintextFallback() {
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        User user = service.createUser("teste", "senha de teste");
        assertThat(user.getPassword()).startsWith("{pbkdf2}").doesNotContain("senha de teste");
        assertThat(encoder.matches("senha de teste", user.getPassword())).isTrue();
        assertThat(encoder.encode("senha de teste")).isNotEqualTo(user.getPassword());
        when(repository.findByLogin("teste")).thenReturn(user);
        assertThat(service.authenticate("teste", "senha de teste")).isSameAs(user);
        assertThat(service.authenticate("teste", "errada")).isNull();
        assertThat(user.toString()).doesNotContain(user.getPassword());
    }

    @Test
    void preservesLongUnicodePasswordsWithoutTruncation() {
        String password = "\u00e1".repeat(90);
        String hash = encoder.encode(password);
        assertThat(encoder.matches(password, hash)).isTrue();
        assertThat(encoder.matches(password + "x", hash)).isFalse();
    }
}
