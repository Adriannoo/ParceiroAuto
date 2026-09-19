package br.edu.uniamerica.parceiro_auto.controller.dto.category;

import br.edu.uniamerica.parceiro_auto.entity.enums.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransactionCategoryRequestDTO(
        @NotBlank(message = "O nome da categoria e obrigatorio.")
        @Size(min = 3, max = 50, message = "O nome da categoria deve ter entre 3 e 50 caracteres.")
        String name,

        @NotNull(message = "O tipo da categoria e obrigatorio.")
        TransactionType type
) {}
