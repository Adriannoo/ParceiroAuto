package br.edu.uniamerica.parceiro_auto.controller;

import br.edu.uniamerica.parceiro_auto.controller.dto.ApiResponse;
import br.edu.uniamerica.parceiro_auto.controller.dto.TransactionRequestDTO;
import br.edu.uniamerica.parceiro_auto.controller.dto.TransactionResponseDTO;
import br.edu.uniamerica.parceiro_auto.entity.enums.TransactionType;
import br.edu.uniamerica.parceiro_auto.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movimentacoes")
public class TransactionController {

    private final TransactionService movimentacaoService;

    public TransactionController(TransactionService movimentacaoService) {
        this.movimentacaoService = movimentacaoService;
    }

    // POST http://localhost:8080/api/movimentacoes
    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> criar(@RequestBody TransactionRequestDTO dto) {
        TransactionResponseDTO resposta = movimentacaoService.criar(dto);
        return new ResponseEntity<>(
                new ApiResponse<>("Movimentacao criada com sucesso", resposta),
                HttpStatus.CREATED
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TransactionResponseDTO>>> listar() {
        List<TransactionResponseDTO> lista = movimentacaoService.listar();
        return new ResponseEntity<>(
                new ApiResponse<>("Movimentacoes listadas com sucesso", lista),
                HttpStatus.OK
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> buscarPorId(@PathVariable Long id) {
        TransactionResponseDTO resposta = movimentacaoService.buscarPorId(id);
        return new ResponseEntity<>(
                new ApiResponse<>("Movimentacao encontrada", resposta),
                HttpStatus.OK
        );
    }

    @GetMapping("/filtro")
    public ResponseEntity<ApiResponse<List<TransactionResponseDTO>>> filtrarPorTipo(@RequestParam TransactionType tipo) {
        List<TransactionResponseDTO> lista = movimentacaoService.filtrarPorTipo(tipo);
        return new ResponseEntity<>(
                new ApiResponse<>("Movimentacoes filtradas com sucesso", lista),
                HttpStatus.OK
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> atualizar(
            @PathVariable Long id,
            @RequestBody TransactionRequestDTO dto) {
        TransactionResponseDTO resposta = movimentacaoService.atualizar(id, dto);
        return new ResponseEntity<>(
                new ApiResponse<>("Movimentacao atualizada com sucesso", resposta),
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        movimentacaoService.deletar(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}