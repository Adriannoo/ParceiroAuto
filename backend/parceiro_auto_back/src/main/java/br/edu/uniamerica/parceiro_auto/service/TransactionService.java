package br.edu.uniamerica.parceiro_auto.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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

        validateRelationsBelongToCompany(company, bankAccount, transactionCategory);

        log.info(
                "Criando transacao empresa id:({}) conta id:({}) tipo:({}) valor:({})",
                company.getId(), bankAccount.getId(), type, value
        );

        // Verifica se a categoria corresponde ao tipo da transação.
        if (transactionCategory.getType() != type) {
            log.warn("Categoria id:({}) nao corresponde ao tipo:({}) da transacao", transactionCategory.getId(), type);
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

        Transaction saved = transactionRepository.save(transaction);
        log.info("Transacao id:({}) criada com sucesso", saved.getId());
        return saved;
    }

    // Procura a transacao pelo ID
    @Transactional(readOnly = true)
    public Transaction findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("O ID nao pode ser nulo!");
        }

        return transactionRepository.findById(id)
                .orElseThrow(
                        () -> new IllegalArgumentException("Transacao nao encontrada!")
                );
    }

    @Transactional(readOnly = true)
    public List<Transaction> findAll() {
        return transactionRepository.findAllByOrderByDateDescIdDesc();
    }

    // Busca todas as transações de uma conta bancária.
    @Transactional(readOnly = true)
    public List<Transaction> findByBankAccount(BankAccount bankAccount) {

        if (bankAccount == null) {
            throw new IllegalArgumentException(
                    "A conta bancária não pode ser nula"
            );
        }

        return transactionRepository.findByBankAccountOrderByDateDescIdDesc(bankAccount);
    }

    // Busca todas as transações de uma empresa.
    @Transactional(readOnly = true)
    public List<Transaction> findByCompany(Company company) {

        if (company == null) {
            throw new IllegalArgumentException(
                    "A empresa não pode ser nula"
            );
        }

        return transactionRepository.findByCompanyOrderByDateDescIdDesc(company);
    }

    // Busca as ultimas movimentacoes por data e ID, com limite aplicado no banco.
    @Transactional(readOnly = true)
    public List<Transaction> findLastByCompany(
            Company company,
            int limit
    ) {

        // Impede limites invalidos e consultas muito grandes nesse endpoint.
        if (company == null || company.getId() == null) {
            throw new IllegalArgumentException("A empresa deve estar cadastrada");
        }
        if (limit < 1 || limit > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O limite deve estar entre 1 e 100");
        }

        // A ordenacao por data e ID ja esta definida no repository.
        return transactionRepository.findByCompanyOrderByDateDescIdDesc(company, PageRequest.of(0, limit));
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

        validateRelationsBelongToCompany(transaction.getCompany(), newBankAccount, newCategory);

        log.info("Atualizando transacao id:({})", transaction.getId());

        // Verifica se a nova categoria corresponde ao novo tipo.
        if (newCategory.getType() != newType) {
            log.warn("Categoria id:({}) nao corresponde ao tipo:({}) da transacao", newCategory.getId(), newType);
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

        Transaction saved = transactionRepository.save(transaction);
        log.info("Transacao id:({}) atualizada com sucesso", saved.getId());
        return saved;
    }

    // Deleta uma transação e reverte o efeito dela no saldo da conta.
    public void deleteTransaction(Transaction transaction) {

        if (transaction == null) {
            throw new IllegalArgumentException(
                    "A transação não pode ser nula"
            );
        }

        log.info("Deletando transacao id:({})", transaction.getId());

        reverseBalanceEffect(transaction);

        transactionRepository.delete(transaction);
        log.info("Transacao id:({}) deletada com sucesso", transaction.getId());
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

    // Valida os vinculos antes de alterar qualquer saldo da movimentacao.
    private void validateRelationsBelongToCompany(
            Company company,
            BankAccount bankAccount,
            TransactionCategory transactionCategory
    ) {
        if (company.getId() == null) {
            throw new IllegalArgumentException("A empresa deve estar cadastrada");
        }

        if (bankAccount.getCompany() == null
                || !company.getId().equals(bankAccount.getCompany().getId())) {
            throw new IllegalArgumentException(
                    "A conta bancária não pertence à empresa informada"
            );
        }

        if (transactionCategory.getCompany() == null
                || !company.getId().equals(transactionCategory.getCompany().getId())) {
            throw new IllegalArgumentException(
                    "A categoria não pertence à empresa informada"
            );
        }

        if (!transactionCategory.isActive()) {
            throw new IllegalArgumentException(
                    "A categoria selecionada está inativa"
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
