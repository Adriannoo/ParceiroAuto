package br.edu.uniamerica.parceiro_auto;

import br.edu.uniamerica.parceiro_auto.entity.Company;
import br.edu.uniamerica.parceiro_auto.entity.RecurrenceRule;
import br.edu.uniamerica.parceiro_auto.entity.Transaction;
import br.edu.uniamerica.parceiro_auto.entity.enums.RecurrenceFrequency;
import br.edu.uniamerica.parceiro_auto.entity.enums.TransactionMethod;
import br.edu.uniamerica.parceiro_auto.entity.enums.TransactionType;
import br.edu.uniamerica.parceiro_auto.repository.RecurrenceRuleRepository;
import br.edu.uniamerica.parceiro_auto.service.RecurrenceRuleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class RecurrenceRuleServiceTest {
    // Mock cria um repository simulado, nao acessa o banco
    private final RecurrenceRuleRepository repository = mock(RecurrenceRuleRepository.class);

    // Cria o service real usando essa dependencia simulada, para testar o comportamento do service
    private final RecurrenceRuleService service = new RecurrenceRuleService(repository);

    // Indentifica o metodo que o JUnit deve executar
    @Test
    void deletesExistingRecurrenceAndReturnsTrue() {
        // Arrange: prepara o cenario, determina que a consulta de existencia do id retornara true
        Long id = 1L; // Id de exemplo para o teste. L significa que e um Long literal
        when(repository.existsById(id)).thenReturn(true);

        // Act: executa o metodo que queremos testar
        boolean result = service.delete(id);

        // Assert: confere o retorno e verifica se o metodo deleteById foi chamado com o id correto
        assertThat(result).isTrue();
        verify(repository).deleteById(id);
    }

    @Test
    void returnsFalseWithoutDeletingWhenRecurrenceDoesNotExist() {
        // Arrange
        Long id = 1L;
        when(repository.existsById(id)).thenReturn(false);

        // Act: tenta deletar a recorrencia que nao existe
        boolean result = service.delete(id);

        // Assert: verifica se o retorno e false e se o metodo deleteById nao foi chamado
        assertThat(result).isFalse();
        verify(repository, never()).deleteById(anyLong());
    }

    @Test
    void returnsRecurrenceWhenIdExists() {
        // Arrange
        Long id = 1L;
        RecurrenceRule rule = new RecurrenceRule();
        rule.setId(id);

        when(repository.findById(id)).thenReturn(Optional.of(rule));

        // Act
        RecurrenceRule result = service.findById(id);

        // Assert
        assertThat(result).isEqualTo(rule);
        verify(repository).findById(id);
    }

    @Test
    void returnsRecurrenceWhenTransactionHasRule() {
        // Arrange
        Transaction transaction = new Transaction();
        RecurrenceRule rule = new RecurrenceRule();
        rule.setTransaction(transaction);

        when(repository.findByTransaction(transaction))
                .thenReturn(Optional.of(rule));

        // Act
        RecurrenceRule result = service.findByTransaction(transaction);

        // Assert
        assertThat(result).isSameAs(rule);
        verify(repository).findByTransaction(transaction);
    }

    @Test
    void returnsNullWhenTransactionHasNoRule() {
        // Arrange
        Transaction transaction = new Transaction();

        when(repository.findByTransaction(transaction))
                .thenReturn(Optional.empty());

        // Act
        RecurrenceRule result = service.findByTransaction(transaction);

        // Assert
        assertThat(result).isNull();
        verify(repository).findByTransaction(transaction);
    }

    @Test
    void deletesRuleAssociatedWithTransaction() {
        // Arrange
        Transaction transaction = new Transaction();
        RecurrenceRule rule = new RecurrenceRule();
        rule.setId(1L);
        rule.setTransaction(transaction);

        when(repository.findByTransaction(transaction))
                .thenReturn(Optional.of(rule));

        // Act
        service.deleteForTransaction(transaction);

        // Assert
        verify(repository).findByTransaction(transaction);
        verify(repository).delete(rule);
    }

    @Test
    void doesNotDeleteWhenTransactionHasNoRule() {
        // Arrange
        Transaction transaction = new Transaction();

        when(repository.findByTransaction(transaction))
                .thenReturn(Optional.empty());

        // Act
        service.deleteForTransaction(transaction);

        // Assert
        verify(repository).findByTransaction(transaction);
        verify(repository, never()).delete(any(RecurrenceRule.class));
    }

    @Test
    void ignoresNullTransactionWhenDeletingRule() {
        // Arrange
        Transaction transaction = null;

        // Act
        service.deleteForTransaction(transaction);

        // Assert
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsNullTransactionBeforeSavingRule() {
        // Arrange
        Transaction transaction = null;

        // Act & Assert
        assertThatThrownBy(() ->
                service.saveForTransaction(transaction, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A movimentacao nao pode ser nula");

        verifyNoInteractions(repository);
    }
    // Confirma o retorno definido pelo service quando o ID nao existe.
    @Test
    void returnsNullWhenRecurrenceIdDoesNotExist() {
        // Arrange: simula uma busca sem resultado.
        when(repository.findById(1L)).thenReturn(Optional.empty());

        // Act
        RecurrenceRule result = service.findById(1L);

        // Assert
        assertThat(result).isNull();
        verify(repository).findById(1L);
    }

    @Test
    void savesNewRecurrenceWithTransactionDataAndNoEndDate() {
        // Arrange: a movimentacao ainda nao possui regra.
        Transaction transaction = transaction(LocalDate.of(2026, 10, 1));
        when(repository.findByTransaction(transaction)).thenReturn(Optional.empty());

        // Act
        service.saveForTransaction(transaction, RecurrenceFrequency.MONTHLY, null);

        // Assert: captura a regra enviada para salvar e confere seus campos.
        ArgumentCaptor<RecurrenceRule> captor = ArgumentCaptor.forClass(RecurrenceRule.class);
        verify(repository).save(captor.capture());
        RecurrenceRule saved = captor.getValue();
        assertThat(saved.getTransaction()).isSameAs(transaction);
        assertThat(saved.getMethod()).isEqualTo(TransactionMethod.PIX);
        assertThat(saved.getFrequency()).isEqualTo(RecurrenceFrequency.MONTHLY);
        assertThat(saved.getStartDate()).isEqualTo(transaction.getDate());
        assertThat(saved.getLastExecution()).isEqualTo(transaction.getDate());
        assertThat(saved.getEndDate()).isNull();
    }

    @Test
    void reusesExistingRuleWhenSavingTransactionRecurrence() {
        // Arrange: usa uma data final igual ao inicio, que deve ser aceita.
        LocalDate date = LocalDate.of(2026, 10, 1);
        Transaction transaction = transaction(date);
        RecurrenceRule existing = rule(1L, date, RecurrenceFrequency.DAILY);
        when(repository.findByTransaction(transaction)).thenReturn(Optional.of(existing));

        // Act
        service.saveForTransaction(transaction, RecurrenceFrequency.WEEKLY, date);

        // Assert: atualiza a mesma regra, preservando seu ID.
        verify(repository).save(same(existing));
        assertThat(existing.getId()).isEqualTo(1L);
        assertThat(existing.getTransaction()).isSameAs(transaction);
        assertThat(existing.getFrequency()).isEqualTo(RecurrenceFrequency.WEEKLY);
        assertThat(existing.getEndDate()).isEqualTo(date);
    }

    @Test
    void removesExistingRuleWhenFrequencyAndEndDateAreRemoved() {
        // Arrange
        Transaction transaction = transaction(LocalDate.of(2026, 10, 1));
        RecurrenceRule existing = rule(1L, transaction.getDate(), RecurrenceFrequency.MONTHLY);
        when(repository.findByTransaction(transaction)).thenReturn(Optional.of(existing));

        // Act: sem frequencia e sem data final, a recorrencia deve ser removida.
        service.saveForTransaction(transaction, null, null);

        // Assert
        verify(repository).delete(existing);
        verify(repository, never()).save(any(RecurrenceRule.class));
    }

    @Test
    void rejectsEndDateWithoutRecurrenceFrequency() {
        // Arrange
        Transaction transaction = transaction(LocalDate.of(2026, 10, 1));
        LocalDate endDate = transaction.getDate().plusDays(10);

        // Act & Assert: dados invalidos devem ser rejeitados antes de acessar o banco.
        assertThatThrownBy(() -> service.saveForTransaction(transaction, null, endDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A data final exige uma frequencia de recorrencia");
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsEndDateBeforeTransactionDate() {
        // Arrange
        Transaction transaction = transaction(LocalDate.of(2026, 10, 1));
        LocalDate endDate = transaction.getDate().minusDays(1);

        // Act & Assert
        assertThatThrownBy(() -> service.saveForTransaction(transaction, RecurrenceFrequency.DAILY, endDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A data final nao pode ser anterior a data da transacao");
        verifyNoInteractions(repository);
    }

    @Test
    void returnsNullWithoutSavingWhenUpdatingMissingRule() {
        // Arrange
        when(repository.findById(1L)).thenReturn(Optional.empty());

        // Act
        var result = service.update(1L, RecurrenceFrequency.MONTHLY, null);

        // Assert
        assertThat(result).isNull();
        verify(repository, never()).save(any(RecurrenceRule.class));
    }

    @Test
    void updatesFrequencyAndAcceptsEndDateEqualToStartDate() {
        // Arrange: inicio no futuro evita que a regra ja esteja encerrada.
        LocalDate start = LocalDate.now().plusDays(10);
        RecurrenceRule existing = rule(1L, start, RecurrenceFrequency.DAILY);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        // Act
        var result = service.update(1L, RecurrenceFrequency.WEEKLY, start);

        // Assert: confere o estado da entidade e os dados devolvidos no DTO.
        assertThat(existing.getFrequency()).isEqualTo(RecurrenceFrequency.WEEKLY);
        assertThat(existing.getEndDate()).isEqualTo(start);
        assertThat(result.recurrenceRuleId()).isEqualTo(1L);
        assertThat(result.frequency()).isEqualTo(RecurrenceFrequency.WEEKLY);
        assertThat(result.nextDate()).isEqualTo(start);
        assertThat(result.endDate()).isEqualTo(start);
        verify(repository).save(existing);
    }

    @Test
    void removesEndDateWhenUpdatingRecurrence() {
        // Arrange
        LocalDate start = LocalDate.now().plusDays(10);
        RecurrenceRule existing = rule(1L, start, RecurrenceFrequency.DAILY);
        existing.setEndDate(start.plusDays(30));
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        // Act
        var result = service.update(1L, RecurrenceFrequency.MONTHLY, null);

        // Assert
        assertThat(existing.getEndDate()).isNull();
        assertThat(result.endDate()).isNull();
        assertThat(result.frequency()).isEqualTo(RecurrenceFrequency.MONTHLY);
        verify(repository).save(existing);
    }

    @Test
    void rejectsUpdateWithEndDateBeforeStartWithoutChangingRule() {
        // Arrange
        LocalDate start = LocalDate.of(2026, 10, 1);
        RecurrenceRule existing = rule(1L, start, RecurrenceFrequency.MONTHLY);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        // Act & Assert
        assertThatThrownBy(() -> service.update(1L, RecurrenceFrequency.DAILY, start.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A data final nao pode ser anterior ao inicio da recorrencia");
        assertThat(existing.getFrequency()).isEqualTo(RecurrenceFrequency.MONTHLY);
        assertThat(existing.getEndDate()).isNull();
        verify(repository, never()).save(any(RecurrenceRule.class));
    }

    @Test
    void returnsEmptyListWhenCompanyHasNoRecurrences() {
        // Arrange
        Company company = new Company();
        company.setId(7L);
        when(repository.findByTransactionCompany(company)).thenReturn(List.of());

        // Act
        var result = service.findByCompany(company);

        // Assert
        assertThat(result).isEmpty();
        verify(repository).findByTransactionCompany(company);
    }

    @Test
    void sortsRecurrencesFiltersExpiredRulesAndMapsTransactionData() {
        // Arrange: o repository devolve regras fora de ordem e uma regra encerrada.
        LocalDate today = LocalDate.now();
        RecurrenceRule later = rule(1L, today.plusDays(10), RecurrenceFrequency.MONTHLY);
        RecurrenceRule sooner = rule(2L, today.plusDays(2), RecurrenceFrequency.WEEKLY);
        RecurrenceRule expired = rule(3L, today.minusDays(2), RecurrenceFrequency.DAILY);
        expired.setEndDate(today.minusDays(1));
        Company company = sooner.getTransaction().getCompany();
        when(repository.findByTransactionCompany(company)).thenReturn(List.of(later, expired, sooner));

        // Act
        var result = service.findByCompany(company);

        // Assert: remove a encerrada e ordena pela proxima data.
        assertThat(result).hasSize(2);
        assertThat(result.get(0).recurrenceRuleId()).isEqualTo(2L);
        assertThat(result.get(1).recurrenceRuleId()).isEqualTo(1L);
        assertThat(result.get(0).nextDate()).isEqualTo(today.plusDays(2));
        // Confere se os dados da movimentacao chegaram corretamente ao DTO.
        assertThat(result.get(0).transactionId()).isEqualTo(20L);
        assertThat(result.get(0).companyId()).isEqualTo(7L);
        assertThat(result.get(0).description()).isEqualTo("Pagamento mensal");
        assertThat(result.get(0).value()).isEqualByComparingTo("150.00");
        assertThat(result.get(0).type()).isEqualTo(TransactionType.SAIDA);
        assertThat(result.get(0).frequency()).isEqualTo(RecurrenceFrequency.WEEKLY);
        assertThat(result.get(0).endDate()).isNull();
    }

    @Test
    void calculatesDailyRecurrenceAndIncludesItsLastAllowedDate() {
        // Arrange: precisa avancar dois dias para chegar a hoje.
        LocalDate today = LocalDate.now();
        RecurrenceRule rule = rule(1L, today.minusDays(2), RecurrenceFrequency.DAILY);
        rule.setEndDate(today);
        Company company = rule.getTransaction().getCompany();
        when(repository.findByTransactionCompany(company)).thenReturn(List.of(rule));

        // Act
        var result = service.findByCompany(company);

        // Assert: data igual ao fim ainda pode aparecer na listagem.
        assertThat(result).hasSize(1);
        assertThat(result.get(0).nextDate()).isEqualTo(today);
        assertThat(result.get(0).endDate()).isEqualTo(today);
    }

    @Test
    void calculatesWeeklyRecurrenceUntilNextAvailableDate() {
        // Arrange: uma regra iniciada ha oito dias vence novamente em seis dias.
        LocalDate today = LocalDate.now();
        RecurrenceRule rule = rule(1L, today.minusDays(8), RecurrenceFrequency.WEEKLY);
        Company company = rule.getTransaction().getCompany();
        when(repository.findByTransactionCompany(company)).thenReturn(List.of(rule));

        // Act
        var result = service.findByCompany(company);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).nextDate()).isEqualTo(today.plusDays(6));
    }

    @Test
    void calculatesMonthlyRecurrenceFromStartDate() {
        // Arrange: a primeira repeticao mensal de ontem ainda esta no futuro.
        LocalDate start = LocalDate.now().minusDays(1);
        RecurrenceRule rule = rule(1L, start, RecurrenceFrequency.MONTHLY);
        Company company = rule.getTransaction().getCompany();
        when(repository.findByTransactionCompany(company)).thenReturn(List.of(rule));

        // Act
        var result = service.findByCompany(company);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).nextDate()).isEqualTo(start.plusMonths(1));
    }

    @Test
    void calculatesYearlyRecurrenceFromStartDate() {
        // Arrange
        LocalDate start = LocalDate.now().minusDays(1);
        RecurrenceRule rule = rule(1L, start, RecurrenceFrequency.YEARLY);
        Company company = rule.getTransaction().getCompany();
        when(repository.findByTransactionCompany(company)).thenReturn(List.of(rule));

        // Act
        var result = service.findByCompany(company);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).nextDate()).isEqualTo(start.plusYears(1));
    }

    @Test
    void keepsStartDateWhenRecurrenceStartsToday() {
        // Arrange: uma regra que comeca hoje nao precisa avancar a data.
        LocalDate today = LocalDate.now();
        RecurrenceRule rule = rule(1L, today, RecurrenceFrequency.MONTHLY);
        Company company = rule.getTransaction().getCompany();
        when(repository.findByTransactionCompany(company)).thenReturn(List.of(rule));

        // Act
        var result = service.findByCompany(company);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).nextDate()).isEqualTo(today);
    }

    @Test
    void limitsNextRecurrencesAfterSortingByDate() {
        // Arrange: pede apenas as duas primeiras entre tres regras.
        LocalDate start = LocalDate.now().plusDays(10);
        RecurrenceRule first = rule(1L, start, RecurrenceFrequency.MONTHLY);
        RecurrenceRule second = rule(2L, start.plusDays(1), RecurrenceFrequency.MONTHLY);
        RecurrenceRule third = rule(3L, start.plusDays(2), RecurrenceFrequency.MONTHLY);
        Company company = first.getTransaction().getCompany();
        when(repository.findByTransactionCompany(company)).thenReturn(List.of(third, first, second));

        // Act
        var result = service.findNextByCompany(company, 2);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).recurrenceRuleId()).isEqualTo(1L);
        assertThat(result.get(1).recurrenceRuleId()).isEqualTo(2L);
    }

    // Executa o mesmo cenario para zero e negativo, sem duplicar o teste.
    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void usesMinimumLimitOfOneForNonPositiveLimits(int limit) {
        // Arrange
        LocalDate start = LocalDate.now().plusDays(10);
        RecurrenceRule first = rule(1L, start, RecurrenceFrequency.DAILY);
        RecurrenceRule second = rule(2L, start.plusDays(1), RecurrenceFrequency.DAILY);
        Company company = first.getTransaction().getCompany();
        when(repository.findByTransactionCompany(company)).thenReturn(List.of(second, first));

        // Act
        var result = service.findNextByCompany(company, limit);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).recurrenceRuleId()).isEqualTo(1L);
    }

    // Monta apenas os dados de movimentacao usados pelos testes de recorrencia.
    private static Transaction transaction(LocalDate date) {
        Company company = new Company();
        company.setId(7L);
        Transaction transaction = new Transaction();
        transaction.setId(20L);
        transaction.setCompany(company);
        transaction.setDate(date);
        transaction.setDescription("Pagamento mensal");
        transaction.setValue(new BigDecimal("150.00"));
        transaction.setType(TransactionType.SAIDA);
        transaction.setMethod(TransactionMethod.PIX);
        return transaction;
    }

    // Centraliza os dados comuns para deixar cada cenario menor e mais facil de ler.
    private static RecurrenceRule rule(Long id, LocalDate start, RecurrenceFrequency frequency) {
        RecurrenceRule rule = new RecurrenceRule();
        rule.setId(id);
        rule.setTransaction(transaction(start));
        rule.setMethod(TransactionMethod.PIX);
        rule.setStartDate(start);
        rule.setLastExecution(start);
        rule.setFrequency(frequency);
        return rule;
    }
}
