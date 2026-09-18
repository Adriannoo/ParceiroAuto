package br.edu.uniamerica.parceiro_auto.controller;

import br.edu.uniamerica.parceiro_auto.controller.dto.ApiResponse;
import br.edu.uniamerica.parceiro_auto.controller.dto.TransactionRequestDTO;
import br.edu.uniamerica.parceiro_auto.controller.dto.TransactionResponseDTO;
import br.edu.uniamerica.parceiro_auto.controller.dto.mapper.TransactionMapper;
import br.edu.uniamerica.parceiro_auto.entity.BankAccount;
import br.edu.uniamerica.parceiro_auto.entity.Company;
import br.edu.uniamerica.parceiro_auto.entity.Transaction;
import br.edu.uniamerica.parceiro_auto.entity.TransactionCategory;
import br.edu.uniamerica.parceiro_auto.exception.ResourceNotFoundException;
import br.edu.uniamerica.parceiro_auto.service.BankAccountService;
import br.edu.uniamerica.parceiro_auto.service.CompanyService;
import br.edu.uniamerica.parceiro_auto.service.TransactionCategoryService;
import br.edu.uniamerica.parceiro_auto.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Movimentações")
@RestController
@RequestMapping("api/transactions")
public class TransactionController {
    private final TransactionService transactionService;
    private final CompanyService companyService;
    private final BankAccountService bankAccountService;
    private final TransactionCategoryService transactionCategoryService;

    public TransactionController(
            TransactionService transactionService,
            CompanyService companyService,
            BankAccountService bankAccountService,
            TransactionCategoryService transactionCategoryService
    ) {
        this.transactionService = transactionService;
        this.companyService = companyService;
        this.bankAccountService = bankAccountService;
        this.transactionCategoryService = transactionCategoryService;
    }

    @PostMapping
    @Operation(summary = "Criar movimentação")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Criado com sucesso", useReturnTypeSchema = true)
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> create(@Validated(TransactionRequestDTO.Create.class) @RequestBody TransactionRequestDTO dto) {
        Company company = companyService.findById(dto.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa nao encontrada"));
        BankAccount bankAccount = bankAccountService.findById(dto.bankAccountId());
        TransactionCategory category = transactionCategoryService.findById(dto.transactionCategoryId());

        Transaction transaction = dto.date() != null
                ? transactionService.createTransaction(company, bankAccount, category, dto.type(), dto.description(), dto.value(), dto.method(), dto.date())
                : transactionService.createTransaction(company, bankAccount, category, dto.type(), dto.description(), dto.value(), dto.method());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Transacao criada com sucesso!", TransactionMapper.toResponseDTO(transaction)));
    }

    @GetMapping("/bank-account/{bankAccountId}")
    @Operation(summary = "Listar movimentações de uma conta")
    public ResponseEntity<ApiResponse<List<TransactionResponseDTO>>> findByBankAccount(@PathVariable Long bankAccountId) {
        BankAccount bankAccount = bankAccountService.findById(bankAccountId);

        List<TransactionResponseDTO> transacoes = transactionService.findByBankAccount(bankAccount).stream()
                .map(TransactionMapper::toResponseDTO)
                .toList();

        return ResponseEntity.ok(new ApiResponse<>("Transacoes listadas com sucesso!", transacoes));
    }

    @GetMapping("/company/{companyId}")
    @Operation(summary = "Listar movimentações de uma empresa")
    public ResponseEntity<ApiResponse<List<TransactionResponseDTO>>> findByCompany(@PathVariable Long companyId) {
        Company company = companyService.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa nao encontrada"));

        List<TransactionResponseDTO> transacoes = transactionService.findByCompany(company).stream()
                .map(TransactionMapper::toResponseDTO)
                .toList();

        return ResponseEntity.ok(new ApiResponse<>("Transacoes listadas com sucesso!", transacoes));
    }

    @GetMapping("/company/{companyId}/last")
    @Operation(summary = "Listar movimentações da empresa com limite")
    public ResponseEntity<ApiResponse<List<TransactionResponseDTO>>> findLastByCompany(@PathVariable Long companyId, @RequestParam int limit) {
        Company company = companyService.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa nao encontrada"));

        List<TransactionResponseDTO> transacoes = transactionService.findLastByCompany(company, limit).stream()
                .map(TransactionMapper::toResponseDTO)
                .toList();

        return ResponseEntity.ok(new ApiResponse<>("Ultimas transacoes listadas com sucesso!", transacoes));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar movimentação")
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> update(@PathVariable Long id, @Valid @RequestBody TransactionRequestDTO dto) {
        Transaction transaction = transactionService.findById(id);
        BankAccount bankAccount = bankAccountService.findById(dto.bankAccountId());
        TransactionCategory category = transactionCategoryService.findById(dto.transactionCategoryId());

        Transaction atualizada = dto.date() != null
                ? transactionService.updateTransaction(transaction, bankAccount, category, dto.type(), dto.description(), dto.value(), dto.method(), dto.date())
                : transactionService.updateTransaction(transaction, bankAccount, category, dto.type(), dto.description(), dto.value(), dto.method());

        return ResponseEntity.ok(new ApiResponse<>("Transacao atualizada com sucesso!", TransactionMapper.toResponseDTO(atualizada)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir movimentação")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Excluído com sucesso, sem corpo de resposta", content = @io.swagger.v3.oas.annotations.media.Content)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Transaction transaction = transactionService.findById(id);
        transactionService.deleteTransaction(transaction);
        return ResponseEntity.noContent().build();
    }
}

