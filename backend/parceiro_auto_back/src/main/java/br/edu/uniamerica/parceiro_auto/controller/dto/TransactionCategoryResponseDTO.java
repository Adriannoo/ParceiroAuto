package br.edu.uniamerica.parceiro_auto.controller.dto;

import br.edu.uniamerica.parceiro_auto.entity.enums.TransactionType;

public record TransactionCategoryResponseDTO(
        Long id,
        Long companyId,
        String name,
        TransactionType type,
        boolean active
) {}
