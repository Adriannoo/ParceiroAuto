package br.edu.uniamerica.parceiro_auto.service;

import br.edu.uniamerica.parceiro_auto.entity.Company;
import br.edu.uniamerica.parceiro_auto.entity.User;
import br.edu.uniamerica.parceiro_auto.entity.UserCompany;
import br.edu.uniamerica.parceiro_auto.entity.enums.UserCompanyRole;
import br.edu.uniamerica.parceiro_auto.exception.ResourceNotFoundException;
import br.edu.uniamerica.parceiro_auto.repository.CompanyRepository;
import br.edu.uniamerica.parceiro_auto.repository.UserCompanyRepository;
import br.edu.uniamerica.parceiro_auto.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompanyAccessService {
    private final CompanyRepository companyRepository;
    private final UserCompanyRepository userCompanyRepository;
    private final UserRepository userRepository;

    public Long currentUserId(HttpServletRequest request) {
        var session = request.getSession(false);
        Object userId = session == null ? null : session.getAttribute("userId");
        if (!(userId instanceof Long id)) {
            throw new ResourceNotFoundException("Usuario nao autenticado");
        }
        return id;
    }

    public Company requireCompany(Long companyId, HttpServletRequest request) {
        Long userId = currentUserId(request);
        return companyRepository.findByIdAndUserCompanies_User_Id(companyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa nao encontrada"));
    }

    public void requireAccess(Company company, HttpServletRequest request) {
        if (company == null || company.getId() == null
                || !userCompanyRepository.existsByCompany_IdAndUser_Id(company.getId(), currentUserId(request))) {
            throw new ResourceNotFoundException("Empresa nao encontrada");
        }
    }

    public void requireAccess(Long companyId, HttpServletRequest request) {
        if (!userCompanyRepository.existsByCompany_IdAndUser_Id(companyId, currentUserId(request))) {
            throw new ResourceNotFoundException("Empresa nao encontrada");
        }
    }

    public void linkOwner(Company company, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));
        UserCompany membership = new UserCompany();
        membership.setCompany(company);
        membership.setUser(user);
        membership.setRole(UserCompanyRole.OWNER);
        userCompanyRepository.save(membership);
    }
}
