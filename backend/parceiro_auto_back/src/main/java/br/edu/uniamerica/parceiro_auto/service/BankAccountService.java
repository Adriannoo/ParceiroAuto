package br.edu.uniamerica.parceiro_auto.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.uniamerica.parceiro_auto.entity.BankAccount;
import br.edu.uniamerica.parceiro_auto.entity.Company;
import br.edu.uniamerica.parceiro_auto.repository.BankAccountRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;

    // Cria uma nova conta bancária para a empresa.
    public BankAccount createBankAccount(
            Company company,
            String bankName,
            String branch,
            String accountNumber,
            String accountType,
            boolean defaultAccount
    ) {

        validateCompany(company);

        String normalizedBankName = validateBankName(bankName);
        String normalizedBranch = validateBranch(branch);
        String normalizedAccountNumber = validateAccountNumber(accountNumber);
        String normalizedAccountType = validateAccountType(accountType);

        // Verifica se já existe uma conta com os mesmos dados para a empresa.
        BankAccount existingAccount =
                bankAccountRepository.findByCompanyAndBankNameAndBranchAndAccountNumber(
                        company,
                        normalizedBankName,
                        normalizedBranch,
                        normalizedAccountNumber
                );

        if (existingAccount != null) {
            throw new IllegalArgumentException(
                    "Já existe uma conta com esse banco, agência e número para essa empresa"
            );
        }

        BankAccount bankAccount = new BankAccount();

        bankAccount.setBankName(normalizedBankName);
        bankAccount.setBranch(normalizedBranch);
        bankAccount.setAccountNumber(normalizedAccountNumber);
        bankAccount.setAccountType(normalizedAccountType);
        bankAccount.setDefaultAccount(defaultAccount);
        bankAccount.setCompany(company);

        // Se a nova conta for definida como padrão, remove a definição das outras contas.
        if (defaultAccount) {
            clearDefaultByCompany(company);
        }

        return bankAccountRepository.save(bankAccount);
    }

    // Busca todas as contas bancárias de uma empresa.
    @Transactional(readOnly = true)
    public List<BankAccount> findByCompany(Company company) {

        validateCompany(company);

        return bankAccountRepository.findByCompany(company);
    }

    // Busca a conta bancária padrão da empresa.
    @Transactional(readOnly = true)
    public BankAccount findDefaultByCompany(Company company) {

        validateCompany(company);

        return bankAccountRepository.findByCompanyAndDefaultAccountTrue(company);
    }

    // Define uma conta bancária como padrão para a empresa, removendo a definição das outras contas.
    public void defineDefaultAccount(
            Company company,
            BankAccount bankAccount
    ) {

        validateCompany(company);
        validateBankAccount(bankAccount);
        validateAccountBelongsToCompany(company, bankAccount);

        clearDefaultByCompany(company);

        bankAccount.setDefaultAccount(true);

        bankAccountRepository.save(bankAccount);
    }

    // Exclui uma conta bancária da empresa.
    public void deleteBankAccount(
            Company company,
            BankAccount bankAccount
    ) {

        validateCompany(company);
        validateBankAccount(bankAccount);
        validateAccountBelongsToCompany(company, bankAccount);

        bankAccountRepository.delete(bankAccount);
    }

    // Atualiza os dados de uma conta bancária da empresa.
    public BankAccount updateBankAccount(
            Company company,
            BankAccount bankAccount,
            String bankName,
            String branch,
            String accountNumber,
            String accountType,
            boolean defaultAccount
    ) {

        validateCompany(company);
        validateBankAccount(bankAccount);
        validateAccountBelongsToCompany(company, bankAccount);

        String normalizedBankName = validateBankName(bankName);
        String normalizedBranch = validateBranch(branch);
        String normalizedAccountNumber = validateAccountNumber(accountNumber);
        String normalizedAccountType = validateAccountType(accountType);

        // Verifica se já existe uma conta com os mesmos dados para a empresa, ignorando a própria conta que está sendo atualizada.
        BankAccount existingAccount =
                bankAccountRepository.findByCompanyAndBankNameAndBranchAndAccountNumber(
                        company,
                        normalizedBankName,
                        normalizedBranch,
                        normalizedAccountNumber
                );

        // Se já existe uma conta com os mesmos dados e não é a mesma conta que está sendo atualizada, lança uma exceção.
        if (existingAccount != null
                && !existingAccount.getId().equals(bankAccount.getId())) {

            throw new IllegalArgumentException(
                    "Já existe uma conta com esse banco, agência e número para essa empresa"
            );
        }

        // Se a conta está sendo marcada como padrão, remove a definição das outras contas.
        if (defaultAccount) {
            clearDefaultByCompany(company);
        }

        bankAccount.setBankName(normalizedBankName);
        bankAccount.setBranch(normalizedBranch);
        bankAccount.setAccountNumber(normalizedAccountNumber);
        bankAccount.setAccountType(normalizedAccountType);
        bankAccount.setDefaultAccount(defaultAccount);

        return bankAccountRepository.save(bankAccount);
    }

    // Remove a definição de conta padrão das outras contas da empresa.
    private void clearDefaultByCompany(Company company) {

        List<BankAccount> accounts =
                bankAccountRepository.findByCompany(company);

        for (BankAccount account : accounts) {
            if (account.isDefaultAccount()) {
                account.setDefaultAccount(false);
            }
        }
    }

    // Verifica se a empresa foi informada.
    private void validateCompany(Company company) {

        if (company == null) {
            throw new IllegalArgumentException(
                    "A empresa não pode ser nula"
            );
        }
    }

    // Verifica se a conta bancária foi informada.
    private void validateBankAccount(BankAccount bankAccount) {

        if (bankAccount == null) {
            throw new IllegalArgumentException(
                    "A conta bancária não pode ser nula"
            );
        }
    }

    // Verifica se a conta bancária pertence à empresa informada.
    private void validateAccountBelongsToCompany(
            Company company,
            BankAccount bankAccount
    ) {

        if (bankAccount.getCompany() == null
                || bankAccount.getCompany().getId() == null
                || company.getId() == null
                || !bankAccount.getCompany()
                        .getId()
                        .equals(company.getId())) {

            throw new IllegalArgumentException(
                    "A conta bancária não pertence a essa empresa"
            );
        }
    }

    // Valida e normaliza o nome do banco.
    private String validateBankName(String bankName) {

        if (bankName == null || bankName.isBlank()) {
            throw new IllegalArgumentException(
                    "O banco não pode estar vazio"
            );
        }

        return bankName.trim();
    }

    // Valida e normaliza o tipo da conta.
    private String validateAccountType(String accountType) {

        if (accountType == null || accountType.isBlank()) {
            throw new IllegalArgumentException(
                    "O tipo da conta não pode estar vazio"
            );
        }

        return accountType.trim();
    }

    // Valida a agência, que deve possuir exatamente 4 dígitos.
    private String validateBranch(String branch) {

        if (branch == null || branch.isBlank()) {
            throw new IllegalArgumentException(
                    "A agência não pode estar vazia"
            );
        }

        String normalizedBranch = branch.trim();

        if (!normalizedBranch.matches("\\d{4}")) {
            throw new IllegalArgumentException(
                    "A agência deve ter exatamente 4 dígitos"
            );
        }

        return normalizedBranch;
    }

    // Valida o número da conta, que deve possuir de 4 a 13 dígitos.
    private String validateAccountNumber(String accountNumber) {

        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException(
                    "O número da conta não pode estar vazio"
            );
        }

        String normalizedAccountNumber = accountNumber.trim();

        if (!normalizedAccountNumber.matches("\\d{4,13}")) {
            throw new IllegalArgumentException(
                    "O número da conta deve ter entre 4 e 13 dígitos"
            );
        }

        return normalizedAccountNumber;
    }
}