# Swagger e validação de entrada

## Verificacoes depois do merge

`ApiDocumentationAndValidationTest` tambem verifica JSON malformado, enums e parametros
invalidos (400), rotas inexistentes (404), metodos incorretos (405) e respostas de
conflito de banco (409) sem detalhes de SQL.

`TransactionAtomicityTest` usa H2 com persistencia real e simula falhas depois da escrita.
Ele verifica rollback de criacao, atualizacao e exclusao, incluindo saldo e recorrencia,
alem do fluxo completo de sucesso. O `TransactionApplicationService` coordena essas
operacoes com `@Transactional`. Esse teste nao substitui a verificacao com PostgreSQL.

## Acessar a documentação

Com o back iniciado, abra http://localhost:8080/swagger-ui/index.html.
O contrato OpenAPI em JSON fica em http://localhost:8080/v3/api-docs.

O projeto usa Spring Boot 4.1.0 e springdoc 3.1.1. A compatibilidade é
documentada em https://springdoc.org/. O springdoc lê controllers e DTOs
para gerar o contrato; o Swagger UI permite consultar e executar os endpoints.

## O papel das anotações

- `@NotNull`: o campo não pode ser nulo.
- `@NotBlank`: o texto não pode ser nulo, vazio ou conter apenas espaços.
- `@Positive`: o número deve ser maior que zero; combinar com `@NotNull`
  quando também for obrigatório.
- `@Size`: limita o tamanho do texto.
- `@Digits`: limita a quantidade de dígitos inteiros e casas decimais.
- `@Valid`: solicita a validação do DTO recebido em `@RequestBody`.
- `@Validated`: permite selecionar um grupo de validação.
- `@Tag`: organiza endpoints no Swagger.
- `@Operation`: descreve a operação.
- `@Schema`: descreve campos e exemplos; não substitui validação.

Em movimentações, o grupo `TransactionRequestDTO.Create` herda `Default`:
a criação valida tanto as restrições comuns como o `companyId` obrigatório.
A atualização usa `@Valid`, valida as restrições comuns e mantém a empresa
original. Se `companyId` for enviado na atualização, ele não altera a empresa.

A data continua opcional: na criação assume a data atual; na atualização
mantém a data original. O valor aceita até 17 dígitos inteiros e 2 casas
decimais, conforme a coluna `DECIMAL(19, 2)`.

As regras existentes de empresas e contas bancárias foram mantidas. O campo
`active` da empresa continua opcional, e `defaultAccount` da conta usa `false`
quando omitido. Login e senha agora são validados no cadastro e na autenticação.

## Experimentar uma requisição

No Swagger, abra um endpoint, clique em **Try it out**, preencha o corpo e
clique em **Execute**. As operações são reais e podem alterar o banco configurado.

Para testar a validação de movimentações, envie `{}` no POST `/api/transactions`:
o resultado deve ser HTTP 400 antes de acessar os services.

Exemplo de criação com formato válido:

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

Os IDs precisam existir e respeitar as regras de negócio; formato válido
não garante que o cadastro será aceito. As validações do service continuam
necessárias. O tratamento global de erros ainda não foi implementado: não há
um formato personalizado de erros por campo garantido por esta alteração.

## Verificação automatizada

Execute `mvnw.cmd test` no diretório do back, com um JDK configurado.
`ApiDocumentationAndValidationTest` usa H2 em memória, desabilita migrations
nesse contexto e substitui os services por mocks. Verifica HTTP 400 sem chamadas
aos services, regras de criação/atualização, precisão monetária, OpenAPI e Swagger UI.
Essa classe nao valida a persistencia; os testes abaixo cobrem os demais cenarios.

## Ajustes de contas, consultas e integracao externa

- A empresa da conta bancaria vem somente da URL. O JSON nao precisa de `companyId`.
- A agencia exige 4 digitos e o numero da conta aceita de 4 a 13 digitos.
- A consulta `/api/transactions/company/{companyId}/last?limit=2` aceita limite de
  1 a 100 e ordena por data decrescente, usando o ID para desempatar. O banco aplica o limite.
- A BrasilAPI e consultada por `BrasilApiClient`, com OpenFeign e Spring Cloud 2025.1.3.
  A compatibilidade com Boot 4.1 esta em https://spring.io/projects/spring-cloud/.
- `@EnableFeignClients` habilita os clientes; `@FeignClient` define o servico externo.
  A URL fica em `integrations.brasil-api.url`, com timeout de conexao de 3s e leitura de 5s.
- A consulta externa nao abre transacao de banco. Ausencia retorna 404, falha HTTP
  externa retorna 502 e falha de comunicacao retorna 503, com logs sem o corpo externo.

`TransactionServiceTest` verifica vinculos e limites. `BrasilApiIntegrationTest`
usa um servidor HTTP local para testar o Feign, incluindo sucesso, erro e timeout.
Nenhum desses testes depende da BrasilAPI real.

Para verificar PostgreSQL, informe um banco separado de testes:

```powershell
./mvnw.cmd test "-Dtest=PostgresPersistenceTest" "-Dtest.postgres.url=jdbc:postgresql://localhost:55439/postgres" "-Dtest.postgres.user=postgres"
```

Se houver senha, informe `-Dtest.postgres.password` no seu ambiente de testes.
O teste cria um schema exclusivo `test_*`, aplica V1, V2 e V3 e valida as entidades.
Tambem confere que uma nova chamada ao Flyway nao reaplica as migrations, verifica
ordenacao e limite, rejeita vinculos de outra empresa e verifica rollback do saldo.
O schema permanece no banco de teste para inspecao. Sem `test.postgres.url`, essa
classe e ignorada. Nao use o banco de trabalho para esse comando.

As migrations existentes foram preservadas. Dados de exemplo sao criados somente
pelos testes, sem popular o banco normal da aplicacao.
