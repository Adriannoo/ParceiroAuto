package br.edu.uniamerica.parceiro_auto.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.uniamerica.parceiro_auto.entity.BankAccount;
import br.edu.uniamerica.parceiro_auto.entity.Company;
import br.edu.uniamerica.parceiro_auto.entity.Transaction;
import br.edu.uniamerica.parceiro_auto.entity.TransactionCategory;
import br.edu.uniamerica.parceiro_auto.entity.enums.TransactionMethod;
import br.edu.uniamerica.parceiro_auto.entity.enums.TransactionType;
import br.edu.uniamerica.parceiro_auto.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;

   // Cria uma transação utilizando a data atual.
    public Transaction createTransaction(
            Company company,
            BankAccount bankAccount,
            TransactionCategory transactionCategory,
            TransactionType type,
            String description,
            BigDecimal value,
            TransactionMethod method
    ) {

        return createTransaction(
                company,
                bankAccount,
                transactionCategory,
                type,
                description,
                value,
                method,
                LocalDate.now()
        );
    }

    // Cria uma transação utilizando a data informada.
    public Transaction createTransaction(
            Company company,
            BankAccount bankAccount,
            TransactionCategory transactionCategory,
            TransactionType type,
            String description,
            BigDecimal value,
            TransactionMethod method,
            LocalDate date
    ) {

        validateRequiredFields(
                company,
                bankAccount,
                transactionCategory,
                type,
                description,
                value,
                method,
                date
        );

        // Verifica se a categoria corresponde ao tipo da transação.
        if (transactionCategory.getType() != type) {
            throw new IllegalArgumentException(
                    "A categoria selecionada não corresponde ao tipo da transação"
            );
        }

        // Atualiza o saldo da conta de acordo com a transação.
        applyBalanceEffect(bankAccount, type, value);

        Transaction transaction = new Transaction();

        transaction.setCompany(company);
        transaction.setBankAccount(bankAccount);
        transaction.setTransactionCategory(transactionCategory);
        transaction.setType(type);
        transaction.setDescription(description.trim());
        transaction.setValue(value);
        transaction.setMethod(method);
        transaction.setDate(date);

        return transactionRepository.save(transaction);
    }

    // Busca todas as transações de uma conta bancária.
    @Transactional(readOnly = true)
    public List<Transaction> findByBankAccount(BankAccount bankAccount) {

        if (bankAccount == null) {
            throw new IllegalArgumentException(
                    "A conta bancária não pode ser nula"
            );
        }

        return transactionRepository.findByBankAccount(bankAccount);
    }

    // Busca todas as transações de uma empresa.
    @Transactional(readOnly = true)
    public List<Transaction> findByCompany(Company company) {

        if (company == null) {
            throw new IllegalArgumentException(
                    "A empresa não pode ser nula"
            );
        }

        return transactionRepository.findByCompany(company);
    }

    // Busca todas as transações de uma empresa com a quantidade definida pelo desenvolvedor.
    @Transactional(readOnly = true)
    public List<Transaction> findLastByCompany(
            Company company,
            int limit
    ) {

        return findByCompany(company)
                .stream()
                .limit(limit)
                .toList();
    }

    // Atualiza uma transação, mantendo a data original dela.
    public Transaction updateTransaction(
            Transaction transaction,
            BankAccount newBankAccount,
            TransactionCategory newCategory,
            TransactionType newType,
            String newDescription,
            BigDecimal newValue,
            TransactionMethod newMethod
    ) {

        return updateTransaction(
                transaction,
                newBankAccount,
                newCategory,
                newType,
                newDescription,
                newValue,
                newMethod,
                transaction == null ? null : transaction.getDate()
        );
    }

    // Atualiza uma transação, permitindo alterar a data dela.
    public Transaction updateTransaction(
            Transaction transaction,
            BankAccount newBankAccount,
            TransactionCategory newCategory,
            TransactionType newType,
            String newDescription,
            BigDecimal newValue,
            TransactionMethod newMethod,
            LocalDate newDate
    ) {

        if (transaction == null) {
            throw new IllegalArgumentException(
                    "A transação não pode ser nula"
            );
        }

        validateRequiredFields(
                transaction.getCompany(),
                newBankAccount,
                newCategory,
                newType,
                newDescription,
                newValue,
                newMethod,
                newDate
        );

        // Verifica se a nova categoria corresponde ao novo tipo.
        if (newCategory.getType() != newType) {
            throw new IllegalArgumentException(
                    "A categoria selecionada não corresponde ao tipo da transação"
            );
        }

        reverseBalanceEffect(transaction);

        applyBalanceEffect(
                newBankAccount,
                newType,
                newValue
        );

        transaction.setBankAccount(newBankAccount);
        transaction.setTransactionCategory(newCategory);
        transaction.setType(newType);
        transaction.setDescription(newDescription.trim());
        transaction.setValue(newValue);
        transaction.setMethod(newMethod);
        transaction.setDate(newDate);

        return transactionRepository.save(transaction);
    }

    // Deleta uma transação e reverte o efeito dela no saldo da conta.
    public void deleteTransaction(Transaction transaction) {

        if (transaction == null) {
            throw new IllegalArgumentException(
                    "A transação não pode ser nula"
            );
        }

        reverseBalanceEffect(transaction);

        transactionRepository.delete(transaction);
    }

    //Valida se os campos obrigatórios foram preenchidos corretamente.
    private void validateRequiredFields(
            Company company,
            BankAccount bankAccount,
            TransactionCategory transactionCategory,
            TransactionType type,
            String description,
            BigDecimal value,
            TransactionMethod method,
            LocalDate date
    ) {

        if (company == null) {
            throw new IllegalArgumentException(
                    "A empresa não pode ser nula"
            );
        }

        if (bankAccount == null) {
            throw new IllegalArgumentException(
                    "A conta bancária não pode ser nula"
            );
        }

        if (transactionCategory == null) {
            throw new IllegalArgumentException(
                    "A categoria não pode ser nula"
            );
        }

        if (type == null) {
            throw new IllegalArgumentException(
                    "O tipo da transação não pode ser nulo"
            );
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(
                    "A descrição não pode estar vazia"
            );
        }

        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "O valor deve ser maior que zero"
            );
        }

        if (method == null) {
            throw new IllegalArgumentException(
                    "O método da transação não pode ser nulo"
            );
        }

        if (date == null) {
            throw new IllegalArgumentException(
                    "A data não pode ser nula"
            );
        }
    }

    // Ao editar ou excluir uma transação, precisamos desfazer o efeito dela no saldo da conta bancária.
    private void reverseBalanceEffect(Transaction transaction) {

        BankAccount bankAccount = transaction.getBankAccount();

        BigDecimal currentBalance = bankAccount.getBalance() == null
                ? BigDecimal.ZERO
                : bankAccount.getBalance();

        if (transaction.getType() == TransactionType.SAIDA) {

            // Desfaz uma saída: devolve o valor ao saldo.
            bankAccount.setBalance(
                    currentBalance.add(transaction.getValue())
            );

        } else {

            // Desfaz uma entrada: remove o valor do saldo.
            bankAccount.setBalance(
                    currentBalance.subtract(transaction.getValue())
            );
        }
    }

    private void applyBalanceEffect(
            BankAccount bankAccount,
            TransactionType type,
            BigDecimal value
    ) {

        BigDecimal currentBalance = bankAccount.getBalance() == null
                ? BigDecimal.ZERO
                : bankAccount.getBalance();

        if (type == TransactionType.SAIDA) {

            // Saída de dinheiro: diminui o saldo.
            bankAccount.setBalance(
                    currentBalance.subtract(value)
            );

        } else {

            // Entrada de dinheiro: aumenta o saldo.
            bankAccount.setBalance(
                    currentBalance.add(value)
            );
        }
    }
}