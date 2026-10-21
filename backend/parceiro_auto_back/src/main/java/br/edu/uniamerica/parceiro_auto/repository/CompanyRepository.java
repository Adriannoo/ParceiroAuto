package br.edu.uniamerica.parceiro_auto.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.uniamerica.parceiro_auto.entity.Company;

import java.util.Optional;
import java.util.List;

public interface CompanyRepository extends JpaRepository<Company, Long> {

  Company findByCnpj(String normalizedCnpj);
  Optional<Company> findByIdAndUserCompanies_User_Id(Long id, Long userId);
  List<Company> findAllByUserCompanies_User_Id(Long userId);
  Company findByCnpjAndUserCompanies_User_Id(String cnpj, Long userId);
  
}
