package br.edu.uniamerica.parceiro_auto.controller.dto;

import br.edu.uniamerica.parceiro_auto.entity.enums.RecurrenceFrequency;
import br.edu.uniamerica.parceiro_auto.entity.enums.TransactionMethod;
import br.edu.uniamerica.parceiro_auto.entity.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponseDTO(
        Long id,
        Long companyId,
        Long bankAccountId,
        Long transactionCategoryId,
        TransactionType type,
        String description,
        BigDecimal value,
        LocalDate date,
        TransactionMethod method,
        RecurrenceFrequency recurrenceFrequency,
        LocalDate recurrenceEndDate
) {}
