package br.edu.uniamerica.parceiro_auto.controller.dto;

import br.edu.uniamerica.parceiro_auto.entity.enums.TransactionMethod;
import br.edu.uniamerica.parceiro_auto.entity.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import jakarta.validation.groups.Default;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequestDTO(
        @NotNull(groups = Create.class, message = "O ID da empresa é obrigatório.") // @NotNull = Não pode ser nulo
        @Positive(message = "O ID da empresa deve ser positivo.") // @Positive = Precisa ser maior do que zero
        @Schema(description = "Obrigatório na criação. Na atualização, a empresa original é mantida.", example = "1")
        Long companyId,

        @NotNull(message = "O ID da conta bancária é obrigatório.")
        @Positive(message = "O ID da conta bancária deve ser positivo.")
        @Schema(example = "1") // @Schema = anotação para a documentação, ela acrescenta descrições, exemplos e informações sobre campos
        // Schema(exmaple = "1") ou seja, o exemplo de um Id de conta bancaria seria o numero 1! :)
        Long bankAccountId,

        @NotNull(message = "O ID da categoria é obrigatório.")
        @Positive(message = "O ID da categoria deve ser positivo.")
        @Schema(example = "1")
        Long transactionCategoryId,

        @NotNull(message = "O tipo da transação é obrigatório.")
        TransactionType type,

        @NotBlank(message = "A descrição é obrigatória.")
        @Size(max = 255, message = "A descrição deve conter no máximo 255 caracteres.")
        @Schema(example = "Pagamento de serviço")
        String description,

        @NotNull(message = "O valor é obrigatório.")
        @Positive(message = "O valor deve ser maior que zero.")
        @Digits(integer = 17, fraction = 2, message = "O valor deve ter no máximo 17 dígitos inteiros e 2 casas decimais.") // @Digits = Define os digitos e casas decimais
        @Schema(example = "150.50")
        BigDecimal value,

        @NotNull(message = "O método de pagamento é obrigatório.")
        TransactionMethod method,

        @Schema(description = "Opcional: na criação assume a data atual; na atualização mantém a data original.", example = "2026-09-16")
        LocalDate date
) {
    // Interface sem metodos, ela identifica um conjunto de regras.
    public interface Create extends Default {}
}
