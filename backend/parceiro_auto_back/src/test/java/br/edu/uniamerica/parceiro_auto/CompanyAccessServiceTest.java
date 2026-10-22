package br.edu.uniamerica.parceiro_auto;

import br.edu.uniamerica.parceiro_auto.entity.Company;
import br.edu.uniamerica.parceiro_auto.entity.User;
import br.edu.uniamerica.parceiro_auto.entity.UserCompany;
import br.edu.uniamerica.parceiro_auto.entity.enums.UserCompanyRole;
import br.edu.uniamerica.parceiro_auto.exception.ResourceNotFoundException;
import br.edu.uniamerica.parceiro_auto.repository.CompanyRepository;
import br.edu.uniamerica.parceiro_auto.repository.UserCompanyRepository;
import br.edu.uniamerica.parceiro_auto.repository.UserRepository;
import br.edu.uniamerica.parceiro_auto.service.CompanyAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CompanyAccessServiceTest {

    private static final Long USER_ID = 42L;
    private static final Long COMPANY_ID = 7L;

    private final CompanyRepository companyRepository = mock(CompanyRepository.class);
    private final UserCompanyRepository userCompanyRepository = mock(UserCompanyRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final CompanyAccessService service = new CompanyAccessService(
            companyRepository,
            userCompanyRepository,
            userRepository
    );

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", USER_ID);
        request.setSession(session);
    }

    @Test
    void returnsCompanyWhenItBelongsToCurrentUser() {
        // Arrange
        Company company = new Company();
        company.setId(COMPANY_ID);
        when(companyRepository.findByIdAndUserCompanies_User_Id(COMPANY_ID, USER_ID))
                .thenReturn(Optional.of(company));

        // Act
        Company result = service.requireCompany(COMPANY_ID, request);

        // Assert
        assertThat(result).isSameAs(company);
        verify(companyRepository).findByIdAndUserCompanies_User_Id(COMPANY_ID, USER_ID);
        verifyNoInteractions(userCompanyRepository, userRepository);
    }

    @Test
    void rejectsCompanyThatDoesNotBelongToCurrentUser() {
        // Arrange
        when(companyRepository.findByIdAndUserCompanies_User_Id(COMPANY_ID, USER_ID))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.requireCompany(COMPANY_ID, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Empresa nao encontrada");
        verify(companyRepository).findByIdAndUserCompanies_User_Id(COMPANY_ID, USER_ID);
        verifyNoInteractions(userCompanyRepository, userRepository);
    }

    @Test
    void rejectsRequestWithoutAuthenticatedUserBeforeQueryingCompanies() {
        // Arrange
        request = new MockHttpServletRequest();

        // Act & Assert
        assertThatThrownBy(() -> service.requireCompany(COMPANY_ID, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuario nao autenticado");
        verifyNoInteractions(companyRepository, userCompanyRepository, userRepository);
    }

    @Test
    void returnsCurrentUserIdFromSession() {
        // Act
        Long result = service.currentUserId(request);

        // Assert
        assertThat(result).isEqualTo(USER_ID);
        verifyNoInteractions(companyRepository, userCompanyRepository, userRepository);
    }

    @Test
    void rejectsSessionWhenUserIdHasUnexpectedType() {
        // Arrange
        request.getSession().setAttribute("userId", "42");

        // Act & Assert
        assertThatThrownBy(() -> service.currentUserId(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuario nao autenticado");
        verifyNoInteractions(companyRepository, userCompanyRepository, userRepository);
    }

    @Test
    void allowsAccessWhenCompanyIsLinkedToUser() {
        // Arrange
        when(userCompanyRepository.existsByCompany_IdAndUser_Id(COMPANY_ID, USER_ID)).thenReturn(true);

        // Act
        service.requireAccess(COMPANY_ID, request);

        // Assert
        verify(userCompanyRepository).existsByCompany_IdAndUser_Id(COMPANY_ID, USER_ID);
        verifyNoInteractions(companyRepository, userRepository);
    }

    @Test
    void rejectsAccessWhenCompanyIsNotLinkedToUser() {
        // Arrange
        when(userCompanyRepository.existsByCompany_IdAndUser_Id(COMPANY_ID, USER_ID)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> service.requireAccess(COMPANY_ID, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Empresa nao encontrada");
        verify(userCompanyRepository).existsByCompany_IdAndUser_Id(COMPANY_ID, USER_ID);
        verifyNoInteractions(companyRepository, userRepository);
    }

    @Test
    void allowsAccessToCompanyEntityLinkedToCurrentUser() {
        // Arrange
        Company company = new Company();
        company.setId(COMPANY_ID);
        when(userCompanyRepository.existsByCompany_IdAndUser_Id(COMPANY_ID, USER_ID)).thenReturn(true);

        // Act
        service.requireAccess(company, request);

        // Assert
        verify(userCompanyRepository).existsByCompany_IdAndUser_Id(COMPANY_ID, USER_ID);
        verifyNoInteractions(companyRepository, userRepository);
    }

    @Test
    void rejectsCompanyWithoutIdBeforeCheckingMembership() {
        // Arrange
        Company company = new Company();

        // Act & Assert
        assertThatThrownBy(() -> service.requireAccess(company, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Empresa nao encontrada");
        verifyNoInteractions(companyRepository, userCompanyRepository, userRepository);
    }

    @Test
    void rejectsNullCompanyBeforeCheckingMembership() {
        // Act & Assert
        assertThatThrownBy(() -> service.requireAccess((Company) null, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Empresa nao encontrada");
        verifyNoInteractions(companyRepository, userCompanyRepository, userRepository);
    }

    @Test
    void linksNewCompanyToUserAsOwner() {
        // Arrange
        Company company = new Company();
        User user = new User();
        user.setId(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        // Act
        service.linkOwner(company, USER_ID);

        // Assert
        org.mockito.ArgumentCaptor<UserCompany> membershipCaptor = org.mockito.ArgumentCaptor.forClass(UserCompany.class);
        verify(userCompanyRepository).save(membershipCaptor.capture());
        UserCompany membership = membershipCaptor.getValue();
        assertThat(membership.getCompany()).isSameAs(company);
        assertThat(membership.getUser()).isSameAs(user);
        assertThat(membership.getRole()).isEqualTo(UserCompanyRole.OWNER);
        verify(userRepository).findById(USER_ID);
        verifyNoMoreInteractions(userRepository, userCompanyRepository);
        verifyNoInteractions(companyRepository);
    }

    @Test
    void rejectsOwnerLinkWhenUserDoesNotExist() {
        // Arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.linkOwner(new Company(), USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuario nao encontrado");
        verify(userRepository).findById(USER_ID);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(companyRepository, userCompanyRepository);
    }
}
