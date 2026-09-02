package br.edu.uniamerica.parceiro_auto.service;

import br.edu.uniamerica.parceiro_auto.controller.dto.TransactionRequestDTO;
import br.edu.uniamerica.parceiro_auto.controller.dto.TransactionResponseDTO;
import br.edu.uniamerica.parceiro_auto.entity.enums.TransactionType;
import br.edu.uniamerica.parceiro_auto.repository.TransactionRepository;
import br.edu.uniamerica.parceiro_auto.entity.Transaction;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;


//Refatorar service
@Service
public class TransactionService {

    private final TransactionRepository movimentacaoRepository;

    public TransactionService(TransactionRepository movimentacaoRepository) {
        this.movimentacaoRepository = movimentacaoRepository;
    }

    public TransactionResponseDTO criar(TransactionRequestDTO dto) {
        Transaction entity = new Transaction();

        entity.setEmpresa(dto.empresa());
        entity.setConta(dto.conta());
        entity.setCategoria(dto.categoria());
        entity.setTipo(dto.tipo());
        entity.setDescricao(dto.descricao());
        entity.setValor(dto.valor());
        entity.setData(dto.data());
        entity.setForma(dto.forma());

        Transaction salvo = movimentacaoRepository.save(entity);

        return toResponseDTO(salvo);
    }

    // Metodo privado para converter entity em DTO. Evita duplicacao de codigo e facilita manutencao
    public TransactionResponseDTO toResponseDTO(Transaction entity) {
        return new TransactionResponseDTO(
                entity.getId(),
                entity.getEmpresa(),
                entity.getConta(),
                entity.getCategoria(),
                entity.getTipo(),
                entity.getDescricao(),
                entity.getValor(),
                entity.getData(),
                entity.getForma()
        );
    }

    public TransactionResponseDTO atualizar(Long id, TransactionRequestDTO dto) {
        Transaction entity = movimentacaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movimentacao nao encontrada com o ID: " + id));

        entity.setEmpresa(dto.empresa());
        entity.setConta(dto.conta());
        entity.setCategoria(dto.categoria());
        entity.setTipo(dto.tipo());
        entity.setDescricao(dto.descricao());
        entity.setValor(dto.valor());
        entity.setData(dto.data());
        entity.setForma(dto.forma());

        Transaction atualizado = movimentacaoRepository.save(entity);

        return toResponseDTO(atualizado);
    }

    public List<TransactionResponseDTO> listar() {
        return movimentacaoRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public TransactionResponseDTO buscarPorId(Long id) {
        Transaction entity = movimentacaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movimentacao nao encontrada com o ID: " + id));

        return toResponseDTO(entity);
    }

    public List<TransactionResponseDTO> filtrarPorTipo(TransactionType tipo) {
        return movimentacaoRepository.findByTipo(tipo)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public void deletar(Long id) {
        Transaction entity = movimentacaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movimentacao nao encontrada com o ID: " + id));

        movimentacaoRepository.delete(entity);
    }
}
