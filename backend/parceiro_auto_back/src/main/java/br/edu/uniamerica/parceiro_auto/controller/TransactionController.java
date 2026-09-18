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
import br.edu.uniamerica.parceiro_auto.service.RecurrenceRuleService;
import br.edu.uniamerica.parceiro_auto.service.TransactionCategoryService;
import br.edu.uniamerica.parceiro_auto.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.time.LocalDate;

@Tag(name = "Movimentações")
@RestController
@RequestMapping("api/transactions")
public class TransactionController {
    private final TransactionService transactionService;
    private final CompanyService companyService;
    private final BankAccountService bankAccountService;
    private final TransactionCategoryService transactionCategoryService;
    private final RecurrenceRuleService recurrenceRuleService;

    public TransactionController(
            TransactionService transactionService,
            CompanyService companyService,
            BankAccountService bankAccountService,
            TransactionCategoryService transactionCategoryService,
            RecurrenceRuleService recurrenceRuleService
    ) {
        this.transactionService = transactionService;
        this.companyService = companyService;
        this.bankAccountService = bankAccountService;
        this.transactionCategoryService = transactionCategoryService;
        this.recurrenceRuleService = recurrenceRuleService;
    }

    @GetMapping
    @Operation(summary = "Listar movimentações")
    public ResponseEntity<ApiResponse<List<TransactionResponseDTO>>> findAll() {
        List<TransactionResponseDTO> transacoes = transactionService.findAll()
                .stream()
                .map(transaction -> TransactionMapper.toResponseDTO(transaction, recurrenceRuleService.findByTransaction(transaction)))
                .toList();

        return ResponseEntity.ok(new ApiResponse<>("Transacoes listadas com sucesso!", transacoes));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar movimentação por ID")
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> findById(@PathVariable Long id) {
        Transaction transaction = transactionService.findById(id);

        return ResponseEntity.ok(new ApiResponse<>("Transacao encontrada com sucesso!", TransactionMapper.toResponseDTO(transaction, recurrenceRuleService.findByTransaction(transaction))));
    }

    @PostMapping
    @Operation(summary = "Criar movimentação")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Criado com sucesso", useReturnTypeSchema = true)
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> create(@Validated(TransactionRequestDTO.Create.class) @RequestBody TransactionRequestDTO dto) {
        validateRecurrence(dto.date() == null ? LocalDate.now() : dto.date(), dto);

        Company company = companyService.findById(dto.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa nao encontrada"));
        BankAccount bankAccount = bankAccountService.findById(dto.bankAccountId());
        TransactionCategory category = transactionCategoryService.findById(dto.transactionCategoryId());

        Transaction transaction = dto.date() != null
                ? transactionService.createTransaction(company, bankAccount, category, dto.type(), dto.description(), dto.value(), dto.method(), dto.date())
                : transactionService.createTransaction(company, bankAccount, category, dto.type(), dto.description(), dto.value(), dto.method());

        recurrenceRuleService.saveForTransaction(transaction, dto.recurrenceFrequency(), dto.recurrenceEndDate());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Transacao criada com sucesso!", TransactionMapper.toResponseDTO(transaction, recurrenceRuleService.findByTransaction(transaction))));
    }

    @GetMapping("/bank-account/{bankAccountId}")
    @Operation(summary = "Listar movimentações de uma conta")
    public ResponseEntity<ApiResponse<List<TransactionResponseDTO>>> findByBankAccount(@PathVariable Long bankAccountId) {
        BankAccount bankAccount = bankAccountService.findById(bankAccountId);

        List<TransactionResponseDTO> transacoes = transactionService.findByBankAccount(bankAccount).stream()
                .map(transaction -> TransactionMapper.toResponseDTO(transaction, recurrenceRuleService.findByTransaction(transaction)))
                .toList();

        return ResponseEntity.ok(new ApiResponse<>("Transacoes listadas com sucesso!", transacoes));
    }

    @GetMapping("/company/{companyId}")
    @Operation(summary = "Listar movimentações de uma empresa")
    public ResponseEntity<ApiResponse<List<TransactionResponseDTO>>> findByCompany(@PathVariable Long companyId) {
        Company company = companyService.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa nao encontrada"));

        List<TransactionResponseDTO> transacoes = transactionService.findByCompany(company).stream()
                .map(transaction -> TransactionMapper.toResponseDTO(transaction, recurrenceRuleService.findByTransaction(transaction)))
                .toList();

        return ResponseEntity.ok(new ApiResponse<>("Transacoes listadas com sucesso!", transacoes));
    }

    @GetMapping("/company/{companyId}/last")
    @Operation(summary = "Listar movimentações da empresa com limite")
    public ResponseEntity<ApiResponse<List<TransactionResponseDTO>>> findLastByCompany(
            @PathVariable Long companyId,
            @Parameter(description = "Quantidade de movimentacoes, entre 1 e 100", schema = @Schema(minimum = "1", maximum = "100"))
            @RequestParam int limit) {
        Company company = companyService.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa nao encontrada"));

        List<TransactionResponseDTO> transacoes = transactionService.findLastByCompany(company, limit).stream()
                .map(transaction -> TransactionMapper.toResponseDTO(transaction, recurrenceRuleService.findByTransaction(transaction)))
                .toList();

        return ResponseEntity.ok(new ApiResponse<>("Ultimas transacoes listadas com sucesso!", transacoes));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar movimentação")
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> update(@PathVariable Long id, @Valid @RequestBody TransactionRequestDTO dto) {
        Transaction transaction = transactionService.findById(id);
        validateRecurrence(dto.date() == null ? transaction.getDate() : dto.date(), dto);

        BankAccount bankAccount = bankAccountService.findById(dto.bankAccountId());
        TransactionCategory category = transactionCategoryService.findById(dto.transactionCategoryId());

        Transaction atualizada = dto.date() != null
                ? transactionService.updateTransaction(transaction, bankAccount, category, dto.type(), dto.description(), dto.value(), dto.method(), dto.date())
                : transactionService.updateTransaction(transaction, bankAccount, category, dto.type(), dto.description(), dto.value(), dto.method());

        recurrenceRuleService.saveForTransaction(atualizada, dto.recurrenceFrequency(), dto.recurrenceEndDate());

        return ResponseEntity.ok(new ApiResponse<>("Transacao atualizada com sucesso!", TransactionMapper.toResponseDTO(atualizada, recurrenceRuleService.findByTransaction(atualizada))));
    }

    private void validateRecurrence(LocalDate transactionDate, TransactionRequestDTO dto) {
        if (dto.recurrenceFrequency() == null && dto.recurrenceEndDate() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A data final exige uma frequencia de recorrencia"
            );
        }

        if (dto.recurrenceEndDate() != null && dto.recurrenceEndDate().isBefore(transactionDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A data final nao pode ser anterior a data da transacao"
            );
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir movimentação")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Excluído com sucesso, sem corpo de resposta", content = @io.swagger.v3.oas.annotations.media.Content)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Transaction transaction = transactionService.findById(id);
        recurrenceRuleService.deleteForTransaction(transaction);
        transactionService.deleteTransaction(transaction);
        return ResponseEntity.noContent().build();
    }
}
