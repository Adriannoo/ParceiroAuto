package br.edu.uniamerica.parceiro_auto.service;

import br.edu.uniamerica.parceiro_auto.entity.TransactionCategory;
import br.edu.uniamerica.parceiro_auto.repository.TransactionCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionCategoryService {
    private final TransactionCategoryRepository transactionalCategoryRepository;

    @Transactional(readOnly = true)
    public TransactionCategory findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("O ID nao pode ser nulo!");
        }
        return transactionalCategoryRepository.findById(id)
                .orElseThrow(
                        () -> new IllegalArgumentException("Categoria nao encontrada")
                );
    }
}
