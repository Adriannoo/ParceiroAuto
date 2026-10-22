package br.edu.uniamerica.parceiro_auto;

import br.edu.uniamerica.parceiro_auto.entity.Company;
import br.edu.uniamerica.parceiro_auto.entity.User;
import br.edu.uniamerica.parceiro_auto.entity.UserCompany;
import br.edu.uniamerica.parceiro_auto.entity.enums.CompanySize;
import br.edu.uniamerica.parceiro_auto.entity.enums.LegalNature;
import br.edu.uniamerica.parceiro_auto.entity.enums.TaxRegime;
import br.edu.uniamerica.parceiro_auto.entity.enums.UserCompanyRole;
import br.edu.uniamerica.parceiro_auto.repository.CompanyRepository;
import br.edu.uniamerica.parceiro_auto.repository.UserCompanyRepository;
import br.edu.uniamerica.parceiro_auto.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:company-repository;NON_KEYWORDS=TRANSACTION,VALUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class CompanyRepositoryTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCompanyRepository userCompanyRepository;

    @Test
    void deveRetornarSomenteEmpresasVinculadasAoUsuarioSolicitado() {
        // Arrange
        User owner = user("owner@example.com");
        User anotherUser = user("another@example.com");
        userRepository.saveAllAndFlush(List.of(owner, anotherUser));

        Company ownedCompany = company("11222333000181", "Empresa do proprietario");
        Company otherCompany = company("11444777000161", "Empresa de outra pessoa");
        companyRepository.saveAllAndFlush(List.of(ownedCompany, otherCompany));

        userCompanyRepository.saveAllAndFlush(List.of(
                membership(ownedCompany, owner),
                membership(otherCompany, anotherUser)
        ));

        // Act
        List<Company> ownerCompanies = companyRepository.findAllByUserCompanies_User_Id(owner.getId());
        var ownerCompanyLookup = companyRepository.findByIdAndUserCompanies_User_Id(ownedCompany.getId(), owner.getId());
        var deniedCompanyLookup = companyRepository.findByIdAndUserCompanies_User_Id(otherCompany.getId(), owner.getId());

        // Assert
        assertThat(ownerCompanies).extracting(Company::getId).containsExactly(ownedCompany.getId());
        assertThat(ownerCompanyLookup).contains(ownedCompany);
        assertThat(deniedCompanyLookup).isEmpty();
    }

    private static User user(String login) {
        User user = new User();
        user.setLogin(login);
        user.setName("Usuario de teste");
        user.setPassword("hash-de-teste");
        return user;
    }

    private static Company company(String cnpj, String name) {
        Company company = new Company();
        company.setCnpj(cnpj);
        company.setLegalName(name);
        company.setTradeName(name);
        company.setLegalNature(LegalNature.LTDA);
        company.setTaxRegime(TaxRegime.SIMPLES_NACIONAL);
        company.setSize(CompanySize.ME);
        company.setPostalCode("85801-000");
        company.setStreet("Avenida Brasil");
        company.setStreetNumber("123");
        company.setNeighborhood("Centro");
        company.setCity("Cascavel");
        company.setState("PR");
        company.setPhone("(45) 3030-1234");
        company.setEmail("empresa@example.com");
        return company;
    }

    private static UserCompany membership(Company company, User user) {
        UserCompany membership = new UserCompany();
        membership.setCompany(company);
        membership.setUser(user);
        membership.setRole(UserCompanyRole.OWNER);
        return membership;
    }
}
