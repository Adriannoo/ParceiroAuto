package br.edu.uniamerica.parceiro_auto.controller.dto;

public record CompanyResponseDTO(
        Long id,
        String cnpj,
        String legalName,
        String tradeName
) {
}
