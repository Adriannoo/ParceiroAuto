package br.edu.uniamerica.parceiro_auto.repository;

import br.edu.uniamerica.parceiro_auto.entity.TransactionCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionCategoryRepository extends JpaRepository<TransactionCategory, Long> {
}
