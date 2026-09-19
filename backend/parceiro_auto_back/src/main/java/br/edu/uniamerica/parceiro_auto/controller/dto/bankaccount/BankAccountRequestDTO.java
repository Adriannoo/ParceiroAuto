package br.edu.uniamerica.parceiro_auto.controller.dto.bankaccount;

import jakarta.validation.constraints.*;

public record BankAccountRequestDTO(

        // A empresa vem da URL, evitando dois IDs diferentes na mesma requisicao.

        @NotBlank(message = "O nome do banco é obrigatório.")
        @Size(max = 50, message = "O nome do banco deve conter no máximo 50 caracteres.")
        String bankName,

        @NotBlank(message = "A agência é obrigatória.")
        // Mantem o mesmo formato exigido pelo service.
        @Pattern(regexp = "\\d{4}", message = "A agencia deve ter exatamente 4 digitos.")
        String branch,

        @NotBlank(message = "O número da conta é obrigatório.")
        @Pattern(regexp = "\\d{4,13}", message = "O numero da conta deve ter entre 4 e 13 digitos.")
        String accountNumber,

        @NotBlank(message = "O tipo de conta é obrigatório.")
        @Size(max = 20, message = "O tipo de conta deve conter no máximo 20 caracteres.")
        String accountType,

        boolean defaultAccount
) {}
