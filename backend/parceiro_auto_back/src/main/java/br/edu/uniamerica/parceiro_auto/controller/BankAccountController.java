package br.edu.uniamerica.parceiro_auto.controller;

import br.edu.uniamerica.parceiro_auto.controller.dto.ApiResponse;
import br.edu.uniamerica.parceiro_auto.controller.dto.BankAccountRequestDTO;
import br.edu.uniamerica.parceiro_auto.controller.dto.BankAccountResponseDTO;
import br.edu.uniamerica.parceiro_auto.controller.dto.mapper.BankAccountMapper;
import br.edu.uniamerica.parceiro_auto.entity.BankAccount;
import br.edu.uniamerica.parceiro_auto.entity.Company;
import br.edu.uniamerica.parceiro_auto.service.BankAccountService;
import br.edu.uniamerica.parceiro_auto.service.CompanyService;
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

    // Construtor
    public BankAccountController(BankAccountService bankAccountService, CompanyService companyService) {
        this.bankAccountService = bankAccountService;
        this.companyService = companyService;
    }

    // Endpoint para criar uma nova conta bancaria
    // Queremos devolver 201 CREATED, que e o status para criacao.
    // POST localhost:8080/api/bank-accounts
    @PostMapping
    public ResponseEntity<ApiResponse<BankAccountResponseDTO>> create(@RequestBody BankAccountRequestDTO dto){
        Company company = companyService.findById(dto.companyId())
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa nao encontrada")
                );

        BankAccount bankAccount = bankAccountService.createBankAccount(
                company, dto.bankName(), dto.branch(), dto.accountNumber(), dto.accountType(), dto.defaultAccount()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Conta bancaria criada com sucesso!", BankAccountMapper.toResponseDTO(bankAccount)));
    }

    //Endpoint para listar todas as contas bancarias de uma empresa
    // Queremos devolver 200 OK, status generico para sucesso
    // GET localhost:8080/api/bank-accounts/company/{companyId}
    @GetMapping("/company/{companyId}")
    public ResponseEntity<ApiResponse<List<BankAccountResponseDTO>>> findByCompany(@PathVariable Long companyId){
        Company company = companyService.findById(companyId)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa nao encontrada")
                );

        List<BankAccountResponseDTO> contas = bankAccountService.findByCompany(company)
                .stream()
                .map(BankAccountMapper::toResponseDTO)
                .toList();

        return ResponseEntity.ok(new ApiResponse<>("Contas listadas com sucesso!", contas));
    }

    

}
