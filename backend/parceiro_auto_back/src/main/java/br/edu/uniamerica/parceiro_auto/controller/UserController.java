package br.edu.uniamerica.parceiro_auto.controller;

import br.edu.uniamerica.parceiro_auto.controller.dto.ApiResponse;
import br.edu.uniamerica.parceiro_auto.controller.dto.UserRequestDTO;
import br.edu.uniamerica.parceiro_auto.controller.dto.UserResponseDTO;
import br.edu.uniamerica.parceiro_auto.controller.dto.mapper.UserMapper;
import br.edu.uniamerica.parceiro_auto.entity.User;
import br.edu.uniamerica.parceiro_auto.exception.ResourceNotFoundException;
import br.edu.uniamerica.parceiro_auto.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Usuários")
@RestController
@RequestMapping("api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Endpoint para criar um novo usuario
    // Queremos devolver 201 CREATED, que e o status correto para criacao.
    // POST localhost:8080/api/users
    @PostMapping
    @Operation(summary = "Cadastrar usuário")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Criado com sucesso", useReturnTypeSchema = true)
    public ResponseEntity<ApiResponse<UserResponseDTO>> create(@Valid @RequestBody UserRequestDTO dto) {
        User user = userService.createUser(dto.login(), dto.password());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Usuario criado com sucesso!", UserMapper.toResponseDTO(user)));
    }

    // Endpoint para autenticar um usuario
    // Queremos devolver 200 OK, que e o status generico para sucesso.
    // POST localhost:8080/api/users/login
    @PostMapping("/login")
    @Operation(summary = "Autenticar usuário")
    public ResponseEntity<ApiResponse<UserResponseDTO>> authenticate(@Valid @RequestBody UserRequestDTO dto) {
        User user = userService.authenticate(dto.login(), dto.password());

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login ou senha invalidos!");
        }

        return ResponseEntity.ok(new ApiResponse<>("Login realizado com sucesso!", UserMapper.toResponseDTO(user)));
    }

    // Endpoint para buscar um usuario pelo id
    // Queremos devolver 200 OK, que e o status generico para sucesso.
    // GET localhost:8080/api/users/{id}
    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuário por ID")
    public ResponseEntity<ApiResponse<UserResponseDTO>> findById(@PathVariable Long id) {
        User user = userService.findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Usuario nao econtrado!")
                );

        return ResponseEntity.ok(new ApiResponse<>("Usuario encontrado com sucesso!", UserMapper.toResponseDTO(user)));
    }

    // Endpoint para buscar um usuario pelo login
    // Queremos devolver 200 OK, que e o status generico para sucesso.
    // GET localhost:8080/api/users?login={login}
    @GetMapping
    @Operation(summary = "Buscar usuário por login")
    public ResponseEntity<ApiResponse<UserResponseDTO>> findByLogin(@RequestParam String login) {
        User user = userService.findByLogin(login);

        if(user == null) {
            throw new ResourceNotFoundException("Usuario nao encontrado!");
        }

        return ResponseEntity.ok(new ApiResponse<>("Usuario encontrado com sucesso!", UserMapper.toResponseDTO(user)));
    }
}
