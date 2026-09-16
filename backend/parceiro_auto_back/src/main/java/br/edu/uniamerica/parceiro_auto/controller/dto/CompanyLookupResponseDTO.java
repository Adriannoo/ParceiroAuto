package br.edu.uniamerica.parceiro_auto.controller.dto;

public record CompanyLookupResponseDTO(
        String cnpj,
        String legalName,
        String tradeName,
        String postalCode,
        String street,
        String streetNumber,
        String neighborhood,
        String city,
        String state,
        String phone,
        String email
) {
}