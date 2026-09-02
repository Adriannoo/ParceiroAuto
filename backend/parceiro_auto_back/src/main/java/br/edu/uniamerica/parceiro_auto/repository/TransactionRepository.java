package br.edu.uniamerica.parceiro_auto.repository;

import br.edu.uniamerica.parceiro_auto.entity.Transaction;
import br.edu.uniamerica.parceiro_auto.entity.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long>{

    // Spring Data JPA faz as querys sozinhas, so precisa declarar os metodos na

    List<Transaction> findByTipo(TransactionType tipo);

}
