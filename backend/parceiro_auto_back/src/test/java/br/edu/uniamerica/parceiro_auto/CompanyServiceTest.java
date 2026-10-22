package br.edu.uniamerica.parceiro_auto;

import br.edu.uniamerica.parceiro_auto.client.BrasilApiClient;
import br.edu.uniamerica.parceiro_auto.client.BrasilApiCnpjResponse;
import br.edu.uniamerica.parceiro_auto.controller.dto.company.CompanyLookupResponseDTO;
import br.edu.uniamerica.parceiro_auto.controller.dto.company.CompanyRequestDTO;
import br.edu.uniamerica.parceiro_auto.entity.Company;
import br.edu.uniamerica.parceiro_auto.entity.enums.CompanySize;
import br.edu.uniamerica.parceiro_auto.entity.enums.LegalNature;
import br.edu.uniamerica.parceiro_auto.entity.enums.TaxRegime;
import br.edu.uniamerica.parceiro_auto.exception.BusinessRuleException;
import br.edu.uniamerica.parceiro_auto.exception.ResourceNotFoundException;
import br.edu.uniamerica.parceiro_auto.repository.CompanyRepository;
import br.edu.uniamerica.parceiro_auto.service.CompanyAccessService;
import br.edu.uniamerica.parceiro_auto.service.CompanyService;
import br.edu.uniamerica.parceiro_auto.service.TransactionCategoryService;
import feign.FeignException;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CompanyServiceTest {

    private static final Long OWNER_ID = 42L;
    private static final Long COMPANY_ID = 10L;
    private static final String NORMALIZED_CNPJ = "11222333000181";

    private final CompanyRepository companyRepository = mock(CompanyRepository.class);
    private final BrasilApiClient brasilApiClient = mock(BrasilApiClient.class);
    private final TransactionCategoryService categoryService = mock(TransactionCategoryService.class);
    private final CompanyAccessService accessService = mock(CompanyAccessService.class);
    private final CompanyService service = new CompanyService(
            companyRepository,
            brasilApiClient,
            categoryService,
            accessService
    );

    private CompanyRequestDTO request;

    @BeforeEach
    void setUp() {
        request = companyRequest("11.222.333/0001-81", " Oficina Exemplo ", "pr", " IE 123 ", " Sala 2 ", null);
    }

    @Test
    void deveCriarEmpresaComDadosNormalizadosEVinculosIniciais() {
        // Arrange
        when(companyRepository.findByCnpj(NORMALIZED_CNPJ)).thenReturn(null);
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company saved = invocation.getArgument(0);
            saved.setId(COMPANY_ID);
            return saved;
        });

        // Act
        Company result = service.createCompany(request, OWNER_ID);

        // Assert
        assertThat(result.getId()).isEqualTo(COMPANY_ID);
        assertThat(result.getCnpj()).isEqualTo(NORMALIZED_CNPJ);
        assertThat(result.getLegalName()).isEqualTo("Oficina Exemplo");
        assertThat(result.getState()).isEqualTo("PR");
        assertThat(result.getStateRegistration()).isEqualTo("IE 123");
        assertThat(result.getAddressComplement()).isEqualTo("Sala 2");
        assertThat(result.isActive()).isTrue();
        verify(companyRepository).save(result);
        verify(accessService).linkOwner(result, OWNER_ID);
        verify(categoryService).createDefaults(result);
        verifyNoInteractions(brasilApiClient);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "123"})
    void deveRejeitarCnpjAusenteOuInvalidoAntesDeSalvar(String cnpj) {
        // Arrange
        request = companyRequest(cnpj, "Oficina Exemplo", "PR", "IE", null, true);

        // Act & Assert
        assertThatThrownBy(() -> service.createCompany(request, OWNER_ID))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(companyRepository, accessService, categoryService, brasilApiClient);
    }

    @Test
    void deveRejeitarCnpjDuplicadoSemCriarVinculosOuCategorias() {
        // Arrange
        Company existingCompany = company(COMPANY_ID, NORMALIZED_CNPJ, "Empresa Existente");
        when(companyRepository.findByCnpj(NORMALIZED_CNPJ)).thenReturn(existingCompany);

        // Act & Assert
        assertThatThrownBy(() -> service.createCompany(request, OWNER_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Ja existe uma empresa com esse CNPJ");
        verify(companyRepository).findByCnpj(NORMALIZED_CNPJ);
        verifyNoMoreInteractions(companyRepository);
        verifyNoInteractions(accessService, categoryService, brasilApiClient);
    }

    @Test
    void deveRejeitarRazaoSocialVaziaAntesDeSalvar() {
        // Arrange
        request = companyRequest("11222333000181", "   ", "PR", "IE", null, true);
        when(companyRepository.findByCnpj(NORMALIZED_CNPJ)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> service.createCompany(request, OWNER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A razao social nao pode estar vazia");
        verify(companyRepository).findByCnpj(NORMALIZED_CNPJ);
        verifyNoMoreInteractions(companyRepository);
        verifyNoInteractions(accessService, categoryService, brasilApiClient);
    }

    @Test
    void deveAtualizarEmpresaNormalizandoCamposOpcionaisEUf() {
        // Arrange
        Company company = company(COMPANY_ID, NORMALIZED_CNPJ, "Nome Antigo");
        request = companyRequest("11.222.333/0001-81", " Nome Atualizado ", "pr", "  ", " ", false);
        when(companyRepository.findByCnpj(NORMALIZED_CNPJ)).thenReturn(company);
        when(companyRepository.save(company)).thenReturn(company);

        // Act
        Company result = service.updateCompany(company, request);

        // Assert
        assertThat(result.getLegalName()).isEqualTo("Nome Atualizado");
        assertThat(result.getState()).isEqualTo("PR");
        assertThat(result.getStateRegistration()).isNull();
        assertThat(result.getAddressComplement()).isNull();
        assertThat(result.isActive()).isFalse();
        verify(companyRepository).save(company);
        verifyNoInteractions(accessService, categoryService, brasilApiClient);
    }

    @Test
    void deveRejeitarAtualizacaoComCnpjDeOutraEmpresaSemAlterarEmpresaAtual() {
        // Arrange
        Company company = company(COMPANY_ID, "00112233000181", "Nome Original");
        Company otherCompany = company(20L, NORMALIZED_CNPJ, "Outra Empresa");
        when(companyRepository.findByCnpj(NORMALIZED_CNPJ)).thenReturn(otherCompany);

        // Act & Assert
        assertThatThrownBy(() -> service.updateCompany(company, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Ja existe uma empresa com esse CNPJ");
        assertThat(company.getCnpj()).isEqualTo("00112233000181");
        assertThat(company.getLegalName()).isEqualTo("Nome Original");
        verify(companyRepository).findByCnpj(NORMALIZED_CNPJ);
        verifyNoMoreInteractions(companyRepository);
        verifyNoInteractions(accessService, categoryService, brasilApiClient);
    }

    @Test
    void deveRejeitarAtualizacaoComEmpresaNula() {
        // Act & Assert
        assertThatThrownBy(() -> service.updateCompany(null, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A empresa nao pode ser nula");
        verifyNoInteractions(companyRepository, accessService, categoryService, brasilApiClient);
    }

    @Test
    void deveRetornarVazioAoBuscarPorIdNulo() {
        // Act
        Optional<Company> result = service.findById(null);

        // Assert
        assertThat(result).isEmpty();
        verifyNoInteractions(companyRepository, accessService, categoryService, brasilApiClient);
    }

    @Test
    void deveBuscarEmpresaPorId() {
        // Arrange
        Company company = company(COMPANY_ID, NORMALIZED_CNPJ, "Minha Empresa");
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));

        // Act
        Optional<Company> result = service.findById(COMPANY_ID);

        // Assert
        assertThat(result).contains(company);
        verify(companyRepository).findById(COMPANY_ID);
        verifyNoMoreInteractions(companyRepository);
        verifyNoInteractions(accessService, categoryService, brasilApiClient);
    }

    @Test
    void deveRetornarTodasAsEmpresasDoRepositorio() {
        // Arrange
        List<Company> companies = List.of(company(COMPANY_ID, NORMALIZED_CNPJ, "Minha Empresa"));
        when(companyRepository.findAll()).thenReturn(companies);

        // Act
        List<Company> result = service.findAll();

        // Assert
        assertThat(result).containsExactlyElementsOf(companies);
        verify(companyRepository).findAll();
        verifyNoMoreInteractions(companyRepository);
        verifyNoInteractions(accessService, categoryService, brasilApiClient);
    }

    @Test
    void deveRetornarEmpresasDoUsuarioSolicitado() {
        // Arrange
        List<Company> companies = List.of(company(COMPANY_ID, NORMALIZED_CNPJ, "Minha Empresa"));
        when(companyRepository.findAllByUserCompanies_User_Id(OWNER_ID)).thenReturn(companies);

        // Act
        List<Company> result = service.findAllByUser(OWNER_ID);

        // Assert
        assertThat(result).containsExactlyElementsOf(companies);
        verify(companyRepository).findAllByUserCompanies_User_Id(OWNER_ID);
        verifyNoMoreInteractions(companyRepository);
        verifyNoInteractions(accessService, categoryService, brasilApiClient);
    }

    @Test
    void deveBuscarCnpjNormalizadoLimitadoAoUsuario() {
        // Arrange
        Company company = company(COMPANY_ID, NORMALIZED_CNPJ, "Minha Empresa");
        when(companyRepository.findByCnpjAndUserCompanies_User_Id(NORMALIZED_CNPJ, OWNER_ID)).thenReturn(company);

        // Act
        Company result = service.findByCnpjAndUser("11.222.333/0001-81", OWNER_ID);

        // Assert
        assertThat(result).isSameAs(company);
        verify(companyRepository).findByCnpjAndUserCompanies_User_Id(NORMALIZED_CNPJ, OWNER_ID);
        verifyNoMoreInteractions(companyRepository);
        verifyNoInteractions(accessService, categoryService, brasilApiClient);
    }

    @Test
    void deveBuscarEmpresaPeloCnpjNormalizado() {
        // Arrange
        Company company = company(COMPANY_ID, NORMALIZED_CNPJ, "Minha Empresa");
        when(companyRepository.findByCnpj(NORMALIZED_CNPJ)).thenReturn(company);

        // Act
        Company result = service.findByCnpj("11.222.333/0001-81");

        // Assert
        assertThat(result).isSameAs(company);
        verify(companyRepository).findByCnpj(NORMALIZED_CNPJ);
        verifyNoMoreInteractions(companyRepository);
        verifyNoInteractions(accessService, categoryService, brasilApiClient);
    }

    @Test
    void deveRetornarNuloParaCnpjEmBrancoSemConsultarRepositorio() {
        // Act
        Company result = service.findByCnpjAndUser("  ", OWNER_ID);

        // Assert
        assertThat(result).isNull();
        verifyNoInteractions(companyRepository, accessService, categoryService, brasilApiClient);
    }

    @Test
    void deveConsultarBrasilApiComCnpjNormalizadoEMapearResposta() {
        // Arrange
        BrasilApiCnpjResponse response = new BrasilApiCnpjResponse(
                NORMALIZED_CNPJ, "Oficina Exemplo Ltda", "Oficina Exemplo", "85801-000",
                "Avenida Brasil", "123", "Centro", "Cascavel", "PR", "(45) 3030-1234",
                "oficina@example.com"
        );
        when(brasilApiClient.findByCnpj(NORMALIZED_CNPJ)).thenReturn(response);

        // Act
        CompanyLookupResponseDTO result = service.lookupByCnpj("11.222.333/0001-81");

        // Assert
        assertThat(result.cnpj()).isEqualTo(NORMALIZED_CNPJ);
        assertThat(result.legalName()).isEqualTo("Oficina Exemplo Ltda");
        assertThat(result.tradeName()).isEqualTo("Oficina Exemplo");
        assertThat(result.city()).isEqualTo("Cascavel");
        verify(brasilApiClient).findByCnpj(NORMALIZED_CNPJ);
        verifyNoInteractions(companyRepository, accessService, categoryService);
    }

    @Test
    void deveRejeitarCnpjInvalidoSemConsultarBrasilApi() {
        // Act & Assert
        assertThatThrownBy(() -> service.lookupByCnpj("123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CNPJ invalido");
        verifyNoInteractions(brasilApiClient, companyRepository, accessService, categoryService);
    }

    @Test
    void deveTraduzirCnpjNaoEncontradoPelaBrasilApiPara404() {
        // Arrange
        when(brasilApiClient.findByCnpj(NORMALIZED_CNPJ)).thenThrow(mock(FeignException.NotFound.class));

        // Act & Assert
        assertThatThrownBy(() -> service.lookupByCnpj(NORMALIZED_CNPJ))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(brasilApiClient).findByCnpj(NORMALIZED_CNPJ);
        verifyNoInteractions(companyRepository, accessService, categoryService);
    }

    @Test
    void deveTraduzirFalhaDeConexaoComBrasilApiPara503() {
        // Arrange
        when(brasilApiClient.findByCnpj(NORMALIZED_CNPJ)).thenThrow(mock(RetryableException.class));

        // Act & Assert
        assertThatThrownBy(() -> service.lookupByCnpj(NORMALIZED_CNPJ))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        verify(brasilApiClient).findByCnpj(NORMALIZED_CNPJ);
        verifyNoInteractions(companyRepository, accessService, categoryService);
    }

    @Test
    void deveTraduzirErroInesperadoDaBrasilApiPara502() {
        // Arrange
        when(brasilApiClient.findByCnpj(NORMALIZED_CNPJ)).thenThrow(mock(FeignException.class));

        // Act & Assert
        assertThatThrownBy(() -> service.lookupByCnpj(NORMALIZED_CNPJ))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));
        verify(brasilApiClient).findByCnpj(NORMALIZED_CNPJ);
        verifyNoInteractions(companyRepository, accessService, categoryService);
    }

    @Test
    void deveRetornar502QuandoBrasilApiResponderVazio() {
        // Arrange
        when(brasilApiClient.findByCnpj(NORMALIZED_CNPJ)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> service.lookupByCnpj(NORMALIZED_CNPJ))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));
        verify(brasilApiClient).findByCnpj(NORMALIZED_CNPJ);
        verifyNoInteractions(companyRepository, accessService, categoryService);
    }

    @Test
    void deveExcluirEmpresaExistente() {
        // Arrange
        Company company = company(COMPANY_ID, NORMALIZED_CNPJ, "Minha Empresa");
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));

        // Act
        service.deleteCompany(COMPANY_ID);

        // Assert
        verify(companyRepository).findById(COMPANY_ID);
        verify(companyRepository).delete(company);
        verifyNoMoreInteractions(companyRepository);
        verifyNoInteractions(accessService, categoryService, brasilApiClient);
    }

    @Test
    void deveRejeitarExclusaoDeEmpresaInexistente() {
        // Arrange
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.deleteCompany(COMPANY_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Empresa não encontrada");
        verify(companyRepository).findById(COMPANY_ID);
        verifyNoMoreInteractions(companyRepository);
        verifyNoInteractions(accessService, categoryService, brasilApiClient);
    }

    private static CompanyRequestDTO companyRequest(
            String cnpj,
            String legalName,
            String state,
            String stateRegistration,
            String addressComplement,
            Boolean active
    ) {
        return new CompanyRequestDTO(
                cnpj,
                legalName,
                " Oficina Exemplo ",
                stateRegistration,
                LegalNature.LTDA,
                TaxRegime.SIMPLES_NACIONAL,
                CompanySize.ME,
                "85801-000",
                " Avenida Brasil ",
                " 123 ",
                addressComplement,
                " Centro ",
                " Cascavel ",
                state,
                "(45) 3030-1234",
                " oficina@example.com ",
                active
        );
    }

    private static Company company(Long id, String cnpj, String legalName) {
        Company company = new Company();
        company.setId(id);
        company.setCnpj(cnpj);
        company.setLegalName(legalName);
        return company;
    }
}
