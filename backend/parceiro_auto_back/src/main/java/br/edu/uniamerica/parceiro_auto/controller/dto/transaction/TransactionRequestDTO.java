package br.edu.uniamerica.parceiro_auto.controller.dto.transaction;

import br.edu.uniamerica.parceiro_auto.entity.enums.RecurrenceFrequency;
import br.edu.uniamerica.parceiro_auto.entity.enums.TransactionMethod;
import br.edu.uniamerica.parceiro_auto.entity.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequestDTO(
        @NotNull(groups = Create.class, message = "O ID da empresa e obrigatorio.")
        @Positive(message = "O ID da empresa deve ser positivo.")
        @Schema(description = "Obrigatorio na criacao. Na atualizacao, a empresa original e mantida.", example = "1")
        Long companyId,

        @NotNull(message = "O ID da conta bancaria e obrigatorio.")
        @Positive(message = "O ID da conta bancaria deve ser positivo.")
        @Schema(example = "1")
        Long bankAccountId,

        @NotNull(message = "O ID da categoria e obrigatorio.")
        @Positive(message = "O ID da categoria deve ser positivo.")
        @Schema(example = "1")
        Long transactionCategoryId,

        @NotNull(message = "O tipo da transacao e obrigatorio.")
        TransactionType type,

        @NotBlank(message = "A descricao e obrigatoria.")
        @Size(max = 255, message = "A descricao deve conter no maximo 255 caracteres.")
        @Schema(example = "Pagamento de servico")
        String description,

        @NotNull(message = "O valor e obrigatorio.")
        @Positive(message = "O valor deve ser maior que zero.")
        @Digits(integer = 17, fraction = 2, message = "O valor deve ter no maximo 17 digitos inteiros e 2 casas decimais.")
        @Schema(example = "150.50")
        BigDecimal value,

        @NotNull(message = "O metodo de pagamento e obrigatorio.")
        TransactionMethod method,

        @Schema(description = "Opcional: na criacao assume a data atual; na atualizacao mantem a data original.", example = "2026-09-16")
        LocalDate date,

        RecurrenceFrequency recurrenceFrequency,

        LocalDate recurrenceEndDate
) {
    public interface Create extends Default {}
}
