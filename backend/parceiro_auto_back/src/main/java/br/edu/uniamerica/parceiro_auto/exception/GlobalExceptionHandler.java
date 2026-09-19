package br.edu.uniamerica.parceiro_auto.exception;

import br.edu.uniamerica.parceiro_auto.controller.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 400 - falhou a Bean Validation dos DTOs
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> erros = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(erro -> erros.put(erro.getField(), erro.getDefaultMessage()));

        log.warn("Falha de validacao: {}", erros);
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>("Erro de validacao", erros));
    }

    // 404 - recurso inexistente
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Recurso nao encontrado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(ex.getMessage(), null));
    }

    // 409 - conflito de regra de negocio (ex: CNPJ duplicado)
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessRuleException ex) {
        log.warn("Regra de negocio violada: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse<>(ex.getMessage(), null));
    }

    // Nao devolve SQL, nomes de tabelas ou valores internos para o cliente.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Conflito de integridade no banco", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse<>("Operacao nao permitida: existem dados duplicados ou registros vinculados", null));
    }

    // JSON malformado, enum invalido ou parametro incorreto sao erros de entrada.
    @ExceptionHandler({HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> handleInvalidInput(Exception ex) {
        log.warn("Formato de requisicao invalido: {}", ex.getClass().getSimpleName());
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>("Requisicao invalida: confira o JSON e os parametros informados", null));
    }

    // 400 - dado invalido vindo da camada de service
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Requisicao invalida: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(ex.getMessage(), null));
    }

    // mantem o status que o controller ja escolheu
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(ResponseStatusException ex) {
        log.warn("Erro tratado: {}", ex.getReason());
        return ResponseEntity.status(ex.getStatusCode())
                .body(new ApiResponse<>(ex.getReason(), null));
    }

    // 500 - qualquer coisa nao prevista
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        // Preserva erros HTTP do Spring, como rota ausente (404) e metodo incorreto (405).
        if (ex instanceof ErrorResponse error) {
            return ResponseEntity.status(error.getStatusCode()).headers(error.getHeaders())
                    .body(new ApiResponse<>("Requisicao nao pode ser atendida", null));
        }
        log.error("Erro inesperado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>("Erro interno no servidor", null));
    }
}
