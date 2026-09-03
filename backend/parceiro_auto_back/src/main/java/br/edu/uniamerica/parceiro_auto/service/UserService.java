package br.edu.uniamerica.parceiro_auto.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.uniamerica.parceiro_auto.entity.User;
import br.edu.uniamerica.parceiro_auto.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

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

        // Verifica se já existe um usuário com o mesmo login.
        User existingUser = userRepository.findByLogin(normalizedLogin);

        if (existingUser != null) {
            throw new IllegalArgumentException(
                    "Já existe um usuário com esse login"
            );
        }

        User user = new User();

        user.setLogin(normalizedLogin);
        user.setPassword(password);

        return userRepository.save(user);
    }

    // Autentica um usuário com base no login e senha fornecidos.
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

        User user = userRepository.findByLogin(normalizedLogin);

        if (user == null) {
            return null;
        }

        if (!user.getPassword().equals(password)) {
            return null;
        }

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