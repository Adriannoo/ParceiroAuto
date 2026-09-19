package br.edu.uniamerica.parceiro_auto.repository;

import br.edu.uniamerica.parceiro_auto.entity.Company;
import br.edu.uniamerica.parceiro_auto.entity.RecurrenceRule;
import br.edu.uniamerica.parceiro_auto.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecurrenceRuleRepository extends JpaRepository<RecurrenceRule, Long> {
    Optional<RecurrenceRule> findByTransaction(Transaction transaction);

    // Uma consulta atende todas as movimentacoes da pagina/lista.
    List<RecurrenceRule> findByTransactionIn(List<Transaction> transactions);

    List<RecurrenceRule> findByTransactionCompany(Company company);
}
