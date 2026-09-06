package br.edu.uniamerica.parceiro_auto.controller.dto;

public record CompanyRequestDTO(
        String cnpj,
        String legalName,
        String tradeName
) {
}
