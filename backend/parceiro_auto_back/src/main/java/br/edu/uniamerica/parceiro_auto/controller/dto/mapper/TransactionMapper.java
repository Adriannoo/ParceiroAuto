package br.edu.uniamerica.parceiro_auto.controller.dto.mapper;

import br.edu.uniamerica.parceiro_auto.controller.dto.TransactionResponseDTO;
import br.edu.uniamerica.parceiro_auto.entity.RecurrenceRule;
import br.edu.uniamerica.parceiro_auto.entity.Transaction;

public class TransactionMapper {
    public static TransactionResponseDTO toResponseDTO(Transaction transaction) {
        return toResponseDTO(transaction, null);
    }

    public static TransactionResponseDTO toResponseDTO(Transaction transaction, RecurrenceRule recurrenceRule) {
        return new TransactionResponseDTO(
                transaction.getId(),
                transaction.getCompany().getId(),
                transaction.getBankAccount().getId(),
                transaction.getTransactionCategory().getId(),
                transaction.getType(),
                transaction.getDescription(),
                transaction.getValue(),
                transaction.getDate(),
                transaction.getMethod(),
                recurrenceRule == null ? null : recurrenceRule.getFrequency(),
                recurrenceRule == null ? null : recurrenceRule.getEndDate()
        );
    }
}
