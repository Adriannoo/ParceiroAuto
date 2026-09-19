package br.edu.uniamerica.parceiro_auto.service;

import br.edu.uniamerica.parceiro_auto.controller.dto.transaction.TransactionRequestDTO;
import br.edu.uniamerica.parceiro_auto.controller.dto.transaction.TransactionResponseDTO;
import br.edu.uniamerica.parceiro_auto.controller.dto.mapper.TransactionMapper;
import br.edu.uniamerica.parceiro_auto.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import br.edu.uniamerica.parceiro_auto.entity.Transaction;
import br.edu.uniamerica.parceiro_auto.repository.RecurrenceRuleRepository;

// Mantem movimentacao, saldo e recorrencia na mesma transacao de banco.
@Service
@RequiredArgsConstructor
@Transactional
public class TransactionApplicationService {
    private final TransactionService transactionService;
    private final CompanyService companyService;
    private final BankAccountService bankAccountService;
    private final TransactionCategoryService transactionCategoryService;
    private final RecurrenceRuleService recurrenceRuleService;
    private final RecurrenceRuleRepository recurrenceRuleRepository;

    // Busca as recorrencias em lote, evitando uma consulta por movimentacao.
    @Transactional(readOnly = true)
    public List<TransactionResponseDTO> toResponseDTOs(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return List.of();
        }
        var rules = recurrenceRuleRepository.findByTransactionIn(transactions).stream()
                .collect(Collectors.toMap(rule -> rule.getTransaction().getId(), rule -> rule));
        return transactions.stream()
                .map(transaction -> TransactionMapper.toResponseDTO(transaction, rules.get(transaction.getId())))
                .toList();
    }

    // Se qualquer etapa falhar, o Spring desfaz todas as alteracoes desta operacao.
    public TransactionResponseDTO create(TransactionRequestDTO dto) {
        LocalDate date = dto.date() == null ? LocalDate.now() : dto.date();
        validateRecurrence(date, dto);
        var company = companyService.findById(dto.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa nao encontrada"));
        var account = bankAccountService.findById(dto.bankAccountId());
        var category = transactionCategoryService.findById(dto.transactionCategoryId());
        var transaction = transactionService.createTransaction(company, account, category,
                dto.type(), dto.description(), dto.value(), dto.method(), date);
        recurrenceRuleService.saveForTransaction(transaction, dto.recurrenceFrequency(), dto.recurrenceEndDate());
        return TransactionMapper.toResponseDTO(transaction, recurrenceRuleService.findByTransaction(transaction));
    }

    // Preserva a empresa e a data original quando a nova data nao for informada.
    public TransactionResponseDTO update(Long id, TransactionRequestDTO dto) {
        var transaction = transactionService.findById(id);
        LocalDate date = dto.date() == null ? transaction.getDate() : dto.date();
        validateRecurrence(date, dto);
        var account = bankAccountService.findById(dto.bankAccountId());
        var category = transactionCategoryService.findById(dto.transactionCategoryId());
        var updated = transactionService.updateTransaction(transaction, account, category,
                dto.type(), dto.description(), dto.value(), dto.method(), date);
        recurrenceRuleService.saveForTransaction(updated, dto.recurrenceFrequency(), dto.recurrenceEndDate());
        return TransactionMapper.toResponseDTO(updated, recurrenceRuleService.findByTransaction(updated));
    }

    // Remove a regra e a movimentacao, revertendo o saldo na mesma transacao.
    public void delete(Long id) {
        var transaction = transactionService.findById(id);
        recurrenceRuleService.deleteForTransaction(transaction);
        transactionService.deleteTransaction(transaction);
    }

    // Valida as datas antes de alterar a movimentacao ou o saldo.
    private void validateRecurrence(LocalDate date, TransactionRequestDTO dto) {
        if (dto.recurrenceFrequency() == null && dto.recurrenceEndDate() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A data final exige uma frequencia de recorrencia");
        }
        if (dto.recurrenceEndDate() != null && dto.recurrenceEndDate().isBefore(date)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A data final nao pode ser anterior a data da transacao");
        }
    }
}
