package br.edu.uniamerica.parceiro_auto.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequestDTO(
    @NotBlank(message = "O login é obrigatório.")
    @Size(max = 50, message = "O login deve conter no máximo 50 caracteres.")
    @Schema(example = "usuario.exemplo")
    String login,

    @NotBlank(message = "A senha é obrigatória.")
    @Size(max = 100, message = "A senha deve conter no máximo 100 caracteres.")
    @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
    String password
) {}
