package br.edu.uniamerica.parceiro_auto.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "company")
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "bank_account")
public class BankAccount {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 50)
  private String banco;

  @Column(nullable = false, length = 4)
  private String agencia;

  @Column(nullable = false, length = 13)
  private String numeroConta;

  @Column(nullable = false, length = 20)
  private String tipoConta;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal saldo = BigDecimal.ZERO;

  @Column(nullable = false)
  private boolean contaPadrao;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "fk_id_company", nullable = false)
  private Company company;
}