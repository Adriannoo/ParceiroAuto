package br.edu.uniamerica.parceiro_auto.controller;

import br.edu.uniamerica.parceiro_auto.controller.dto.ApiResponse;
import br.edu.uniamerica.parceiro_auto.controller.dto.TransactionRequestDTO;
import br.edu.uniamerica.parceiro_auto.controller.dto.TransactionResponseDTO;
import br.edu.uniamerica.parceiro_auto.controller.dto.mapper.TransactionMapper;
import br.edu.uniamerica.parceiro_auto.entity.BankAccount;
import br.edu.uniamerica.parceiro_auto.entity.Company;
import br.edu.uniamerica.parceiro_auto.entity.Transaction;
import br.edu.uniamerica.parceiro_auto.entity.TransactionCategory;
import br.edu.uniamerica.parceiro_auto.service.BankAccountService;
import br.edu.uniamerica.parceiro_auto.service.CompanyService;
import br.edu.uniamerica.parceiro_auto.service.TransactionCategoryService;
import br.edu.uniamerica.parceiro_auto.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> create(@RequestBody TransactionRequestDTO dto) {
        Company company = companyService.findById(dto.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa nao encontrada"));
        BankAccount bankAccount = bankAccountService.findById(dto.bankAccountId());
        TransactionCategory category = transactionCategoryService.findById(dto.transactionCategoryId());

        Transaction transaction = dto.date() != null
                ? transactionService.createTransaction(company, bankAccount, category, dto.type(), dto.description(), dto.value(), dto.method(), dto.date())
                : transactionService.createTransaction(company, bankAccount, category, dto.type(), dto.description(), dto.value(), dto.method());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Transacao criada com sucesso!", TransactionMapper.toResponseDTO(transaction)));
    }

    @GetMapping("/bank-account/{bankAccountId}")
    public ResponseEntity<ApiResponse<List<TransactionResponseDTO>>> findByBankAccount(@PathVariable Long bankAccountId) {
        BankAccount bankAccount = bankAccountService.findById(bankAccountId);

        List<TransactionResponseDTO> transacoes = transactionService.findByBankAccount(bankAccount).stream()
                .map(TransactionMapper::toResponseDTO)
                .toList();

        return ResponseEntity.ok(new ApiResponse<>("Transacoes listadas com sucesso!", transacoes));
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<ApiResponse<List<TransactionResponseDTO>>> findByCompany(@PathVariable Long companyId) {
        Company company = companyService.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa nao encontrada"));

        List<TransactionResponseDTO> transacoes = transactionService.findByCompany(company).stream()
                .map(TransactionMapper::toResponseDTO)
                .toList();

        return ResponseEntity.ok(new ApiResponse<>("Transacoes listadas com sucesso!", transacoes));
    }

    @GetMapping("/company/{companyId}/last")
    public ResponseEntity<ApiResponse<List<TransactionResponseDTO>>> findLastByCompany(@PathVariable Long companyId, @RequestParam int limit) {
        Company company = companyService.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa nao encontrada"));

        List<TransactionResponseDTO> transacoes = transactionService.findLastByCompany(company, limit).stream()
                .map(TransactionMapper::toResponseDTO)
                .toList();

        return ResponseEntity.ok(new ApiResponse<>("Ultimas transacoes listadas com sucesso!", transacoes));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> update(@PathVariable Long id, @RequestBody TransactionRequestDTO dto) {
        Transaction transaction = transactionService.findById(id);
        BankAccount bankAccount = bankAccountService.findById(dto.bankAccountId());
        TransactionCategory category = transactionCategoryService.findById(dto.transactionCategoryId());

        Transaction atualizada = dto.date() != null
                ? transactionService.updateTransaction(transaction, bankAccount, category, dto.type(), dto.description(), dto.value(), dto.method(), dto.date())
                : transactionService.updateTransaction(transaction, bankAccount, category, dto.type(), dto.description(), dto.value(), dto.method());

        return ResponseEntity.ok(new ApiResponse<>("Transacao atualizada com sucesso!", TransactionMapper.toResponseDTO(atualizada)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Transaction transaction = transactionService.findById(id);
        transactionService.deleteTransaction(transaction);
        return ResponseEntity.noContent().build();
    }
}

