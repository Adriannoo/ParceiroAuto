# Swagger e validação de entrada

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
Esses testes não validam a integração com PostgreSQL nem executam suas migrations.
