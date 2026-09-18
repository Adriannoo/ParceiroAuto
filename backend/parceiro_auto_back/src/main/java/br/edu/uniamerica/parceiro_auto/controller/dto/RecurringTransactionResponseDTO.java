package br.edu.uniamerica.parceiro_auto.controller.dto;

import br.edu.uniamerica.parceiro_auto.entity.enums.RecurrenceFrequency;
import br.edu.uniamerica.parceiro_auto.entity.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringTransactionResponseDTO(
        Long recurrenceRuleId,
        Long transactionId,
        Long companyId,
        String description,
        BigDecimal value,
        TransactionType type,
        RecurrenceFrequency frequency,
        LocalDate nextDate,
        LocalDate endDate
) {}
