package br.edu.uniamerica.parceiro_auto.controller.dto.mapper;

import br.edu.uniamerica.parceiro_auto.controller.dto.CompanyResponseDTO;
import br.edu.uniamerica.parceiro_auto.entity.Company;

public final class CompanyMapper {
    public static CompanyResponseDTO toResponseDTO(Company company) {
        return new CompanyResponseDTO(
                company.getId(),
                company.getCnpj(),
                company.getLegalName(),
                company.getTradeName()
        );
    }
}