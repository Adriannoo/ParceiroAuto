# ParceiroAuto

Projeto Integrador do semestre de Engenharia de Software: um sistema contábil com cadastro de empresas, usuários, contas bancárias e movimentações financeiras. A integração entre back e front está em andamento; algumas funcionalidades do front ainda utilizam dados locais.

## Tecnologias

- **Back:** Java 21, Spring Boot 4.1, Spring Web MVC, Spring Data JPA e Lombok.
- **Banco:** PostgreSQL e Flyway; H2 nos testes de validação e documentação.
- **Validação e documentação:** Jakarta Validation e springdoc 3.1.1 (OpenAPI / Swagger UI).
- **Front:** Angular 21, TypeScript e Bootstrap.

## Organização

- `backend/parceiro_auto_back`: API Spring Boot.
- `frontend/parceiro_auto_front`: aplicação Angular.

No back, `controller` recebe as requisições, `service` concentra as regras de negócio, `repository` acessa o banco e `entity` representa os dados persistidos. Os DTOs definem os dados de entrada e saída, e os mappers convertem entidades em DTOs de resposta.

## Como executar

**Back:** configure um JDK compatível com Java 21, mantenha o PostgreSQL disponível e ajuste a conexão em `backend/parceiro_auto_back/src/main/resources/application.properties` para seu ambiente. Recarregue o Maven no IntelliJ e execute `Projeto4periodoApplication`.

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
| `/api/companies` | Cadastro, listagem, busca por ID ou CNPJ, atualização e exclusão. |
| `/api/bank-accounts` | Cadastro, consulta por empresa, conta padrão, atualização e exclusão. |
| `/api/transactions` | Criação, consulta por empresa ou conta, atualização e exclusão. |

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

Na criação de movimentações, `@Validated(TransactionRequestDTO.Create.class)` aplica o grupo `Create`, que herda as regras comuns de `Default`. O `@NotNull(groups = Create.class)` exige `companyId` somente na criação. Na atualização, a empresa original é mantida.

As anotações do Swagger documentam o contrato. As de validação verificam a entrada antes do controller executar sua lógica; as regras de negócio continuam nos services.

## Testes

Na pasta do back, execute `./mvnw.cmd test` ou rode `ApiDocumentationAndValidationTest` pelo IntelliJ. Os testes verificam entradas inválidas, regras dos DTOs e disponibilidade do OpenAPI e Swagger UI, usando H2 em memória e services simulados. Não cobrem a integração completa com PostgreSQL.

Mais detalhes no [guia de Swagger e validação](backend/parceiro_auto_back/docs/swagger-validacao.md).
