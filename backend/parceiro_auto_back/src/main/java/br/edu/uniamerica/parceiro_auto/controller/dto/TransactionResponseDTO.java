package br.edu.uniamerica.parceiro_auto.controller.dto;

import br.edu.uniamerica.parceiro_auto.entity.enums.TransactionForm;
import br.edu.uniamerica.parceiro_auto.entity.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

// Classe do tipo Record, que e imutavel e serve para apresentar dados de forma simples, sem a necessidade de criar getters e setters
public record TransactionResponseDTO(
        Long id,
        String empresa,
        String conta,
        String categoria,
        TransactionType tipo,
        String descricao,
        BigDecimal valor,
        LocalDate data,
        TransactionForm forma
) {
}
