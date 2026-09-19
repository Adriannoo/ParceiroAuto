package br.edu.uniamerica.parceiro_auto.controller.dto.bankaccount;

import java.math.BigDecimal;

public record BankAccountResponseDTO(
        Long id,
        String bankName,
        String branch,
        String accountNumber,
        String accountType,
        BigDecimal balance,
        boolean defaultAccount,
        Long companyId
) {}
