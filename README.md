# ParceiroAuto

Projeto Integrador do semestre de Engenharia de Software: um sistema contábil com cadastro de empresas, usuários, contas bancárias, categorias e movimentações financeiras, incluindo regras de recorrência. A integração entre back e front está em andamento.

## Tecnologias

- **Back:** Java 21, Spring Boot 4.1, Spring Web MVC, Spring Data JPA e Lombok.
- **Banco:** PostgreSQL e Flyway; H2 nos testes de validação e documentação.
- **Validação e documentação:** Jakarta Validation e springdoc 3.1.1 (OpenAPI / Swagger UI).
- **Integração externa:** Spring Cloud OpenFeign para consulta de CNPJ na BrasilAPI.
- **Front:** Angular 21, TypeScript e Bootstrap.

## Organização

- `backend/parceiro_auto_back`: API Spring Boot.
- `frontend/parceiro_auto_front`: aplicação Angular.
- `docs`: guias da API e das funcionalidades do projeto.

No back, `controller` recebe as requisições, `service` concentra as regras de negócio, `repository` acessa o banco e `entity` representa os dados persistidos. Os DTOs definem os dados de entrada e saída, e os mappers convertem entidades em DTOs de resposta.

## Como executar

**Back:** configure um JDK compatível com Java 21 e mantenha o PostgreSQL disponível. Copie `application.properties.example` para `application.properties` em `backend/parceiro_auto_back/src/main/resources` e ajuste a conexão para seu ambiente. O arquivo local não é versionado. Recarregue o Maven no IntelliJ e execute `Projeto4periodoApplication`.

Com `JAVA_HOME` configurado, também é possível iniciar pelo terminal, na pasta do back:

```powershell
./mvnw.cmd spring-boot:run
```

As migrations ficam em `src/main/resources/db/migration` e são executadas pelo Flyway na inicialização. A API usa `http://localhost:8080` por padrão.

**Front:** com Node.js e npm compatíveis com Angular 21, execute na pasta `frontend/parceiro_auto_front`:

```sh
npm install
npm start
```

Acesse `http://localhost:4200`.

## Swagger e API

Com o back rodando:

- [Swagger UI](http://localhost:8080/swagger-ui/index.html): consulta e execução dos endpoints.
- [OpenAPI JSON](http://localhost:8080/v3/api-docs): contrato gerado da API.

Escolha uma operação, clique em **Try it out**, preencha os dados e use **Execute**. As requisições são reais e podem alterar o banco.

| Rota base | Operações disponíveis |
|---|---|
| `/api/users` | Cadastro, autenticação em `/login` e busca por ID ou login. |
| `/api/companies` | Cadastro, listagem, busca por ID ou CNPJ, consulta externa em `/lookup/{cnpj}`, atualização e exclusão. |
| `/api/bank-accounts` | Cadastro, consulta por empresa, conta padrão, atualização e exclusão. |
| `/api/transactions` | Criação, consulta por empresa ou conta, últimas movimentações com limite, atualização e exclusão. |
| `/api/transaction-categories` | Cadastro, listagem de categorias ativas, atualização e inativação. |
| `/api/recurrence-rules` | Consulta das próximas recorrências, edição e encerramento das regras. |

O [guia da API](docs/api.md) reúne as rotas, exemplos e regras de uso do backend.

Exemplo de corpo para `POST /api/transactions`:

```json
{
  "companyId": 1,
  "bankAccountId": 1,
  "transactionCategoryId": 1,
  "type": "ENTRADA",
  "description": "Pagamento de serviço",
  "value": 150.50,
  "method": "PIX"
}
```

Os IDs precisam existir no banco. A data é opcional: na criação assume a data atual; na atualização mantém a original. As respostas com `ApiResponse<T>` usam os campos `mensagem` e `dados`.

Conta e categoria devem pertencer à empresa informada. Movimentações atualizam o saldo da conta; categorias são inativadas sem apagar o histórico. As regras de recorrência permitem consultar próximas datas, mas não geram novos lançamentos automaticamente.

## Anotações usadas

| Anotação | O que faz |
|---|---|
| `@RestController` | Define a classe que atende requisições e devolve os dados da API. |
| `@RequestMapping` | Define a rota base do controller. |
| `@PostMapping` | Liga um método a uma requisição POST, usada nos cadastros e no login. |
| `@GetMapping` | Define uma operação de consulta. |
| `@PutMapping` / `@PatchMapping` | Definem operações de atualização. |
| `@DeleteMapping` | Define uma operação de exclusão. |
| `@RequestBody` | Converte o JSON recebido no DTO de entrada. |
| `@PathVariable` / `@RequestParam` | Recebem valores do caminho ou dos parâmetros da URL. |
| `@NotNull` | Exige um valor diferente de nulo. |
| `@NotBlank` | Exige texto que não seja vazio nem apenas espaços. |
| `@Positive` | Exige número maior que zero; não substitui `@NotNull`. |
| `@Size` / `@Digits` | Limitam tamanho do texto ou quantidade de dígitos do número. |
| `@Email` / `@Pattern` | Verificam o formato do e-mail ou um padrão definido. |
| `@Valid` | Aciona a validação das restrições do DTO recebido. |
| `@Validated` | Permite selecionar um grupo de validação, como o de criação. |
| `@Configuration` | Identifica uma classe de configuração do Spring. |
| `@OpenAPIDefinition` / `@Info` | Definem título, versão e descrição da documentação. |
| `@Tag` | Agrupa os endpoints no Swagger. |
| `@Operation` | Acrescenta o resumo da operação. |
| `@Schema` | Acrescenta descrições, exemplos e informações dos campos. |
| `@ApiResponse` (Swagger) | Documenta o status e a resposta esperada; não altera o retorno real. |
| `@EnableFeignClients` / `@FeignClient` | Habilitam e declaram clientes HTTP, como o da BrasilAPI. |

Na criação de movimentações, `@Validated(TransactionRequestDTO.Create.class)` aplica o grupo `Create`, que herda as regras comuns de `Default`. O `@NotNull(groups = Create.class)` exige `companyId` somente na criação. Na atualização, a empresa original é mantida.

As anotações do Swagger documentam o contrato. As de validação verificam a entrada antes do controller executar sua lógica; as regras de negócio continuam nos services.

## Testes

Na pasta do back, execute `./mvnw.cmd test`. Os testes cobrem validações e documentação com H2 e services simulados, regras de movimentações e integração Feign com um servidor HTTP local, sem depender da BrasilAPI real.

`PostgresPersistenceTest` verifica migrations, consultas e rollback em PostgreSQL. Essa classe só executa quando `test.postgres.url` é informado para um banco de testes separado; sem essa configuração, ela é ignorada.

Mais detalhes no [guia de Swagger e validação](docs/swagger-validacao.md).
