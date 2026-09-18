package br.edu.uniamerica.parceiro_auto.controller;

import br.edu.uniamerica.parceiro_auto.controller.dto.ApiResponse;
import br.edu.uniamerica.parceiro_auto.controller.dto.TransactionCategoryRequestDTO;
import br.edu.uniamerica.parceiro_auto.controller.dto.TransactionCategoryResponseDTO;
import br.edu.uniamerica.parceiro_auto.controller.dto.mapper.TransactionCategoryMapper;
import br.edu.uniamerica.parceiro_auto.entity.Company;
import br.edu.uniamerica.parceiro_auto.entity.TransactionCategory;
import br.edu.uniamerica.parceiro_auto.service.CompanyService;
import br.edu.uniamerica.parceiro_auto.service.TransactionCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Tag(name = "Categorias de movimentacao")
@RestController
@RequestMapping("api/transaction-categories")
public class TransactionCategoryController {

    private final TransactionCategoryService transactionCategoryService;
    private final CompanyService companyService;

    public TransactionCategoryController(
            TransactionCategoryService transactionCategoryService,
            CompanyService companyService
    ) {
        this.transactionCategoryService = transactionCategoryService;
        this.companyService = companyService;
    }

    @GetMapping("/company/{companyId}")
    @Operation(summary = "Listar categorias de movimentacao da empresa")
    public ResponseEntity<ApiResponse<List<TransactionCategoryResponseDTO>>> findByCompany(@PathVariable Long companyId) {
        Company company = companyService.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa nao encontrada"));

        List<TransactionCategoryResponseDTO> categories = transactionCategoryService.findActiveByCompany(company)
                .stream()
                .map(TransactionCategoryMapper::toResponseDTO)
                .toList();

        return ResponseEntity.ok(new ApiResponse<>("Categorias listadas com sucesso!", categories));
    }

    @PostMapping("/company/{companyId}")
    @Operation(summary = "Cadastrar categoria de movimentacao")
    public ResponseEntity<ApiResponse<TransactionCategoryResponseDTO>> create(
            @PathVariable Long companyId,
            @Valid @RequestBody TransactionCategoryRequestDTO dto
    ) {
        Company company = companyService.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa nao encontrada"));

        TransactionCategory category;

        try {
            category = transactionCategoryService.createCategory(company, dto.name(), dto.type());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Categoria criada com sucesso!", TransactionCategoryMapper.toResponseDTO(category)));
    }

    @DeleteMapping("/company/{companyId}/{categoryId}")
    @Operation(summary = "Inativar categoria de movimentacao")
    public ResponseEntity<Void> delete(@PathVariable Long companyId, @PathVariable Long categoryId) {
        Company company = companyService.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa nao encontrada"));

        try {
            transactionCategoryService.deactivateCategory(company, categoryId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/company/{companyId}/{categoryId}")
    @Operation(summary = "Atualizar categoria de movimentacao")
    public ResponseEntity<ApiResponse<TransactionCategoryResponseDTO>> update(
            @PathVariable Long companyId,
            @PathVariable Long categoryId,
            @Valid @RequestBody TransactionCategoryRequestDTO dto
    ) {
        Company company = companyService.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa nao encontrada"));

        TransactionCategory category;

        try {
            category = transactionCategoryService.updateCategory(company, categoryId, dto.name(), dto.type());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }

        return ResponseEntity.ok(new ApiResponse<>("Categoria atualizada com sucesso!", TransactionCategoryMapper.toResponseDTO(category)));
    }
}
