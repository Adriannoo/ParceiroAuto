package br.edu.uniamerica.parceiro_auto.controller.dto.mapper;

import br.edu.uniamerica.parceiro_auto.controller.dto.BankAccountResponseDTO;
import br.edu.uniamerica.parceiro_auto.entity.BankAccount;

public class BankAccountMapper {
    public static BankAccountResponseDTO toResponseDTO(BankAccount bankAccount) {
        return new BankAccountResponseDTO(
                bankAccount.getId(),
                bankAccount.getBankName(),
                bankAccount.getBranch(),
                bankAccount.getAccountNumber(),
                bankAccount.getAccountType(),
                bankAccount.getBalance(),
                bankAccount.isDefaultAccount(),
                bankAccount.getCompany().getId()
        );
    }
}
