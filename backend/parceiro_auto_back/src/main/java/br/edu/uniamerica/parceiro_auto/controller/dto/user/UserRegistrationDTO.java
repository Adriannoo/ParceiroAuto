package br.edu.uniamerica.parceiro_auto.controller.dto.user;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record UserRegistrationDTO(
    @NotBlank @Size(max = 50) String login,
    @NotBlank @Size(min = 6, max = 100) String password,
    @Size(max = 100) String name
) {}
