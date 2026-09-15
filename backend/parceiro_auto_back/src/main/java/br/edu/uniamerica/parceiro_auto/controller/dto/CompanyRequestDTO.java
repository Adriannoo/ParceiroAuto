package br.edu.uniamerica.parceiro_auto.controller.dto;

import br.edu.uniamerica.parceiro_auto.entity.enums.CompanySize;
import br.edu.uniamerica.parceiro_auto.entity.enums.LegalNature;
import br.edu.uniamerica.parceiro_auto.entity.enums.TaxRegime;

public record CompanyRequestDTO(
        String cnpj,
        String legalName,
        String tradeName,
        String stateRegistration,
        LegalNature legalNature,
        TaxRegime taxRegime,
        CompanySize size,
        String postalCode,
        String street,
        String streetNumber,
        String addressComplement,
        String neighborhood,
        String city,
        String state,
        String phone,
        String email,
        Boolean active
) {
}
