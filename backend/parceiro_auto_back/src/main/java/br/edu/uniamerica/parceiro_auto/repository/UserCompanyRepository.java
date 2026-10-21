package br.edu.uniamerica.parceiro_auto.repository;

import br.edu.uniamerica.parceiro_auto.entity.UserCompany;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCompanyRepository extends JpaRepository<UserCompany, Long> {
    boolean existsByCompany_IdAndUser_Id(Long companyId, Long userId);
}
