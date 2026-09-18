package br.edu.uniamerica.parceiro_auto.controller;

import br.edu.uniamerica.parceiro_auto.controller.dto.ApiResponse;
import br.edu.uniamerica.parceiro_auto.controller.dto.BankAccountRequestDTO;
import br.edu.uniamerica.parceiro_auto.controller.dto.BankAccountResponseDTO;
import br.edu.uniamerica.parceiro_auto.controller.dto.mapper.BankAccountMapper;
import br.edu.uniamerica.parceiro_auto.entity.BankAccount;
import br.edu.uniamerica.parceiro_auto.entity.Company;
import br.edu.uniamerica.parceiro_auto.service.BankAccountService;
import br.edu.uniamerica.parceiro_auto.service.CompanyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("api/bank-accounts")
public class BankAccountController {
    private final BankAccountService bankAccountService;
    private final CompanyService companyService;
    public BankAccountController(BankAccountService bankAccountService, CompanyService companyService) {
        this.bankAccountService = bankAccountService;
        this.companyService = companyService;
    }
    // Endpoint para criar uma nova conta bancaria para uma empresa
    // POST localhost:8080/api/bank-accounts/company/{companyId}
    @PostMapping("/company/{companyId}")
    public ResponseEntity<ApiResponse<BankAccountResponseDTO>> create(
            @PathVariable Long companyId,
            @Valid @RequestBody BankAccountRequestDTO dto
    ) {
        Company company = findCompanyOrThrow(companyId);

        BankAccount bankAccount = bankAccountService.createBankAccount(
                company,
                dto.bankName(),
                dto.branch(),
                dto.accountNumber(),
                dto.accountType(),
                dto.defaultAccount()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Conta bancaria criada com sucesso!", BankAccountMapper.toResponseDTO(bankAccount)));
    }

    // Endpoint para listar todas as contas bancarias de uma empresa
    // GET localhost:8080/api/bank-accounts/company/{companyId}
    @GetMapping("/company/{companyId}")
    public ResponseEntity<ApiResponse<List<BankAccountResponseDTO>>> findByCompany(@PathVariable Long companyId) {
        Company company = findCompanyOrThrow(companyId);

        List<BankAccountResponseDTO> accounts = bankAccountService.findByCompany(company)
                .stream()
                .map(BankAccountMapper::toResponseDTO)
                .toList();

        return ResponseEntity.ok(new ApiResponse<>("Contas listadas com sucesso!", accounts));
    }

    // Endpoint para buscar a conta padrao de uma empresa
    // GET localhost:8080/api/bank-accounts/company/{companyId}/default
    @GetMapping("/company/{companyId}/default")
    public ResponseEntity<ApiResponse<BankAccountResponseDTO>> findByDefault(@PathVariable Long companyId) {
        Company company = findCompanyOrThrow(companyId);
        BankAccount bankAccount = bankAccountService.findDefaultByCompany(company);

        if (bankAccount == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhuma conta padrao encontrada!");
        }

        return ResponseEntity.ok(new ApiResponse<>("Conta padrao encontrada com sucesso!", BankAccountMapper.toResponseDTO(bankAccount)));
    }

    // Endpoint para buscar uma conta bancaria pelo id dentro de uma empresa
    // GET localhost:8080/api/bank-accounts/company/{companyId}/accounts/{id}
    @GetMapping("/company/{companyId}/accounts/{id}")
    public ResponseEntity<ApiResponse<BankAccountResponseDTO>> findById(
            @PathVariable Long companyId,
            @PathVariable Long id
    ) {
        Company company = findCompanyOrThrow(companyId);
        BankAccount bankAccount = bankAccountService.findById(id);
        validateAccountBelongsToCompany(company, bankAccount);

        return ResponseEntity.ok(new ApiResponse<>("Conta encontrada com sucesso!", BankAccountMapper.toResponseDTO(bankAccount)));
    }

    // Endpoint para definir uma conta bancaria como padrao
    // PATCH localhost:8080/api/bank-accounts/company/{companyId}/accounts/{id}/default
    @PatchMapping("/company/{companyId}/accounts/{id}/default")
    public ResponseEntity<ApiResponse<BankAccountResponseDTO>> defineDefault(
            @PathVariable Long companyId,
            @PathVariable Long id
    ) {
        Company company = findCompanyOrThrow(companyId);
        BankAccount bankAccount = bankAccountService.findById(id);

        BankAccount updated = bankAccountService.defineDefaultAccount(company, bankAccount);

        return ResponseEntity.ok(new ApiResponse<>("Conta definida como padrao com sucesso!", BankAccountMapper.toResponseDTO(updated)));
    }

    // Endpoint para atualizar uma conta bancaria
    // PUT localhost:8080/api/bank-accounts/company/{companyId}/accounts/{id}
    @PutMapping("/company/{companyId}/accounts/{id}")
    public ResponseEntity<ApiResponse<BankAccountResponseDTO>> update(
            @PathVariable Long companyId,
            @PathVariable Long id,
            @Valid @RequestBody BankAccountRequestDTO dto
    ) {
        Company company = findCompanyOrThrow(companyId);
        BankAccount bankAccount = bankAccountService.findById(id);

        BankAccount updated = bankAccountService.updateBankAccount(
                company,
                bankAccount,
                dto.bankName(),
                dto.branch(),
                dto.accountNumber(),
                dto.accountType(),
                dto.defaultAccount()
        );

        return ResponseEntity.ok(new ApiResponse<>("Conta atualizada com sucesso!", BankAccountMapper.toResponseDTO(updated)));
    }

    // Endpoint para deletar uma conta bancaria
    // DELETE localhost:8080/api/bank-accounts/company/{companyId}/accounts/{id}
    @DeleteMapping("/company/{companyId}/accounts/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long companyId,
            @PathVariable Long id
    ) {
        Company company = findCompanyOrThrow(companyId);
        BankAccount bankAccount = bankAccountService.findById(id);

        bankAccountService.deleteBankAccount(company, bankAccount);

        return ResponseEntity.noContent().build();
    }

    private Company findCompanyOrThrow(Long companyId) {
        return companyService.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa nao encontrada!"));
    }

    private void validateAccountBelongsToCompany(Company company, BankAccount bankAccount) {
        if (!bankAccount.getCompany().getId().equals(company.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A conta bancaria nao pertence a empresa informada!");
        }
    }
}
