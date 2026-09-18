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

// Agrupa os endpoints de movimentacoes na documentacao do Swagger.
@Tag(name = "Movimentações")
@RestController
@RequestMapping("api/transactions")
public class TransactionController {
    private final TransactionService transactionService;
    private final CompanyService companyService;
    private final BankAccountService bankAccountService;
    private final TransactionCategoryService transactionCategoryService;
    private final RecurrenceRuleService recurrenceRuleService;

    // Recebe os services usados nas movimentacoes, nos vinculos e nas regras de recorrencia.
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


    // Endpoint para listar todas as movimentacoes
    // GET localhost:8080/api/transactions
    // Queremos devolver 200 OK, que e o status generico para sucesso
    @GetMapping
    @Operation(summary = "Listar movimentações")
    public ResponseEntity<ApiResponse<List<TransactionResponseDTO>>> findAll() {
        // Converte cada movimentacao em DTO e inclui sua regra de recorrencia, quando houver.
        List<TransactionResponseDTO> transacoes = transactionService.findAll()
                .stream()
                .map(transaction -> TransactionMapper.toResponseDTO(transaction, recurrenceRuleService.findByTransaction(transaction)))
                .toList();

        return ResponseEntity.ok(new ApiResponse<>("Transacoes listadas com sucesso!", transacoes));
    }

    // Endpoint para buscar uma movimentacao pelo ID
    // GET localhost:8080/api/transactions/{id}
    // Queremos devolver 200 OK, que e o status generico para sucesso
    @GetMapping("/{id}")
    @Operation(summary = "Buscar movimentação por ID")
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> findById(@PathVariable Long id) {
        Transaction transaction = transactionService.findById(id);

        return ResponseEntity.ok(new ApiResponse<>("Transacao encontrada com sucesso!", TransactionMapper.toResponseDTO(transaction, recurrenceRuleService.findByTransaction(transaction))));
    }

