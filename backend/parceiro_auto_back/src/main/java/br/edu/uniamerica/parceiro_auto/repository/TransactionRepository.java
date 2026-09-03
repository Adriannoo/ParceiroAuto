package br.edu.uniamerica.parceiro_auto.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.uniamerica.parceiro_auto.entity.BankAccount;
import br.edu.uniamerica.parceiro_auto.entity.Company;
import br.edu.uniamerica.parceiro_auto.entity.Transaction;
import br.edu.uniamerica.parceiro_auto.entity.TransactionCategory;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByBankAccount(BankAccount bankAccount);

    List<Transaction> findByCompany(Company company);

    List<Transaction> findByCompanyAndTransactionCategory(Company company, TransactionCategory category);

    List<Transaction> findByCompanyAndDateBetween(Company company, LocalDate startDate, LocalDate endDate);
}