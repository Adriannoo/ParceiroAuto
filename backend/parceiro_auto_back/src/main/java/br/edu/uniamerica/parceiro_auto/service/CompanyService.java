package br.edu.uniamerica.parceiro_auto.service;

import java.util.List;
import java.util.Optional;

import br.edu.uniamerica.parceiro_auto.controller.dto.CompanyLookupResponseDTO;
import br.edu.uniamerica.parceiro_auto.exception.BusinessRuleException;
import br.edu.uniamerica.parceiro_auto.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.uniamerica.parceiro_auto.controller.dto.CompanyRequestDTO;
import br.edu.uniamerica.parceiro_auto.entity.Company;
import br.edu.uniamerica.parceiro_auto.repository.CompanyRepository;
import br.edu.uniamerica.parceiro_auto.util.CnpjValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final RestClient restClient = RestClient.create();

    public Company createCompany(CompanyRequestDTO dto) {

        log.info("Criando empresa com CNPJ {}", dto.cnpj());
        String normalizedCnpj = validateCnpj(dto.cnpj());

        Company existingCompany = companyRepository.findByCnpj(normalizedCnpj);

        if (existingCompany != null) {
            log.warn("CNPJ duplicado {}", normalizedCnpj);
            throw new BusinessRuleException("Ja existe uma empresa com esse CNPJ");
        }

        Company company = new Company();
        applyCompanyData(company, dto, normalizedCnpj);

        Company saved = companyRepository.save(company);
        log.info("Empresa com CNPJ {} cadastrada (id={})", normalizedCnpj, saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<Company> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }

        return companyRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Company> findAll() {
        return companyRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Company findByCnpj(String cnpj) {
        if (cnpj == null || cnpj.isBlank()) {
            return null;
        }

        String normalizedCnpj = validateCnpj(cnpj);

        return companyRepository.findByCnpj(normalizedCnpj);
    }

    public Company updateCompany(Company company, CompanyRequestDTO dto) {
        if (company == null) {
            throw new IllegalArgumentException("A empresa nao pode ser nula");
        }
        log.info("Atualizando empresa CNPJ:{} id:{}", dto.cnpj(), company.getId());
        String normalizedCnpj = validateCnpj(dto.cnpj());

        Company existingCompany = companyRepository.findByCnpj(normalizedCnpj);

        if (existingCompany != null && !existingCompany.getId().equals(company.getId())) {
            log.warn("CNPJ {} com id: {} já pertence a outra empresa", normalizedCnpj, company.getId());
            throw new BusinessRuleException("Ja existe uma empresa com esse CNPJ");
        }

        applyCompanyData(company, dto, normalizedCnpj);
        Company saved = companyRepository.save(company);
        log.info("Empresa id = {} atualizada com sucesso", saved.getId());
        return saved;
    }

    public void deleteCompany(Long id) {
        Company company = findById(id)
                .orElseThrow(() -> {
                    log.warn("Empresa com id({}) não encontrada para exclusão", id);
                    return new ResourceNotFoundException("Empresa não encontrada");
                });

        companyRepository.delete(company);
        log.info("Empresa com CNPJ: {} id: {} deletada com sucesso!", company.getCnpj(), id);
    }

    private void applyCompanyData(
            Company company,
            CompanyRequestDTO dto,
            String normalizedCnpj
    ) {
        company.setCnpj(normalizedCnpj);
        company.setLegalName(validateRequiredText(dto.legalName(), "A razao social nao pode estar vazia"));
        company.setTradeName(validateRequiredText(dto.tradeName(), "O nome fantasia nao pode estar vazio"));
        company.setStateRegistration(normalizeOptionalText(dto.stateRegistration()));
        company.setLegalNature(validateRequiredValue(dto.legalNature(), "A natureza juridica nao pode estar vazia"));
        company.setTaxRegime(validateRequiredValue(dto.taxRegime(), "O regime tributario nao pode estar vazio"));
        company.setSize(validateRequiredValue(dto.size(), "O porte da empresa nao pode estar vazio"));
        company.setPostalCode(validateRequiredText(dto.postalCode(), "O CEP nao pode estar vazio"));
        company.setStreet(validateRequiredText(dto.street(), "O logradouro nao pode estar vazio"));
        company.setStreetNumber(validateRequiredText(dto.streetNumber(), "O numero nao pode estar vazio"));
        company.setAddressComplement(normalizeOptionalText(dto.addressComplement()));
        company.setNeighborhood(validateRequiredText(dto.neighborhood(), "O bairro nao pode estar vazio"));
        company.setCity(validateRequiredText(dto.city(), "A cidade nao pode estar vazia"));
        company.setState(validateState(dto.state()));
        company.setPhone(validateRequiredText(dto.phone(), "O telefone nao pode estar vazio"));
        company.setEmail(validateRequiredText(dto.email(), "O email nao pode estar vazio"));
        company.setActive(dto.active() == null || dto.active());
    }

    private String validateCnpj(String cnpj) {
        if (cnpj == null || cnpj.isBlank()) {
            throw new IllegalArgumentException("O CNPJ nao pode estar vazio");
        }

        String normalizedCnpj = cnpj.replaceAll("[^0-9]", "");

        if (!CnpjValidator.isValid(normalizedCnpj)) {
            throw new IllegalArgumentException("CNPJ invalido");
        }

        return normalizedCnpj;
    }

    private String validateRequiredText(
            String value,
            String errorMessage
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }

        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String validateState(String state) {
        String normalizedState = validateRequiredText(state, "A UF nao pode estar vazia").toUpperCase();

        if (normalizedState.length() != 2) {
            throw new IllegalArgumentException("A UF deve ter 2 caracteres");
        }

        return normalizedState;
    }

    private <T> T validateRequiredValue(T value, String errorMessage) {
        if (value == null) {
            throw new IllegalArgumentException(errorMessage);
        }

        return value;
    }

    @Transactional(readOnly = true)
    public CompanyLookupResponseDTO lookupByCnpj(String cnpj) {
        String normalizedCnpj = validateCnpj(cnpj);

        BrasilApiCnpjResponse response;
        try {
            response = restClient
                    .get()
                    .uri("https://brasilapi.com.br/api/cnpj/v1/{cnpj}", normalizedCnpj)
                    .retrieve()
                    .body(BrasilApiCnpjResponse.class);
        } catch (RestClientException ex) {
            log.error("Falha ao consultar a BrasilAPI para o CNPJ {}", normalizedCnpj, ex);
            throw new BusinessRuleException("Nao foi possivel consultar os dados do CNPJ no momento");
        }

        if (response == null) {
            log.warn("CNPJ {} nao encontrado na BrasilAPI", normalizedCnpj);
            throw new ResourceNotFoundException("Empresa nao encontrada na BrasilAPI");
        }

        return new CompanyLookupResponseDTO(
                response.cnpj(),
                response.razao_social(),
                response.nome_fantasia(),
                response.cep(),
                response.logradouro(),
                response.numero(),
                response.bairro(),
                response.municipio(),
                response.uf(),
                response.ddd_telefone_1(),
                response.email()
        );
    }

    private record BrasilApiCnpjResponse(
            String cnpj,
            String razao_social,
            String nome_fantasia,
            String cep,
            String logradouro,
            String numero,
            String bairro,
            String municipio,
            String uf,
            String ddd_telefone_1,
            String email
    ) {
    }
}
