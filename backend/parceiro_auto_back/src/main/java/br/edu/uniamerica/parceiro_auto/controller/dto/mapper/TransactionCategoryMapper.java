package br.edu.uniamerica.parceiro_auto.controller.dto.mapper;

import br.edu.uniamerica.parceiro_auto.controller.dto.category.TransactionCategoryResponseDTO;
import br.edu.uniamerica.parceiro_auto.entity.TransactionCategory;

public final class TransactionCategoryMapper {
    // Classe utilitaria: a conversao e feita pelos metodos estaticos.
    private TransactionCategoryMapper() {}

    public static TransactionCategoryResponseDTO toResponseDTO(TransactionCategory category) {
        return new TransactionCategoryResponseDTO(
                category.getId(),
                category.getCompany().getId(),
                category.getName(),
                category.getType(),
                category.isActive()
        );
    }
}
