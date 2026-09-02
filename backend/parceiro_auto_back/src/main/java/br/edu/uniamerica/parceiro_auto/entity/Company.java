package br.edu.uniamerica.parceiro_auto.entity;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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
  private String razaoSocial;

  @Column(nullable = false, length = 50)
  private String nomeFantasia;

  @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
  private List<UserCompany> userCompanies;
}