package br.edu.uniamerica.parceiro_auto.service;

import br.edu.uniamerica.parceiro_auto.controller.dto.RecurringTransactionResponseDTO;
import br.edu.uniamerica.parceiro_auto.entity.Company;
import br.edu.uniamerica.parceiro_auto.entity.RecurrenceRule;
import br.edu.uniamerica.parceiro_auto.entity.Transaction;
import br.edu.uniamerica.parceiro_auto.entity.enums.RecurrenceFrequency;
import br.edu.uniamerica.parceiro_auto.repository.RecurrenceRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RecurrenceRuleService {
    private final RecurrenceRuleRepository recurrenceRuleRepository;

    public void saveForTransaction(Transaction transaction, RecurrenceFrequency frequency, LocalDate endDate) {
        if (transaction == null) {
            deleteForTransaction(transaction);
            return;
        }

        if (frequency == null) {
            if (endDate != null) {
                throw new IllegalArgumentException(
                        "A data final exige uma frequencia de recorrencia"
                );
            }

            deleteForTransaction(transaction);
            return;
        }

        if (endDate != null && endDate.isBefore(transaction.getDate())) {
            throw new IllegalArgumentException(
                    "A data final nao pode ser anterior a data da transacao"
            );
        }

        RecurrenceRule rule = recurrenceRuleRepository.findByTransaction(transaction)
                .orElseGet(RecurrenceRule::new);

        rule.setTransaction(transaction);
        rule.setMethod(transaction.getMethod());
        rule.setFrequency(frequency);
        rule.setStartDate(transaction.getDate());
        rule.setEndDate(endDate);
        rule.setLastExecution(transaction.getDate());

        recurrenceRuleRepository.save(rule);
    }

    public void deleteForTransaction(Transaction transaction) {
        if (transaction == null) {
            return;
        }

        recurrenceRuleRepository.findByTransaction(transaction)
                .ifPresent(recurrenceRuleRepository::delete);
    }

    @Transactional(readOnly = true)
    public RecurrenceRule findByTransaction(Transaction transaction) {
        return recurrenceRuleRepository.findByTransaction(transaction).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<RecurringTransactionResponseDTO> findNextByCompany(Company company, int limit) {
        LocalDate today = LocalDate.now();

        return recurrenceRuleRepository.findByTransactionCompany(company)
                .stream()
                .map(rule -> toNextResponse(rule, today))
                .filter(item -> item.nextDate() != null)
                .sorted(Comparator.comparing(RecurringTransactionResponseDTO::nextDate))
                .limit(Math.max(limit, 1))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecurringTransactionResponseDTO> findByCompany(Company company) {
        LocalDate today = LocalDate.now();

        return recurrenceRuleRepository.findByTransactionCompany(company)
                .stream()
                .map(rule -> toNextResponse(rule, today))
                .filter(item -> item.nextDate() != null)
                .sorted(Comparator.comparing(RecurringTransactionResponseDTO::nextDate))
                .toList();
    }

    @Transactional(readOnly = true)
    public RecurrenceRule findById(Long id) {
        return recurrenceRuleRepository.findById(id).orElse(null);
    }

    public RecurringTransactionResponseDTO update(Long id, RecurrenceFrequency frequency, LocalDate endDate) {
        RecurrenceRule rule = recurrenceRuleRepository.findById(id).orElse(null);

        if (rule == null) {
            return null;
        }

        if (endDate != null && endDate.isBefore(rule.getStartDate())) {
            throw new IllegalArgumentException("A data final nao pode ser anterior ao inicio da recorrencia");
        }

        rule.setFrequency(frequency);
        rule.setEndDate(endDate);

        return toNextResponse(recurrenceRuleRepository.save(rule), LocalDate.now());
    }

    public boolean delete(Long id) {
        if (!recurrenceRuleRepository.existsById(id)) {
            return false;
        }

        recurrenceRuleRepository.deleteById(id);
        return true;
    }

    private RecurringTransactionResponseDTO toNextResponse(RecurrenceRule rule, LocalDate today) {
        LocalDate nextDate = nextDate(rule, today);

        if (nextDate == null) {
            return new RecurringTransactionResponseDTO(
                    rule.getId(),
                    rule.getTransaction().getId(),
                    rule.getTransaction().getCompany().getId(),
                    rule.getTransaction().getDescription(),
                    rule.getTransaction().getValue(),
                    rule.getTransaction().getType(),
                    rule.getFrequency(),
                    null,
                    rule.getEndDate()
            );
        }

        Transaction transaction = rule.getTransaction();

        return new RecurringTransactionResponseDTO(
                rule.getId(),
                transaction.getId(),
                transaction.getCompany().getId(),
                transaction.getDescription(),
                transaction.getValue(),
                transaction.getType(),
                rule.getFrequency(),
                nextDate,
                rule.getEndDate()
        );
    }

    private LocalDate nextDate(RecurrenceRule rule, LocalDate today) {
        LocalDate next = rule.getStartDate();

        while (next.isBefore(today)) {
            next = switch (rule.getFrequency()) {
                case DAILY -> next.plusDays(1);
                case WEEKLY -> next.plusWeeks(1);
                case MONTHLY -> next.plusMonths(1);
                case YEARLY -> next.plusYears(1);
            };
        }

        if (rule.getEndDate() != null && next.isAfter(rule.getEndDate())) {
            return null;
        }

        return next;
    }
}
