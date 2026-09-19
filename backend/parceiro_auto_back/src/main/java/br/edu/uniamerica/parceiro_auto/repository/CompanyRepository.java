package br.edu.uniamerica.parceiro_auto.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.uniamerica.parceiro_auto.entity.Company;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

  Company findByCnpj(String normalizedCnpj);
  
}
