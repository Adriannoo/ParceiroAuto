package br.edu.uniamerica.parceiro_auto.entity;

import java.util.List;

import br.edu.uniamerica.parceiro_auto.entity.enums.CompanySize;
import br.edu.uniamerica.parceiro_auto.entity.enums.LegalNature;
import br.edu.uniamerica.parceiro_auto.entity.enums.TaxRegime;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "userCompanies")
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "company")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 14)
    private String cnpj;

    @Column(nullable = false, length = 50)
    private String legalName;

    @Column(nullable = false, length = 50)
    private String tradeName;

    @Column(length = 30)
    private String stateRegistration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaxRegime taxRegime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompanySize size;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LegalNature legalNature;

    @Column(nullable = false, length = 10)
    private String postalCode;

    @Column(nullable = false, length = 100)
    private String street;

    @Column(nullable = false, length = 10)
    private String streetNumber;

    @Column(length = 100)
    private String addressComplement;

    @Column(nullable = false, length = 80)
    private String neighborhood;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(nullable = false, length = 2)
    private String state;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
    private List<UserCompany> userCompanies;
}
