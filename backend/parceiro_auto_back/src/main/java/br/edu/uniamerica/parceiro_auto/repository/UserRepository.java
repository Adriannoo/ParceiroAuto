package br.edu.uniamerica.parceiro_auto.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.uniamerica.parceiro_auto.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

  User findByLogin(String normalizedLogin);
  
}
