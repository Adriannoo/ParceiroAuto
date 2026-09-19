package br.edu.uniamerica.parceiro_auto.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

import java.util.Map;

@Configuration
public class PasswordConfig {
    // O salt aleatorio gera hashes diferentes mesmo para senhas iguais.
    // O prefixo identifica o algoritmo; a senha original nao pode ser recuperada do hash.
    @Bean
    public PasswordEncoder passwordEncoder() {
        var pbkdf2 = new Pbkdf2PasswordEncoder("", 16, 600_000,
                Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256);
        return new DelegatingPasswordEncoder("pbkdf2", Map.of("pbkdf2", pbkdf2));
    }
}
