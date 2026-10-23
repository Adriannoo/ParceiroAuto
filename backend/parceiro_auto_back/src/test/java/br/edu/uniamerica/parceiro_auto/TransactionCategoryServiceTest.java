package br.edu.uniamerica.parceiro_auto;

import br.edu.uniamerica.parceiro_auto.entity.Company;
import br.edu.uniamerica.parceiro_auto.entity.TransactionCategory;
import br.edu.uniamerica.parceiro_auto.entity.enums.TransactionType;
import br.edu.uniamerica.parceiro_auto.exception.ResourceNotFoundException;
import br.edu.uniamerica.parceiro_auto.repository.TransactionCategoryRepository;
import br.edu.uniamerica.parceiro_auto.service.TransactionCategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransactionCategoryServiceTest {

    private static final Long COMPANY_ID = 7L;
    private static final Long CATEGORY_ID = 11L;

    private final TransactionCategoryRepository repository = mock(TransactionCategoryRepository.class);
    private final TransactionCategoryService service = new TransactionCategoryService(repository);
    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(COMPANY_ID);
    }

    @Test
    void returnsOnlyActiveCategoriesForCompany() {
        // Arrange
        TransactionCategory active = category(CATEGORY_ID, "Servicos", TransactionType.ENTRADA, true);
        when(repository.findByCompanyAndActiveTrueOrderByName(company)).thenReturn(List.of(active));

        // Act
        List<TransactionCategory> result = service.findActiveByCompany(company);

        // Assert
        assertThat(result).containsExactly(active);
        verify(repository).findByCompanyAndActiveTrueOrderByName(company);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void returnsAllCategoriesIncludingInactiveWhenRequested() {
        // Arrange
        List<TransactionCategory> categories = List.of(
                category(1L, "Ativa", TransactionType.ENTRADA, true),
                category(2L, "Inativa", TransactionType.SAIDA, false)
        );
        when(repository.findByCompanyOrderByName(company)).thenReturn(categories);

        // Act
        List<TransactionCategory> result = service.findAllByCompany(company);

        // Assert
        assertThat(result).containsExactlyElementsOf(categories);
        assertThat(result).extracting(TransactionCategory::isActive).containsExactly(true, false);
        verify(repository).findByCompanyOrderByName(company);
        verifyNoMoreInteractions(repository);
    }

    @ParameterizedTest
    @NullSource
    void rejectsNullCompanyWhenListingCategories(Company ignored) {
        // Act & Assert
        assertThatThrownBy(() -> service.findActiveByCompany(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.findAllByCompany(null))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void createsTenActiveDefaultCategoriesWithExpectedNamesAndTypes() {
        // Arrange
        when(repository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        service.createDefaults(company);

        // Assert
        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<Iterable<TransactionCategory>> captor =
                org.mockito.ArgumentCaptor.forClass(Iterable.class);
        verify(repository).saveAll(captor.capture());
        List<TransactionCategory> saved = new java.util.ArrayList<>();
        captor.getValue().forEach(saved::add);
        assertThat(saved).hasSize(10)
                .extracting(TransactionCategory::getName)
                .containsExactly("Vendas", "Servicos", "Pecas", "Fornecedores", "Salarios",
                        "Impostos", "Aluguel", "Energia", "Manutencao", "Outros");
        assertThat(saved).allSatisfy(category -> {
            assertThat(category.getCompany()).isSameAs(company);
            assertThat(category.isActive()).isTrue();
        });
        assertThat(saved).filteredOn(category -> category.getType() == TransactionType.ENTRADA)
                .extracting(TransactionCategory::getName)
                .containsExactly("Vendas", "Servicos", "Pecas");
        assertThat(saved).filteredOn(category -> category.getType() == TransactionType.SAIDA)
                .hasSize(7);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findsCategoryById() {
        // Arrange
        TransactionCategory expected = category(CATEGORY_ID, "Servicos", TransactionType.ENTRADA, true);
        when(repository.findById(CATEGORY_ID)).thenReturn(Optional.of(expected));

        // Act
        TransactionCategory result = service.findById(CATEGORY_ID);

        // Assert
        assertThat(result).isSameAs(expected);
        verify(repository).findById(CATEGORY_ID);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void rejectsNullCategoryIdWithoutQueryingRepository() {
        // Act & Assert
        assertThatThrownBy(() -> service.findById(null))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsMissingCategoryId() {
        // Arrange
        when(repository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.findById(CATEGORY_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Categoria nao encontrada");
        verify(repository).findById(CATEGORY_ID);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void createsCategoryWithTrimmedNameWhenItDoesNotExist() {
        // Arrange
        when(repository.findByCompanyAndNameIgnoreCase(company, "Servicos"))
                .thenReturn(Optional.empty());
        when(repository.save(any(TransactionCategory.class))).thenAnswer(invocation -> {
            TransactionCategory saved = invocation.getArgument(0);
            saved.setId(CATEGORY_ID);
            return saved;
        });

        // Act
        TransactionCategory result = service.createCategory(company, "  Servicos  ", TransactionType.ENTRADA);

        // Assert
        assertThat(result.getId()).isEqualTo(CATEGORY_ID);
        assertThat(result.getName()).isEqualTo("Servicos");
        assertThat(result.getCompany()).isSameAs(company);
        assertThat(result.getType()).isEqualTo(TransactionType.ENTRADA);
        assertThat(result.isActive()).isTrue();
        verify(repository).findByCompanyAndNameIgnoreCase(company, "Servicos");
        verify(repository).save(result);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void reactivatesExistingCategoryWhenNameAndTypeMatch() {
        // Arrange
        TransactionCategory existing = category(CATEGORY_ID, "Servicos", TransactionType.ENTRADA, false);
        when(repository.findByCompanyAndNameIgnoreCase(company, "Servicos"))
                .thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        // Act
        TransactionCategory result = service.createCategory(company, "Servicos", TransactionType.ENTRADA);

        // Assert
        assertThat(result).isSameAs(existing);
        assertThat(result.isActive()).isTrue();
        verify(repository).findByCompanyAndNameIgnoreCase(company, "Servicos");
        verify(repository).save(existing);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void rejectsExistingCategoryNameWhenTransactionTypeDiffers() {
        // Arrange
        TransactionCategory existing = category(CATEGORY_ID, "Servicos", TransactionType.ENTRADA, true);
        when(repository.findByCompanyAndNameIgnoreCase(company, "Servicos"))
                .thenReturn(Optional.of(existing));

        // Act & Assert
        assertThatThrownBy(() -> service.createCategory(company, "Servicos", TransactionType.SAIDA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ja existe uma categoria com esse nome em outro tipo.");
        verify(repository).findByCompanyAndNameIgnoreCase(company, "Servicos");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void rejectsInvalidCategoryCreationArgumentsBeforeRepositoryCalls() {
        // Act & Assert
        assertThatThrownBy(() -> service.createCategory(null, "Servicos", TransactionType.ENTRADA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createCategory(company, "   ", TransactionType.ENTRADA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createCategory(company, "Servicos", null))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void deactivatesCategoryOwnedByCompany() {
        // Arrange
        TransactionCategory existing = category(CATEGORY_ID, "Servicos", TransactionType.ENTRADA, true);
        when(repository.findById(CATEGORY_ID)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        // Act
        TransactionCategory result = service.deactivateCategory(company, CATEGORY_ID);

        // Assert
        assertThat(result.isActive()).isFalse();
        verify(repository).findById(CATEGORY_ID);
        verify(repository).save(existing);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void rejectsDeactivationWhenCategoryBelongsToAnotherCompany() {
        // Arrange
        Company anotherCompany = new Company();
        anotherCompany.setId(99L);
        TransactionCategory existing = category(CATEGORY_ID, "Servicos", TransactionType.ENTRADA, true);
        existing.setCompany(anotherCompany);
        when(repository.findById(CATEGORY_ID)).thenReturn(Optional.of(existing));

        // Act & Assert
        assertThatThrownBy(() -> service.deactivateCategory(company, CATEGORY_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A categoria nao pertence a empresa informada.");
        verify(repository).findById(CATEGORY_ID);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void updatesCategoryAndReactivatesItWhenNameIsUnique() {
        // Arrange
        TransactionCategory existing = category(CATEGORY_ID, "Antigo", TransactionType.ENTRADA, false);
        when(repository.findById(CATEGORY_ID)).thenReturn(Optional.of(existing));
        when(repository.findByCompanyAndNameIgnoreCase(company, "Novo nome")).thenReturn(Optional.empty());
        when(repository.save(existing)).thenReturn(existing);

        // Act
        TransactionCategory result = service.updateCategory(
                company, CATEGORY_ID, "  Novo nome  ", TransactionType.SAIDA);

        // Assert
        assertThat(result.getName()).isEqualTo("Novo nome");
        assertThat(result.getType()).isEqualTo(TransactionType.SAIDA);
        assertThat(result.isActive()).isTrue();
        verify(repository).findById(CATEGORY_ID);
        verify(repository).findByCompanyAndNameIgnoreCase(company, "Novo nome");
        verify(repository).save(existing);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void allowsUpdatingCategoryWithoutTreatingItsOwnNameAsDuplicate() {
        // Arrange
        TransactionCategory existing = category(CATEGORY_ID, "Servicos", TransactionType.ENTRADA, true);
        when(repository.findById(CATEGORY_ID)).thenReturn(Optional.of(existing));
        when(repository.findByCompanyAndNameIgnoreCase(company, "Servicos")).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        // Act
        TransactionCategory result = service.updateCategory(
                company, CATEGORY_ID, "Servicos", TransactionType.ENTRADA);

        // Assert
        assertThat(result).isSameAs(existing);
        verify(repository).findById(CATEGORY_ID);
        verify(repository).findByCompanyAndNameIgnoreCase(company, "Servicos");
        verify(repository).save(existing);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void rejectsUpdatingCategoryToAnotherCategoryName() {
        // Arrange
        TransactionCategory existing = category(CATEGORY_ID, "Antigo", TransactionType.ENTRADA, true);
        TransactionCategory duplicate = category(12L, "Novo", TransactionType.ENTRADA, true);
        when(repository.findById(CATEGORY_ID)).thenReturn(Optional.of(existing));
        when(repository.findByCompanyAndNameIgnoreCase(company, "Novo")).thenReturn(Optional.of(duplicate));

        // Act & Assert
        assertThatThrownBy(() -> service.updateCategory(company, CATEGORY_ID, "Novo", TransactionType.ENTRADA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ja existe uma categoria com esse nome.");
        assertThat(existing.getName()).isEqualTo("Antigo");
        verify(repository).findById(CATEGORY_ID);
        verify(repository).findByCompanyAndNameIgnoreCase(company, "Novo");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void rejectsUpdatingCategoryWithBlankNameOrNullType() {
        // Arrange
        TransactionCategory existing = category(CATEGORY_ID, "Servicos", TransactionType.ENTRADA, true);
        when(repository.findById(CATEGORY_ID)).thenReturn(Optional.of(existing));

        // Act & Assert
        assertThatThrownBy(() -> service.updateCategory(company, CATEGORY_ID, " ", TransactionType.ENTRADA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.updateCategory(company, CATEGORY_ID, "Novo", null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, times(2)).findById(CATEGORY_ID);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void rejectsCategoryUpdatesAndDeactivationForAnotherCompany() {
        // Arrange
        Company anotherCompany = new Company();
        anotherCompany.setId(99L);
        TransactionCategory existing = category(CATEGORY_ID, "Servicos", TransactionType.ENTRADA, true);
        existing.setCompany(anotherCompany);
        when(repository.findById(CATEGORY_ID)).thenReturn(Optional.of(existing));

        // Act & Assert
        assertThatThrownBy(() -> service.updateCategory(company, CATEGORY_ID, "Novo", TransactionType.SAIDA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.deactivateCategory(company, CATEGORY_ID))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, times(2)).findById(CATEGORY_ID);
        verifyNoMoreInteractions(repository);
    }

    private TransactionCategory category(Long id, String name, TransactionType type, boolean active) {
        TransactionCategory category = new TransactionCategory();
        category.setId(id);
        category.setName(name);
        category.setCompany(company);
        category.setType(type);
        category.setActive(active);
        return category;
    }
}
