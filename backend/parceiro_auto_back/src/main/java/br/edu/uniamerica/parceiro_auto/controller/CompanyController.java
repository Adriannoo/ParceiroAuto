package br.edu.uniamerica.parceiro_auto.controller;

import br.edu.uniamerica.parceiro_auto.controller.dto.ApiResponse;
import br.edu.uniamerica.parceiro_auto.controller.dto.CompanyLookupResponseDTO;
import br.edu.uniamerica.parceiro_auto.controller.dto.CompanyRequestDTO;
import br.edu.uniamerica.parceiro_auto.controller.dto.CompanyResponseDTO;
import br.edu.uniamerica.parceiro_auto.controller.dto.mapper.CompanyMapper;
import br.edu.uniamerica.parceiro_auto.entity.Company;
import br.edu.uniamerica.parceiro_auto.service.CompanyService;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Tag(name = "Empresas") // Agrupa operacoes por assunto deles
@RestController
@RequestMapping("api/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    // Metodos embrulados no "ApiResponse", padroniza o formato de resposta dos endpoints e facilita com o front

    // End point para criar uma nova empresa
    // Queremos devolver 201 CREATED, que e o status correto apra criacao. Por isso usamos ResponseEntity
    // LOCALHOST:8080/api/companies
    @PostMapping
    @Operation(summary = "Cadastrar empresa") // Define o resumo do endpoint, e o resumo do que faz. No caso Post de cadastrar empresa
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Criado com sucesso", useReturnTypeSchema = true)
    public ResponseEntity<ApiResponse<CompanyResponseDTO>> create(@Valid @RequestBody CompanyRequestDTO dto) {
        Company company = companyService.createCompany(dto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Empresa criada com sucesso!", CompanyMapper.toResponseDTO(company)));
    }

    @GetMapping("/lookup/{cnpj}")
    public ResponseEntity<ApiResponse<CompanyLookupResponseDTO>> lookupByCnpj(@PathVariable String cnpj) {
        CompanyLookupResponseDTO company = companyService.lookupByCnpj(cnpj);

        return ResponseEntity.ok(
                new ApiResponse<>("Dados da empresa encontrados com sucesso!", company)
        );
    }
    // End point para pegar os dados da empresa pelo id
    // Queremos devolver 200 OK, status generico para sucesso
    // GET LOCALHOST:8080/api/companies/{id}
    @Operation(summary = "Buscar empresa por ID")
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<ApiResponse<CompanyResponseDTO>> findById(@PathVariable Long id) {
        Company company = companyService.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Empresa nao encontrada"
                        )
                );

        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>("Empresa encontrada com sucesso!", CompanyMapper.toResponseDTO(company)));
    }

    // End point para pegar os dados da empresa pelo cnpj
    // Queremos devolver 200 OK, status generico para sucesso
    // GET LOCALHOST:8080/api/companies?cnpj=99999999999999
    @GetMapping(params = "cnpj")
    @Operation(summary = "Buscar empresa por CNPJ")
    public ResponseEntity<ApiResponse<CompanyResponseDTO>> findByCnpj(@RequestParam("cnpj") String cnpj) {
        Company company = companyService.findByCnpj(cnpj);

        // Como o "findByCnpj" nao esta usando optional no repository, nao fazemos tratamento de retorno Optional com orElseThrow
        // Por isso o tratamento precisa ser manual
        if (company == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa nao encontrada");
        }

        return ResponseEntity.ok(new ApiResponse<>("Empresa encontrada com sucesso!", CompanyMapper.toResponseDTO(company)));
    }

    // End point para listar todas as empesas
    // Queremos devolver 200 OK, status generico para sucesso
    // GET LOCALHOST:8080/api/companies
    @GetMapping(params = "!cnpj")
    @Operation(summary = "Listar empresas")
    public ResponseEntity<ApiResponse<List<CompanyResponseDTO>>> findAll() {
        List<CompanyResponseDTO> companies = companyService.findAll().stream()
                .map(CompanyMapper::toResponseDTO)
                .toList();

        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>("Empresas listadas com sucesso!", companies));
    }

    // End point para atualizar uma empesa
    // Queremos devolver 200 OK, status generico para sucesso
    // PUT LOCALHOST:8080/api/id
    /*
     * @Valid = Mandda verificar as restrições desse DTO antes de executar o corpo, na DTO tem uma implementacao de grupo de movimentacoes com extends.
     */

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar empresa")
    public ResponseEntity<ApiResponse<CompanyResponseDTO>> updateCompany(@PathVariable Long id, @Valid @RequestBody CompanyRequestDTO dto) {
        Company company = companyService.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Empresa nao encontrada"
                        )
                );

            Company updated = companyService.updateCompany(company, dto);

        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>("Empresa atualizada com sucesso!", CompanyMapper.toResponseDTO(updated)));
    }

    // End point para deletar uma empesa
    // Vamos devolver 204 No Content - sem corpo, status para delete sucesso
    // DELETE LOCALHOST:8080/api/id
    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir empresa")
    public ResponseEntity<String> deleteCompany(@PathVariable Long id) {
        // Sem o FindById no controller, ID inexistente vai gerar 500 Internal Server Error por conta do Ille da service, nao 404
        companyService.deleteCompany(id);
        return ResponseEntity.status(HttpStatus.OK).body("Empresa deletada com sucesso!");
    }
}

