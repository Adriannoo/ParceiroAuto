package br.edu.uniamerica.parceiro_auto.controller;

import br.edu.uniamerica.parceiro_auto.controller.dto.ApiResponse;
import br.edu.uniamerica.parceiro_auto.controller.dto.transaction.TransactionRequestDTO;
import br.edu.uniamerica.parceiro_auto.controller.dto.transaction.TransactionResponseDTO;
import br.edu.uniamerica.parceiro_auto.controller.dto.mapper.TransactionMapper;
import br.edu.uniamerica.parceiro_auto.entity.BankAccount;
import br.edu.uniamerica.parceiro_auto.entity.Company;
import br.edu.uniamerica.parceiro_auto.entity.Transaction;
import br.edu.uniamerica.parceiro_auto.exception.ResourceNotFoundException;
import br.edu.uniamerica.parceiro_auto.service.BankAccountService;
import br.edu.uniamerica.parceiro_auto.service.CompanyService;
import br.edu.uniamerica.parceiro_auto.service.RecurrenceRuleService;
import br.edu.uniamerica.parceiro_auto.service.TransactionApplicationService;
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

import java.util.List;

// Agrupa os endpoints de movimentacoes na documentacao do Swagger.
@Tag(name = "Movimentações")
@RestController
@RequestMapping("api/transactions")
public class TransactionController {
    private final TransactionService transactionService;
    private final CompanyService companyService;
    private final BankAccountService bankAccountService;
    private final TransactionApplicationService transactionApplicationService;
    private final RecurrenceRuleService recurrenceRuleService;

    // Recebe os services usados nas movimentacoes, nos vinculos e nas regras de recorrencia.
    public TransactionController(
            TransactionService transactionService,
            CompanyService companyService,
            BankAccountService bankAccountService,
            TransactionApplicationService transactionApplicationService,
            RecurrenceRuleService recurrenceRuleService
    ) {
        this.transactionService = transactionService;
        this.companyService = companyService;
        this.bankAccountService = bankAccountService;
        this.transactionApplicationService = transactionApplicationService;
        this.recurrenceRuleService = recurrenceRuleService;
    }


    // Endpoint para listar todas as movimentacoes
    // GET localhost:8080/api/transactions
    // Queremos devolver 200 OK, que e o status generico para sucesso
    @GetMapping
    @Operation(summary = "Listar movimentações")
    public ResponseEntity<ApiResponse<List<TransactionResponseDTO>>> findAll() {
        // Converte cada movimentacao em DTO e inclui sua regra de recorrencia, quando houver.
        List<TransactionResponseDTO> transacoes = transactionApplicationService.toResponseDTOs(transactionService.findAll());

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
        // O service salva movimentacao, saldo e recorrencia como uma unica operacao.
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Transacao criada com sucesso!", transactionApplicationService.create(dto)));
    }

    // Endpoint para listar as movimentacoes de uma conta bancaria
    // GET localhost:8080/api/transactions/bank-account/{bankAccountId}
    // Queremos devolver 200 OK com a lista de movimentacoes
    @GetMapping("/bank-account/{bankAccountId}")
    @Operation(summary = "Listar movimentações de uma conta")
    public ResponseEntity<ApiResponse<List<TransactionResponseDTO>>> findByBankAccount(@PathVariable Long bankAccountId) {
        BankAccount bankAccount = bankAccountService.findById(bankAccountId);

        List<TransactionResponseDTO> transacoes = transactionApplicationService.toResponseDTOs(transactionService.findByBankAccount(bankAccount));

        return ResponseEntity.ok(new ApiResponse<>("Transacoes listadas com sucesso!", transacoes));
    }

    // Endpoint para listar as movimentacoes de uma empresa
    // GET localhost:8080/api/transactions/company/{companyId}
    // Queremos devolver 200 OK com a lista de movimentacoes
    @GetMapping("/company/{companyId}")
    @Operation(summary = "Listar movimentações de uma empresa")
    public ResponseEntity<ApiResponse<List<TransactionResponseDTO>>> findByCompany(@PathVariable Long companyId) {
        Company company = companyService.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa nao encontrada"));

        List<TransactionResponseDTO> transacoes = transactionApplicationService.toResponseDTOs(transactionService.findByCompany(company));

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
                .orElseThrow(() -> new ResourceNotFoundException("Empresa nao encontrada"));

        List<TransactionResponseDTO> transacoes = transactionApplicationService.toResponseDTOs(transactionService.findLastByCompany(company, limit));

        return ResponseEntity.ok(new ApiResponse<>("Ultimas transacoes listadas com sucesso!", transacoes));
    }

    // Endpoint para atualizar uma movimentacao e sua regra de recorrencia
    // PUT localhost:8080/api/transactions/{id}
    // Queremos devolver 200 OK com os dados atualizados
    // @Valid verifica as regras comuns do DTO; a empresa original da movimentacao e mantida.
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar movimentação")
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> update(@PathVariable Long id, @Valid @RequestBody TransactionRequestDTO dto) {
        // Uma falha na recorrencia tambem desfaz a atualizacao da movimentacao e do saldo.
        return ResponseEntity.ok(new ApiResponse<>("Transacao atualizada com sucesso!", transactionApplicationService.update(id, dto)));
    }

    // Endpoint para excluir uma movimentacao
    // DELETE localhost:8080/api/transactions/{id}
    // Queremos devolver 204 NO CONTENT, sem corpo de resposta
    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir movimentação")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Excluído com sucesso, sem corpo de resposta", content = @io.swagger.v3.oas.annotations.media.Content)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        // O service remove a regra, reverte o saldo e exclui a movimentacao juntos.
        transactionApplicationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
