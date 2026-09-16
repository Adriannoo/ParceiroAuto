package br.edu.uniamerica.parceiro_auto.controller.dto;

import jakarta.validation.constraints.*;

public record BankAccountRequestDTO(

        @NotNull(message = "O ID da empresa é obrigatório.")
        @Positive(message = "O ID da empresa deve ser um número positivo.")
        Long companyId,

        @NotBlank(message = "O nome do banco é obrigatório.")
        @Size(max = 50, message = "O nome do banco deve conter no máximo 50 caracteres.")
        String bankName,

        @NotBlank(message = "A agência é obrigatória.")
        @Size(max = 4, message = "A agência deve conter no máximo 4 números.")
        @Pattern(regexp = "[0-9]+", message = "A agência deve conter apenas números.")
        String branch,

        @NotBlank(message = "O número da conta é obrigatório.")
        @Size(max = 13, message = "O número da conta deve conter no máximo 13 números.")
        @Pattern(regexp = "[0-9]+", message = "O número da conta deve conter apenas números.")
        String accountNumber,

        @NotBlank(message = "O tipo de conta é obrigatório.")
        @Size(max = 20, message = "O tipo de conta deve conter no máximo 20 caracteres.")
        String accountType,

        boolean defaultAccount
) {}
