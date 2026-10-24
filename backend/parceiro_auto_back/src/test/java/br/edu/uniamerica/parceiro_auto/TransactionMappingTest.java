package br.edu.uniamerica.parceiro_auto;

import br.edu.uniamerica.parceiro_auto.entity.*;
import br.edu.uniamerica.parceiro_auto.entity.enums.RecurrenceFrequency;
import br.edu.uniamerica.parceiro_auto.repository.RecurrenceRuleRepository;
import br.edu.uniamerica.parceiro_auto.service.TransactionApplicationService;
import br.edu.uniamerica.parceiro_auto.controller.dto.mapper.TransactionMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionMappingTest {
    @Test
    void loadsRecurrencesOnceAndPreservesTransactionOrder() {
        var repository = mock(RecurrenceRuleRepository.class);
        var service = new TransactionApplicationService(null, null, null, null, null, repository);
        var first = transaction(2L);
        var second = transaction(1L);
        var rule = new RecurrenceRule();
        rule.setTransaction(second);
        rule.setFrequency(RecurrenceFrequency.MONTHLY);
        var transactions = List.of(first, second);
        when(repository.findByTransactionIn(transactions)).thenReturn(List.of(rule));
        var result = service.toResponseDTOs(transactions);
        assertThat(result).extracting(dto -> dto.id()).containsExactly(2L, 1L);
        assertThat(result.get(0).recurrenceFrequency()).isNull();
        assertThat(result.get(1).recurrenceFrequency()).isEqualTo(RecurrenceFrequency.MONTHLY);
        verify(repository).findByTransactionIn(transactions);
        assertThat(service.toResponseDTOs(List.of())).isEmpty();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void mapsTransactionWithoutRecurrenceAndKeepsItsIdentifiers() {
        // Arrange
        Transaction transaction = transaction(20L);
        transaction.getCompany().setId(1L);
        transaction.getBankAccount().setId(2L);
        transaction.getTransactionCategory().setId(3L);
        transaction.setDescription("Venda sem recorrencia");

        // Act
        var result = TransactionMapper.toResponseDTO(transaction);

        // Assert
        assertThat(result.id()).isEqualTo(20L);
        assertThat(result.companyId()).isEqualTo(1L);
        assertThat(result.bankAccountId()).isEqualTo(2L);
        assertThat(result.transactionCategoryId()).isEqualTo(3L);
        assertThat(result.description()).isEqualTo("Venda sem recorrencia");
        assertThat(result.recurrenceFrequency()).isNull();
        assertThat(result.recurrenceEndDate()).isNull();
    }

    private Transaction transaction(Long id) {
        var transaction = new Transaction();
        transaction.setId(id);
        transaction.setCompany(new Company());
        transaction.setBankAccount(new BankAccount());
        transaction.setTransactionCategory(new TransactionCategory());
        return transaction;
    }
}
