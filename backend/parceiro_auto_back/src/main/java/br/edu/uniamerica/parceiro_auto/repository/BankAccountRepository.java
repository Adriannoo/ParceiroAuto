package br.edu.uniamerica.parceiro_auto.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.uniamerica.parceiro_auto.entity.BankAccount;
import br.edu.uniamerica.parceiro_auto.entity.Company;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    List<BankAccount> findByCompany(Company company);

    BankAccount findByCompanyAndDefaultAccountTrue(Company company);

    BankAccount findByCompanyAndBankNameAndBranchAndAccountNumber(
            Company company,
            String bankName,
            String branch,
            String accountNumber
    );
}