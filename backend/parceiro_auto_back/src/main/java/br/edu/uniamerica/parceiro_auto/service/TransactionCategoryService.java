package br.edu.uniamerica.parceiro_auto.service;

import br.edu.uniamerica.parceiro_auto.entity.Company;
import br.edu.uniamerica.parceiro_auto.entity.TransactionCategory;
import br.edu.uniamerica.parceiro_auto.entity.enums.TransactionType;
import br.edu.uniamerica.parceiro_auto.exception.ResourceNotFoundException;
import br.edu.uniamerica.parceiro_auto.repository.TransactionCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TransactionCategoryService {
    private final TransactionCategoryRepository transactionalCategoryRepository;

    public List<TransactionCategory> findActiveByCompany(Company company) {
        if (company == null) {
            throw new IllegalArgumentException("A empresa nao pode ser nula!");
        }

        List<TransactionCategory> categories = transactionalCategoryRepository.findByCompanyAndActiveTrueOrderByName(company);

        if (!categories.isEmpty()) {
            return categories;
        }

        return transactionalCategoryRepository.saveAll(List.of(
                createDefault(company, "Vendas", TransactionType.ENTRADA),
                createDefault(company, "Servicos", TransactionType.ENTRADA),
                createDefault(company, "Pecas", TransactionType.ENTRADA),
                createDefault(company, "Fornecedores", TransactionType.SAIDA),
                createDefault(company, "Salarios", TransactionType.SAIDA),
                createDefault(company, "Impostos", TransactionType.SAIDA),
                createDefault(company, "Aluguel", TransactionType.SAIDA),
                createDefault(company, "Energia", TransactionType.SAIDA),
                createDefault(company, "Manutencao", TransactionType.SAIDA),
                createDefault(company, "Outros", TransactionType.SAIDA)
        ));
    }

    @Transactional(readOnly = true)
    public TransactionCategory findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("O ID nao pode ser nulo!");
        }
        return transactionalCategoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Categoria com id({}) nao encontrada", id);
                    return new ResourceNotFoundException("Categoria nao encontrada");
                });
    }

    public TransactionCategory createCategory(Company company, String name, TransactionType type) {
        if (company == null) {
            throw new IllegalArgumentException("A empresa nao pode ser nula!");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome da categoria nao pode ser vazio!");
        }
        if (type == null) {
            throw new IllegalArgumentException("O tipo da categoria nao pode ser nulo!");
        }

        String formattedName = name.trim();

        return transactionalCategoryRepository.findByCompanyAndNameIgnoreCase(company, formattedName)
                .map(category -> {
                    if (category.getType() != type) {
                        throw new IllegalArgumentException("Ja existe uma categoria com esse nome em outro tipo.");
                    }

                    category.setActive(true);
                    return transactionalCategoryRepository.save(category);
                })
                .orElseGet(() -> transactionalCategoryRepository.save(createDefault(company, formattedName, type)));
    }

    public TransactionCategory deactivateCategory(Company company, Long categoryId) {
        TransactionCategory category = findById(categoryId);

        if (!category.getCompany().getId().equals(company.getId())) {
            throw new IllegalArgumentException("A categoria nao pertence a empresa informada.");
        }

        category.setActive(false);
        return transactionalCategoryRepository.save(category);
    }

    public TransactionCategory updateCategory(Company company, Long categoryId, String name, TransactionType type) {
        TransactionCategory category = findById(categoryId);

        if (!category.getCompany().getId().equals(company.getId())) {
            throw new IllegalArgumentException("A categoria nao pertence a empresa informada.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome da categoria nao pode ser vazio!");
        }
        if (type == null) {
            throw new IllegalArgumentException("O tipo da categoria nao pode ser nulo!");
        }

        String formattedName = name.trim();

        transactionalCategoryRepository.findByCompanyAndNameIgnoreCase(company, formattedName)
                .filter(existing -> !existing.getId().equals(category.getId()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Ja existe uma categoria com esse nome.");
                });

        category.setName(formattedName);
        category.setType(type);
        category.setActive(true);
        return transactionalCategoryRepository.save(category);
    }

    private TransactionCategory createDefault(Company company, String name, TransactionType type) {
        TransactionCategory category = new TransactionCategory();
        category.setCompany(company);
        category.setName(name);
        category.setType(type);
        category.setActive(true);
        return category;
    }
}
