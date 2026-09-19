package br.edu.uniamerica.parceiro_auto.controller;

import br.edu.uniamerica.parceiro_auto.controller.dto.ApiResponse;
import br.edu.uniamerica.parceiro_auto.controller.dto.recurrence.RecurrenceRuleRequestDTO;
import br.edu.uniamerica.parceiro_auto.controller.dto.recurrence.RecurringTransactionResponseDTO;
import br.edu.uniamerica.parceiro_auto.entity.Company;
import jakarta.validation.Valid;
import br.edu.uniamerica.parceiro_auto.service.CompanyService;
import br.edu.uniamerica.parceiro_auto.service.RecurrenceRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Tag(name = "Recorrencias")
@RestController
@RequestMapping("api/recurrence-rules")
public class RecurrenceRuleController {
    private final RecurrenceRuleService recurrenceRuleService;
    private final CompanyService companyService;

    public RecurrenceRuleController(RecurrenceRuleService recurrenceRuleService, CompanyService companyService) {
        this.recurrenceRuleService = recurrenceRuleService;
        this.companyService = companyService;
    }

    // GET: consulta as proximas datas; nao cria novas movimentacoes.
    @GetMapping("/company/{companyId}/next")
    @Operation(summary = "Listar proximas movimentacoes recorrentes")
    public ResponseEntity<ApiResponse<List<RecurringTransactionResponseDTO>>> findNextByCompany(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "3") int limit
    ) {
        Company company = companyService.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa nao encontrada"));

        return ResponseEntity.ok(new ApiResponse<>(
                "Proximas recorrencias listadas com sucesso!",
                recurrenceRuleService.findNextByCompany(company, limit)
        ));
    }

    // GET: lista as regras que ainda possuem uma proxima data disponivel.
    @GetMapping("/company/{companyId}")
    @Operation(summary = "Listar recorrencias da empresa")
    public ResponseEntity<ApiResponse<List<RecurringTransactionResponseDTO>>> findByCompany(@PathVariable Long companyId) {
        Company company = companyService.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa nao encontrada"));

        return ResponseEntity.ok(new ApiResponse<>(
                "Recorrencias listadas com sucesso!",
                recurrenceRuleService.findByCompany(company)
        ));
    }

    // PUT: valida o DTO e atualiza frequencia e data final; retorna 200.
    @PutMapping("/{id}")
    @Operation(summary = "Editar recorrencia")
    public ResponseEntity<ApiResponse<RecurringTransactionResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody RecurrenceRuleRequestDTO dto
    ) {
        RecurringTransactionResponseDTO updated = recurrenceRuleService.update(id, dto.frequency(), dto.endDate());
        if (updated == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recorrencia nao encontrada");
        }
        return ResponseEntity.ok(new ApiResponse<>("Recorrencia atualizada com sucesso!", updated));
    }

    // DELETE: encerra a regra, preserva a movimentacao original e retorna 204.
    @DeleteMapping("/{id}")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Recorrencia encerrada", content = @io.swagger.v3.oas.annotations.media.Content)
    @Operation(summary = "Encerrar recorrencia")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!recurrenceRuleService.delete(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recorrencia nao encontrada");
        }

        return ResponseEntity.noContent().build();
    }
}
