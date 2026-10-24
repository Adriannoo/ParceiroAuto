package br.edu.uniamerica.parceiro_auto;

import br.edu.uniamerica.parceiro_auto.entity.BankAccount;
import br.edu.uniamerica.parceiro_auto.entity.Company;
import br.edu.uniamerica.parceiro_auto.exception.BusinessRuleException;
import br.edu.uniamerica.parceiro_auto.exception.ResourceNotFoundException;
import br.edu.uniamerica.parceiro_auto.repository.BankAccountRepository;
import br.edu.uniamerica.parceiro_auto.service.BankAccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

// O service e real; apenas o acesso ao banco e simulado.
class BankAccountServiceTest {
    private final BankAccountRepository repository = mock(BankAccountRepository.class);
    private final BankAccountService service = new BankAccountService(repository);

    @Test
    void createsAccountWithTrimmedFieldsAndCompany() {
        // Arrange
        Company company = company(7L);
        when(repository.save(any(BankAccount.class))).thenAnswer(call -> call.getArgument(0));

        // Act
        BankAccount result = service.createBankAccount(company, " Banco ", " 1234 ", " 12345 ", " Corrente ", false);

        // Assert: verifica todos os campos enviados para persistencia.
        assertThat(result.getCompany()).isSameAs(company);
        assertThat(result.getBankName()).isEqualTo("Banco");
        assertThat(result.getBranch()).isEqualTo("1234");
        assertThat(result.getAccountNumber()).isEqualTo("12345");
        assertThat(result.getAccountType()).isEqualTo("Corrente");
        assertThat(result.isDefaultAccount()).isFalse();
        verify(repository).findByCompanyAndBankNameAndBranchAndAccountNumber(company, "Banco", "1234", "12345");
        verify(repository).save(result);
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void createsDefaultAccountAndClearsPreviousDefault() {
        // Arrange: inclui uma conta padrao e outra comum para exercitar os dois casos.
        Company company = company(7L);
        BankAccount previous = account(1L, company);
        previous.setDefaultAccount(true);
        BankAccount regular = account(2L, company);
        List<BankAccount> accounts = List.of(previous, regular);
        when(repository.findByCompany(company)).thenReturn(accounts);
        when(repository.save(any(BankAccount.class))).thenAnswer(call -> call.getArgument(0));

        // Act
        BankAccount result = service.createBankAccount(company, "Banco", "1234", "12345", "Corrente", true);

        // Assert
        assertThat(result.isDefaultAccount()).isTrue();
        assertThat(previous.isDefaultAccount()).isFalse();
        assertThat(regular.isDefaultAccount()).isFalse();
        verify(repository).saveAll(accounts);
    }

    @Test
    void rejectsDuplicateAccountBeforeChangingDefaultsOrSaving() {
        // Arrange
        Company company = company(7L);
        when(repository.findByCompanyAndBankNameAndBranchAndAccountNumber(company, "Banco", "1234", "12345"))
                .thenReturn(account(1L, company));

        // Act & Assert
        assertThatThrownBy(() -> service.createBankAccount(company, "Banco", "1234", "12345", "Corrente", true))
                .isInstanceOf(BusinessRuleException.class);
        verify(repository, never()).save(any());
        verify(repository, never()).findByCompany(any());
    }

    // Cada linha informa um campo invalido; NULL representa ausencia do valor.
    @ParameterizedTest
    @CsvSource(nullValues = "NULL", value = {
            "NULL,1234,12345,Corrente", "' ',1234,12345,Corrente",
            "Banco,NULL,12345,Corrente", "Banco,' ',12345,Corrente",
            "Banco,123,12345,Corrente", "Banco,ABCD,12345,Corrente", "Banco,12345,12345,Corrente",
            "Banco,1234,NULL,Corrente", "Banco,1234,' ',Corrente",
            "Banco,1234,123,Corrente", "Banco,1234,12345678901234,Corrente", "Banco,1234,ABCD,Corrente",
            "Banco,1234,12345,NULL", "Banco,1234,12345,' '"
    })
    void rejectsInvalidFieldsWithoutAccessingRepository(String bank, String branch, String number, String type) {
        // Arrange
        Company company = company(7L);

        // Act & Assert
        assertThatThrownBy(() -> service.createBankAccount(company, bank, branch, number, type, false))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsNullCompanyAndNullAccountBeforeAccessingRepository() {
        // Act & Assert: valida os argumentos antes de tentar alterar dados.
        assertThatThrownBy(() -> service.createBankAccount(null, "Banco", "1234", "12345", "Corrente", false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.deleteBankAccount(company(7L), null))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void findsAccountByIdAndRejectsMissingOrNullId() {
        // Arrange
        BankAccount existing = account(1L, company(7L));
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.findById(2L)).thenReturn(Optional.empty());

        // Act
        BankAccount result = service.findById(1L);

        // Assert
        assertThat(result).isSameAs(existing);
        assertThatThrownBy(() -> service.findById(2L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.findById(null)).isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).findById(null);
    }

    @Test
    void listsCompanyAccountsAndReturnsItsDefaultAccount() {
        // Arrange
        Company company = company(7L);
        BankAccount existing = account(1L, company);
        when(repository.findByCompany(company)).thenReturn(List.of(existing));
        when(repository.findByCompanyAndDefaultAccountTrue(company)).thenReturn(existing);

        // Act
        List<BankAccount> result = service.findByCompany(company);
        BankAccount defaultAccount = service.findDefaultByCompany(company);

        // Assert
        assertThat(result).containsExactly(existing);
        assertThat(defaultAccount).isSameAs(existing);
    }

    @Test
    void definesDefaultAndSavesSelectedAccountOnlyOnce() {
        // Arrange
        Company company = company(7L);
        BankAccount selected = account(1L, company);
        when(repository.findByCompany(company)).thenReturn(List.of());
        when(repository.save(selected)).thenReturn(selected);

        // Act
        BankAccount result = service.defineDefaultAccount(company, selected);

        // Assert
        assertThat(result.isDefaultAccount()).isTrue();
        verify(repository).saveAll(List.of());
        verify(repository, times(1)).save(selected);
    }

    @Test
    void deletesAccountOwnedByCompany() {
        // Arrange
        Company company = company(7L);
        BankAccount existing = account(1L, company);

        // Act
        service.deleteBankAccount(company, existing);

        // Assert
        verify(repository).delete(existing);
    }

    @Test
    void rejectsAccountWithoutCompany() {
        // Arrange
        BankAccount existing = account(1L, null);

        // Act & Assert
        assertThatThrownBy(() -> service.deleteBankAccount(company(7L), existing))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsAccountWhoseCompanyHasNoId() {
        // Arrange
        BankAccount existing = account(1L, company(null));

        // Act & Assert
        assertThatThrownBy(() -> service.deleteBankAccount(company(7L), existing))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsUnregisteredTargetCompany() {
        // Arrange
        BankAccount existing = account(1L, company(7L));

        // Act & Assert
        assertThatThrownBy(() -> service.deleteBankAccount(company(null), existing))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsAccountBelongingToAnotherCompany() {
        // Arrange
        BankAccount existing = account(1L, company(99L));

        // Act & Assert
        assertThatThrownBy(() -> service.defineDefaultAccount(company(7L), existing))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(existing.isDefaultAccount()).isFalse();
        verifyNoInteractions(repository);
    }

    @Test
    void updatesAccountWithoutTreatingItsOwnDataAsDuplicate() {
        // Arrange
        Company company = company(7L);
        BankAccount existing = account(1L, company);
        when(repository.findByCompanyAndBankNameAndBranchAndAccountNumber(company, "Novo", "4321", "54321"))
                .thenReturn(existing);
        when(repository.save(existing)).thenReturn(existing);

        // Act
        BankAccount result = service.updateBankAccount(company, existing, " Novo ", " 4321 ", " 54321 ", " Poupanca ", false);

        // Assert
        assertThat(result).isSameAs(existing);
        assertThat(result.getBankName()).isEqualTo("Novo");
        assertThat(result.getBranch()).isEqualTo("4321");
        assertThat(result.getAccountNumber()).isEqualTo("54321");
        assertThat(result.getAccountType()).isEqualTo("Poupanca");
        assertThat(result.isDefaultAccount()).isFalse();
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void updatesAccountToDefaultWhenThereIsNoDuplicate() {
        // Arrange
        Company company = company(7L);
        BankAccount existing = account(1L, company);
        BankAccount previous = account(2L, company);
        previous.setDefaultAccount(true);
        when(repository.findByCompany(company)).thenReturn(List.of(previous, existing));
        when(repository.save(existing)).thenReturn(existing);

        // Act
        BankAccount result = service.updateBankAccount(company, existing, "Novo", "4321", "54321", "Corrente", true);

        // Assert
        assertThat(result.isDefaultAccount()).isTrue();
        assertThat(previous.isDefaultAccount()).isFalse();
        verify(repository).saveAll(List.of(previous, existing));
        verify(repository).save(existing);
    }

    @Test
    void rejectsUpdateDuplicatingAnotherAccountWithoutChangingOriginal() {
        // Arrange
        Company company = company(7L);
        BankAccount existing = account(1L, company);
        when(repository.findByCompanyAndBankNameAndBranchAndAccountNumber(company, "Novo", "4321", "54321"))
                .thenReturn(account(2L, company));

        // Act & Assert
        assertThatThrownBy(() -> service.updateBankAccount(company, existing, "Novo", "4321", "54321", "Corrente", false))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(existing.getBankName()).isEqualTo("Banco");
        verify(repository, never()).save(any());
    }

    private static Company company(Long id) {
        Company company = new Company();
        company.setId(id);
        return company;
    }

    private static BankAccount account(Long id, Company company) {
        BankAccount account = new BankAccount();
        account.setId(id);
        account.setCompany(company);
        account.setBankName("Banco");
        account.setBranch("1234");
        account.setAccountNumber("12345");
        account.setAccountType("Corrente");
        return account;
    }
}
