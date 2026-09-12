package br.edu.uniamerica.parceiro_auto.controller.dto;

public record BankAccountRequestDTO(
        Long companyId,
        String bankName,
        String branch,
        String accountNumber,
        String accountType,
        boolean defaultAccount
) {}