    // Endpoint para criar uma movimentacao vinculada a empresa, conta e categoria
    // POST localhost:8080/api/transactions
    // Queremos devolver 201 CREATED, que e o status para criacao
    // @Validated aplica o grupo Create, que exige companyId e as demais regras comuns do DTO.
    @PostMapping
    @Operation(summary = "Criar movimentação")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Criado com sucesso", useReturnTypeSchema = true)
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> create(@Validated(TransactionRequestDTO.Create.class) @RequestBody TransactionRequestDTO dto) {
        validateRecurrence(dto.date() == null ? LocalDate.now() : dto.date(), dto);

        Company company = companyService.findById(dto.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa nao encontrada"));
        BankAccount bankAccount = bankAccountService.findById(dto.bankAccountId());
        TransactionCategory category = transactionCategoryService.findById(dto.transactionCategoryId());

        // Usa a data recebida ou deixa o service assumir a data atual quando ela nao vier.
        Transaction transaction = dto.date() != null
                ? transactionService.createTransaction(company, bankAccount, category, dto.type(), dto.description(), dto.value(), dto.method(), dto.date())
                : transactionService.createTransaction(company, bankAccount, category, dto.type(), dto.description(), dto.value(), dto.method());

        // Apos criar a movimentacao, configura a recorrencia conforme os dados recebidos.
        recurrenceRuleService.saveForTransaction(transaction, dto.recurrenceFrequency(), dto.recurrenceEndDate());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Transacao criada com sucesso!", TransactionMapper.toResponseDTO(transaction, recurrenceRuleService.findByTransaction(transaction))));
    }

    // Endpoint para listar as movimentacoes de uma conta bancaria
    // GET localhost:8080/api/transactions/bank-account/{bankAccountId}
    // Queremos devolver 200 OK com a lista de movimentacoes
    @GetMapping("/bank-account/{bankAccountId}")
    @Operation(summary = "Listar movimentações de uma conta")
    public ResponseEntity<ApiResponse<List<TransactionResponseDTO>>> findByBankAccount(@PathVariable Long bankAccountId) {
        BankAccount bankAccount = bankAccountService.findById(bankAccountId);

        List<TransactionResponseDTO> transacoes = transactionService.findByBankAccount(bankAccount).stream()
                .map(transaction -> TransactionMapper.toResponseDTO(transaction, recurrenceRuleService.findByTransaction(transaction)))
                .toList();

        return ResponseEntity.ok(new ApiResponse<>("Transacoes listadas com sucesso!", transacoes));
    }

    // Endpoint para listar as movimentacoes de uma empresa
    // GET localhost:8080/api/transactions/company/{companyId}
    // Queremos devolver 200 OK com a lista de movimentacoes
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

    // Endpoint para buscar as ultimas movimentacoes da empresa, ordenadas por data e ID
    // GET localhost:8080/api/transactions/company/{companyId}/last?limit=5
    // Queremos devolver 200 OK; o service valida o limite de 1 a 100 e aplica no banco
    // @RequestParam recebe o limite da URL e @Parameter descreve esse campo no Swagger.
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

    // Endpoint para atualizar uma movimentacao e sua regra de recorrencia
    // PUT localhost:8080/api/transactions/{id}
    // Queremos devolver 200 OK com os dados atualizados
    // @Valid verifica as regras comuns do DTO; a empresa original da movimentacao e mantida.
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar movimentação")
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> update(@PathVariable Long id, @Valid @RequestBody TransactionRequestDTO dto) {
        Transaction transaction = transactionService.findById(id);
        validateRecurrence(dto.date() == null ? transaction.getDate() : dto.date(), dto);

        BankAccount bankAccount = bankAccountService.findById(dto.bankAccountId());
        TransactionCategory category = transactionCategoryService.findById(dto.transactionCategoryId());

        // Atualiza a data quando informada; caso contrario, preserva a data original.
        // O service desfaz o efeito anterior no saldo e aplica os novos valores.
        Transaction atualizada = dto.date() != null
                ? transactionService.updateTransaction(transaction, bankAccount, category, dto.type(), dto.description(), dto.value(), dto.method(), dto.date())
                : transactionService.updateTransaction(transaction, bankAccount, category, dto.type(), dto.description(), dto.value(), dto.method());

        recurrenceRuleService.saveForTransaction(atualizada, dto.recurrenceFrequency(), dto.recurrenceEndDate());

        return ResponseEntity.ok(new ApiResponse<>("Transacao atualizada com sucesso!", TransactionMapper.toResponseDTO(atualizada, recurrenceRuleService.findByTransaction(atualizada))));
    }

    // Valida a recorrencia antes de salvar ou atualizar a movimentacao.
    // Devolve 400 BAD REQUEST quando a data final nao combina com os dados recebidos.
    private void validateRecurrence(LocalDate transactionDate, TransactionRequestDTO dto) {
        // Uma data final so faz sentido quando existe uma frequencia de recorrencia.
        if (dto.recurrenceFrequency() == null && dto.recurrenceEndDate() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A data final exige uma frequencia de recorrencia"
            );
        }

        // A recorrencia nao pode terminar antes da data da propria movimentacao.
        if (dto.recurrenceEndDate() != null && dto.recurrenceEndDate().isBefore(transactionDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A data final nao pode ser anterior a data da transacao"
            );
        }
    }

    // Endpoint para excluir uma movimentacao
    // DELETE localhost:8080/api/transactions/{id}
    // Queremos devolver 204 NO CONTENT, sem corpo de resposta
    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir movimentação")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Excluído com sucesso, sem corpo de resposta", content = @io.swagger.v3.oas.annotations.media.Content)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Transaction transaction = transactionService.findById(id);
        // Remove primeiro a regra vinculada; depois o service reverte o saldo e exclui a movimentacao.
        recurrenceRuleService.deleteForTransaction(transaction);
        transactionService.deleteTransaction(transaction);
        return ResponseEntity.noContent().build();
    }
}
