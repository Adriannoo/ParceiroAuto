package br.edu.uniamerica.parceiro_auto.controller;

import br.edu.uniamerica.parceiro_auto.controller.dto.ApiResponse;
import br.edu.uniamerica.parceiro_auto.controller.dto.category.TransactionCategoryRequestDTO;
import br.edu.uniamerica.parceiro_auto.controller.dto.category.TransactionCategoryResponseDTO;
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

// Agrupa os endpoints de categorias na documentacao do Swagger.
@Tag(name = "Categorias de movimentacao")
@RestController
@RequestMapping("api/transaction-categories")
public class TransactionCategoryController {

    private final TransactionCategoryService transactionCategoryService;
    private final CompanyService companyService;

    // Recebe os services pelo construtor para consultar empresas e gerenciar categorias.
    public TransactionCategoryController(
            TransactionCategoryService transactionCategoryService,
            CompanyService companyService
    ) {
        this.transactionCategoryService = transactionCategoryService;
        this.companyService = companyService;
    }

    // Endpoint para listar as categorias ativas de uma empresa
    // GET localhost:8080/api/transaction-categories/company/{companyId}
    // Queremos devolver 200 OK, que e o status generico para sucesso
    @GetMapping("/company/{companyId}")
    @Operation(summary = "Listar categorias de movimentacao da empresa")
    public ResponseEntity<ApiResponse<List<TransactionCategoryResponseDTO>>> findByCompany(@PathVariable Long companyId) {
        Company company = companyService.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa nao encontrada"));

        // Converte as entidades em DTOs para devolver apenas os dados da resposta.
        List<TransactionCategoryResponseDTO> categories = transactionCategoryService.findActiveByCompany(company)
                .stream()
                .map(TransactionCategoryMapper::toResponseDTO)
                .toList();

        return ResponseEntity.ok(new ApiResponse<>("Categorias listadas com sucesso!", categories));
    }

    // Endpoint para cadastrar uma categoria vinculada a empresa informada na URL
    // POST localhost:8080/api/transaction-categories/company/{companyId}
    // Queremos devolver 201 CREATED, que e o status para criacao
    // @Valid verifica as restricoes do DTO recebido pelo @RequestBody.
    @PostMapping("/company/{companyId}")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Categoria criada", useReturnTypeSchema = true)
    @Operation(summary = "Cadastrar categoria de movimentacao")
    public ResponseEntity<ApiResponse<TransactionCategoryResponseDTO>> create(
            @PathVariable Long companyId,
            @Valid @RequestBody TransactionCategoryRequestDTO dto
    ) {
        Company company = companyService.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa nao encontrada"));

        // O tratamento dos erros fica centralizado no GlobalExceptionHandler.
        TransactionCategory category = transactionCategoryService.createCategory(company, dto.name(), dto.type());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Categoria criada com sucesso!", TransactionCategoryMapper.toResponseDTO(category)));
    }

    // Endpoint para inativar uma categoria, sem apagar seu registro do banco
    // DELETE localhost:8080/api/transaction-categories/company/{companyId}/{categoryId}
    // Queremos devolver 204 NO CONTENT, sem corpo de resposta
    @DeleteMapping("/company/{companyId}/{categoryId}")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Categoria inativada", content = @io.swagger.v3.oas.annotations.media.Content)
    @Operation(summary = "Inativar categoria de movimentacao")
    public ResponseEntity<Void> delete(@PathVariable Long companyId, @PathVariable Long categoryId) {
        Company company = companyService.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa nao encontrada"));

        transactionCategoryService.deactivateCategory(company, categoryId);

        return ResponseEntity.noContent().build();
    }

    // Endpoint para atualizar o nome e o tipo de uma categoria da empresa
    // PUT localhost:8080/api/transaction-categories/company/{companyId}/{categoryId}
    // Queremos devolver 200 OK com os dados atualizados
    @PutMapping("/company/{companyId}/{categoryId}")
    @Operation(summary = "Atualizar categoria de movimentacao")
    public ResponseEntity<ApiResponse<TransactionCategoryResponseDTO>> update(
            @PathVariable Long companyId,
            @PathVariable Long categoryId,
            @Valid @RequestBody TransactionCategoryRequestDTO dto
    ) {
        Company company = companyService.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa nao encontrada"));

        TransactionCategory category = transactionCategoryService.updateCategory(company, categoryId, dto.name(), dto.type());

        return ResponseEntity.ok(new ApiResponse<>("Categoria atualizada com sucesso!", TransactionCategoryMapper.toResponseDTO(category)));
    }
}
