package br.edu.uniamerica.parceiro_auto.controller.dto;

import br.edu.uniamerica.parceiro_auto.entity.enums.CompanySize;
import br.edu.uniamerica.parceiro_auto.entity.enums.LegalNature;
import br.edu.uniamerica.parceiro_auto.entity.enums.TaxRegime;
import jakarta.validation.constraints.*;

public record CompanyRequestDTO(

        // Formato e digitos verificadores sao validados no CompanyService (CnpjValidator)
        @NotBlank(message = "O CNPJ é obrigatório.")
        String cnpj,

        @NotBlank(message = "A razão social é obrigatória.")
        @Size(max = 50, message = "A razão social deve conter no máximo 50 caracteres.")
        String legalName,

        @NotBlank(message = "O nome fantasia é obrigatório.")
        @Size(max = 50, message = "O nome fantasia deve conter no máximo 50 caracteres.")
        String tradeName,

        @Size(max = 30, message = "A inscrição estadual deve conter no máximo 30 caracteres.")
        String stateRegistration,

        @NotNull(message = "A natureza jurídica é obrigatória.")
        LegalNature legalNature,

        @NotNull(message = "O regime tributário é obrigatório.")
        TaxRegime taxRegime,

        @NotNull(message = "O porte da empresa é obrigatório.")
        CompanySize size,

        // Aceita 00000-000 (formato enviado pelo front) ou 00000000
        @NotBlank(message = "O CEP é obrigatório.")
        @Pattern(regexp = "\\d{5}-?\\d{3}", message = "O CEP deve estar no formato 00000-000.")
        String postalCode,

        @NotBlank(message = "O logradouro é obrigatório.")
        @Size(max = 100, message = "O logradouro deve conter no máximo 100 caracteres.")
        String street,

        @NotBlank(message = "O número do imóvel é obrigatório.")
        @Size(max = 10, message = "O número do imóvel deve conter no máximo 10 caracteres.")
        String streetNumber,

        @Size(max = 100, message = "O complemento deve conter no máximo 100 caracteres.")
        String addressComplement,

        @NotBlank(message = "O bairro é obrigatório.")
        @Size(max = 80, message = "O bairro deve conter no máximo 80 caracteres.")
        String neighborhood,

        @NotBlank(message = "A cidade é obrigatória.")
        @Size(max = 80, message = "A cidade deve conter no máximo 80 caracteres.")
        String city,

        @NotBlank(message = "O estado é obrigatório.")
        @Pattern(regexp = "[A-Za-z]{2}", message = "O estado deve ser informado pela sigla de 2 letras.")
        String state,

        // Aceita (00) 00000-0000, (00) 0000-0000 (formatos do front) ou apenas digitos
        @NotBlank(message = "O telefone é obrigatório.")
        @Pattern(regexp = "\\(?\\d{2}\\)?\\s?\\d{4,5}-?\\d{4}", message = "O telefone deve estar no formato (00) 00000-0000.")
        String phone,

        @NotBlank(message = "O email é obrigatório.")
        @Email(message = "O email informado é inválido.")
        @Size(max = 120, message = "O email deve conter no máximo 120 caracteres.")
        String email,

        // Opcional: o service assume true quando vier nulo
        Boolean active
) {
}