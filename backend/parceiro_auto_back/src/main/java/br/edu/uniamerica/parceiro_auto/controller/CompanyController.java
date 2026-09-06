package br.edu.uniamerica.parceiro_auto.controller;

import br.edu.uniamerica.parceiro_auto.controller.dto.ApiResponse;
import br.edu.uniamerica.parceiro_auto.controller.dto.CompanyRequestDTO;
import br.edu.uniamerica.parceiro_auto.controller.dto.CompanyResponseDTO;
import br.edu.uniamerica.parceiro_auto.controller.dto.mapper.CompanyMapper;
import br.edu.uniamerica.parceiro_auto.entity.Company;
import br.edu.uniamerica.parceiro_auto.service.CompanyService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("api/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    // End point para criar uma nova empresa
    // LOCALHOST:8080/api/companies
    @PostMapping
    public ResponseEntity<ApiResponse<CompanyResponseDTO>> criar(@RequestBody CompanyRequestDTO dto) {
        Company company = companyService.createCompany(dto.cnpj(), dto.legalName(), dto.tradeName());

        CompanyResponseDTO responseDTO = new CompanyResponseDTO(
                company.getId(),
                company.getCnpj(),
                company.getLegalName(),
                company.getTradeName()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Empresa criada com sucesso!", responseDTO));
    }

    // End point para pegar os dados da empresa
    // GET LOCALHOST:8080/api/companies/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyResponseDTO>> findById(@PathVariable Long id) {
        Optional<Company> company = companyService.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Empresa nao encontrada"
                        )
                );

        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>("Dados da empresa encontrados com sucesso!", CompanyMapper.toResponseDTO(company)));

    }


}
