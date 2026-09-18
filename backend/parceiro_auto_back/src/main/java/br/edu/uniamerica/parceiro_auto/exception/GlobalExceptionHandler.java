package br.edu.uniamerica.parceiro_auto.exception;

import br.edu.uniamerica.parceiro_auto.controller.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    @ExceptionHandler({BusinessRuleException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ApiResponse<Void>> handleBusiness(RuntimeException ex) {
        log.warn("Regra de negocio violada: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse<>(ex.getMessage(), null));
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
        log.error("Erro inesperado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>("Erro interno no servidor", null));
    }
}