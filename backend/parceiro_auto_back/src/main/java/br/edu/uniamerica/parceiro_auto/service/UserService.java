package br.edu.uniamerica.parceiro_auto.service;

import java.util.Optional;

import br.edu.uniamerica.parceiro_auto.exception.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import br.edu.uniamerica.parceiro_auto.entity.User;
import br.edu.uniamerica.parceiro_auto.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Cria um novo usuário com login e senha.
    public User createUser(String login, String password) {

        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException(
                    "O login não pode estar vazio"
            );
        }

        String normalizedLogin = login.trim();

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException(
                    "A senha não pode estar vazia"
            );
        }

        log.info("Criando usuario com login:({})", normalizedLogin);

        // Verifica se já existe um usuário com o mesmo login.
        User existingUser = userRepository.findByLogin(normalizedLogin);

        if (existingUser != null) {
            log.warn("Login:({}) ja esta em uso", normalizedLogin);
            throw new BusinessRuleException(
                    "Já existe um usuário com esse login"
            );
        }

        User user = new User();

        user.setLogin(normalizedLogin);
        // Persiste apenas o hash; nunca a senha recebida.
        user.setPassword(passwordEncoder.encode(password));

        User saved = userRepository.save(user);
        log.info("Usuario id:({}) criado com sucesso", saved.getId());
        return saved;
    }

    // Autentica um usuário com base no login e senha fornecidos.
    @Transactional(readOnly = true)
    public User authenticate(String login, String password) {

        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException(
                    "O login não pode estar vazio"
            );
        }

        String normalizedLogin = login.trim();

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException(
                    "A senha não pode estar vazia"
            );
        }

        log.info("Tentativa de login para usuario:({})", normalizedLogin);

        User user = userRepository.findByLogin(normalizedLogin);

        if (user == null) {
            log.warn("Login falhou: usuario:({}) nao encontrado", normalizedLogin);
            return null;
        }

        // Compara usando o salt e o algoritmo presentes no hash armazenado.
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("Login falhou: senha incorreta para usuario:({})", normalizedLogin);
            return null;
        }

        log.info("Login realizado com sucesso para usuario id:({})", user.getId());
        return user;
    }

    // Busca um usuário pelo ID.
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {

        if (id == null) {
            return Optional.empty();
        }

        return userRepository.findById(id);
    }

    // Busca um usuário pelo login.
    @Transactional(readOnly = true)
    public User findByLogin(String login) {

        if (login == null || login.isBlank()) {
            return null;
        }

        return userRepository.findByLogin(login.trim());
    }
}
