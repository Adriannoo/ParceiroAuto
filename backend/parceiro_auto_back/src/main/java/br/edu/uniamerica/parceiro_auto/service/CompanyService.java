package br.edu.uniamerica.parceiro_auto.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.uniamerica.parceiro_auto.entity.Company;
import br.edu.uniamerica.parceiro_auto.repository.CompanyRepository;
import br.edu.uniamerica.parceiro_auto.util.CnpjValidator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyService {

    private final CompanyRepository companyRepository;

    /*
     * Cria uma nova empresa.
     */
    public Company createCompany(
            String cnpj,
            String legalName,
            String tradeName
    ) {

        String normalizedCnpj = validateCnpj(cnpj);

        String normalizedLegalName =
                validateRequiredText(
                        legalName,
                        "A Razão Social não pode estar vazia"
                );

        String normalizedTradeName =
                validateRequiredText(
                        tradeName,
                        "O Nome Fantasia não pode estar vazio"
                );

        // Verifica se já existe uma empresa com o mesmo CNPJ.
        Company existingCompany =
                companyRepository.findByCnpj(normalizedCnpj);

        if (existingCompany != null) {
            throw new IllegalArgumentException(
                    "Já existe uma empresa com esse CNPJ"
            );
        }

        Company company = new Company();

        company.setCnpj(normalizedCnpj);
        company.setLegalName(normalizedLegalName);
        company.setTradeName(normalizedTradeName);

        return companyRepository.save(company);
    }

    /*
     * Busca uma empresa pelo ID.
     */
    @Transactional(readOnly = true)
    public Optional<Company> findById(Long id) {

        if (id == null) {
            return Optional.empty();
        }

        return companyRepository.findById(id);
    }

    /*
     * Busca uma empresa pelo CNPJ.
     */
    @Transactional(readOnly = true)
    public Company findByCnpj(String cnpj) {

        if (cnpj == null || cnpj.isBlank()) {
            return null;
        }

        String normalizedCnpj = validateCnpj(cnpj);

        return companyRepository.findByCnpj(normalizedCnpj);
    }

    /*
     * Atualiza os dados de uma empresa existente.
     */
    public Company updateCompany(
            Company company,
            String cnpj,
            String legalName,
            String tradeName
    ) {

        if (company == null) {
            throw new IllegalArgumentException(
                    "A empresa não pode ser nula"
            );
        }

        String normalizedCnpj = validateCnpj(cnpj);

        String normalizedLegalName =
                validateRequiredText(
                        legalName,
                        "A Razão Social não pode estar vazia"
                );

        String normalizedTradeName =
                validateRequiredText(
                        tradeName,
                        "O Nome Fantasia não pode estar vazio"
                );

        /*
         * Verifica se outra empresa já possui o mesmo CNPJ.
         */
        Company existingCompany =
                companyRepository.findByCnpj(normalizedCnpj);

        if (existingCompany != null
                && !existingCompany.getId().equals(company.getId())) {

            throw new IllegalArgumentException(
                    "Já existe uma empresa com esse CNPJ"
            );
        }

        company.setCnpj(normalizedCnpj);
        company.setLegalName(normalizedLegalName);
        company.setTradeName(normalizedTradeName);

        return companyRepository.save(company);
    }

    /*
     * Valida e normaliza o CNPJ.
     */
    private String validateCnpj(String cnpj) {

        if (cnpj == null || cnpj.isBlank()) {
            throw new IllegalArgumentException(
                    "O CNPJ não pode estar vazio"
            );
        }

        String normalizedCnpj =
                cnpj.replaceAll("[^0-9]", "");

        if (!CnpjValidator.isValid(normalizedCnpj)) {
            throw new IllegalArgumentException(
                    "CNPJ inválido"
            );
        }

        return normalizedCnpj;
    }

    /*
     * Valida um texto obrigatório e remove espaços
     * desnecessários no início e no final.
     */
    private String validateRequiredText(
            String value,
            String errorMessage
    ) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }

        return value.trim();
    }
}