# API - ParceiroAuto

A API conecta o front ao PostgreSQL e concentra os cadastros e as regras financeiras. Este guia descreve o backend atual; os campos completos de cada request e response podem ser consultados no Swagger.

## Executar e acessar

Configure o Java 21. Em `backend/parceiro_auto_back/src/main/resources`, copie `application.properties.example` para `application.properties` e ajuste a conexao com PostgreSQL. O arquivo local nao e versionado. Com o banco criado e disponivel, execute na pasta do back:

```powershell
./mvnw.cmd spring-boot:run
```

O Flyway aplica as migrations na inicializacao. Por padrao:

- API: `http://localhost:8080/api`.
- [Swagger UI](http://localhost:8080/swagger-ui/index.html): documentacao interativa.
- [OpenAPI JSON](http://localhost:8080/v3/api-docs): contrato gerado a partir do codigo.

No Swagger, escolha uma rota, clique em **Try it out**, preencha os parametros e use **Execute**. Confira o status e o corpo em **Response body**. Os cadastros, alteracoes e exclusoes executados ali afetam o banco configurado.

## Como a requisicao funciona

O controller recebe a URL e o JSON, valida o DTO e chama o service. O service aplica as regras de negocio e usa o repository para acessar o banco. Os mappers transformam as entidades em DTOs de resposta.

As respostas com `ApiResponse<T>` possuem `mensagem` e `dados`; `dados` pode ser um objeto ou uma lista. Cadastros retornam `201`, consultas e atualizacoes retornam `200`. Exclusoes de contas, movimentacoes, categorias e recorrencias retornam `204`, sem corpo. A exclusao de empresa atualmente retorna `200` com uma mensagem em texto.

## Rotas

Todas as rotas abaixo comecam com `/api`. Valores entre chaves devem ser substituidos pelo ID ou dado desejado.

| Recurso | Metodo e caminho | Funcao |
| --- | --- | --- |
| Usuarios | `POST /users` | Cadastrar usuario |
| Usuarios | `POST /users/login` | Conferir login e senha e retornar os dados do usuario |
| Usuarios | `GET /users/{id}` ou `GET /users?login=...` | Buscar usuario |
| Empresas | `POST /companies` | Cadastrar empresa |
| Empresas | `GET /companies` | Listar empresas |
| Empresas | `GET /companies/{id}` ou `GET /companies?cnpj=...` | Buscar empresa cadastrada |
| Empresas | `GET /companies/lookup/{cnpj}` | Consultar CNPJ na BrasilAPI, sem cadastrar a empresa |
| Empresas | `PUT /companies/{id}` / `DELETE /companies/{id}` | Atualizar ou excluir empresa |
| Contas | `POST /bank-accounts/company/{companyId}` | Criar conta vinculada a empresa da URL |
| Contas | `GET /bank-accounts/company/{companyId}` | Listar contas da empresa |
| Contas | `GET /bank-accounts/company/{companyId}/default` | Buscar conta padrao |
| Contas | `GET /bank-accounts/company/{companyId}/accounts/{id}` | Buscar conta da empresa |
| Contas | `PATCH /bank-accounts/company/{companyId}/accounts/{id}/default` | Definir conta padrao |
| Contas | `PUT` / `DELETE /bank-accounts/company/{companyId}/accounts/{id}` | Atualizar ou excluir conta |
| Movimentacoes | `POST /transactions` | Criar movimentacao |
| Movimentacoes | `GET /transactions` ou `GET /transactions/{id}` | Listar todas ou buscar por ID |
| Movimentacoes | `GET /transactions/company/{companyId}` | Listar por empresa |
| Movimentacoes | `GET /transactions/bank-account/{bankAccountId}` | Listar por conta |
| Movimentacoes | `GET /transactions/company/{companyId}/last?limit=5` | Buscar ultimas, com limite obrigatorio de 1 a 100 |
| Movimentacoes | `PUT /transactions/{id}` / `DELETE /transactions/{id}` | Atualizar ou excluir movimentacao |
| Categorias | `GET` / `POST /transaction-categories/company/{companyId}` | Listar ativas ou criar categoria |
| Categorias | `PUT` / `DELETE /transaction-categories/company/{companyId}/{categoryId}` | Atualizar ou inativar categoria |
| Recorrencias | `GET /recurrence-rules/company/{companyId}` | Listar regras com proxima data disponivel |
| Recorrencias | `GET /recurrence-rules/company/{companyId}/next?limit=3` | Listar proximas recorrencias; limite padrao 3 |
| Recorrencias | `PUT /recurrence-rules/{id}` / `DELETE /recurrence-rules/{id}` | Editar ou encerrar regra |

## Exemplo de movimentacao

Antes de testar, cadastre uma empresa, uma conta e uma categoria compativel com o tipo da movimentacao. Use os IDs retornados nos cadastros. Envie este JSON em `POST /api/transactions`, com `Content-Type: application/json`:

```json
{
  "companyId": 1,
  "bankAccountId": 1,
  "transactionCategoryId": 1,
  "type": "ENTRADA",
  "description": "Pagamento de servico",
  "value": 150.50,
  "method": "PIX"
}
```

- `companyId` e obrigatorio na criacao. No `PUT`, a empresa original e mantida.
- Conta e categoria devem pertencer a empresa. O tipo aceita `ENTRADA` ou `SAIDA`; o metodo aceita `PIX`, `CARTAO` ou `DINHEIRO`.
- O valor deve ser positivo, com ate duas casas decimais, e a descricao nao pode estar vazia.
- `date` usa `AAAA-MM-DD`. Se omitida, assume a data atual na criacao e preserva a original na atualizacao.
- Para recorrencia, envie `recurrenceFrequency`: `DAILY`, `WEEKLY`, `MONTHLY` ou `YEARLY`. `recurrenceEndDate` e opcional, exige frequencia e nao pode anteceder a data da movimentacao.
- Na atualizacao, omitir a frequencia remove a regra de recorrencia existente. As consultas de recorrencia calculam as proximas datas; esse fluxo nao cria novos lancamentos automaticamente.

O `TransactionApplicationService` coordena movimentacao, saldo e recorrencia em uma unica transacao: se uma etapa falhar, todas sao desfeitas. Inativar uma categoria preserva seu registro; encerrar uma recorrencia preserva a movimentacao original.

## Validacao e documentacao

| Anotacao | O que faz |
| --- | --- |
| `@RequestBody` | Converte o JSON recebido em DTO |
| `@PathVariable` / `@RequestParam` | Recebem valores do caminho ou da consulta da URL |
| `@NotNull` / `@NotBlank` | Exigem valor nao nulo ou texto preenchido |
| `@Positive` / `@Digits` | Validam valor positivo e quantidade de digitos |
| `@Valid` | Aciona as validacoes do DTO |
| `@Validated` | Seleciona grupos de validacao, como `Create` |
| `@Tag` / `@Operation` | Organizam e descrevem as operacoes no Swagger |
| `@Schema` | Acrescenta descricoes e exemplos dos campos |
| `@ApiResponse` do Swagger | Documenta uma resposta; nao muda o status retornado pelo codigo |
| `@FeignClient` | Declara o cliente HTTP usado na consulta externa |

Validar a entrada evita que dados invalidos avancem para a regra de negocio. `@NotNull` nao otimiza consultas ao banco. Na consulta de ultimas movimentacoes, o limite e aplicado no banco, com ordenacao por data e ID decrescentes.

A consulta de CNPJ usa OpenFeign, com timeout de conexao de 3 segundos e leitura de 5 segundos. Nesse fluxo, ausencia na BrasilAPI retorna `404`, falha HTTP externa retorna `502` e falha de comunicacao retorna `503`. O login invalido retorna `401`; validacoes de entrada e regras de recorrencia podem retornar `400`. O formato do erro nao deve ser presumido igual ao `ApiResponse` de sucesso.

Veja tambem o [guia de Swagger, validacao e testes](swagger-validacao.md) e os detalhes do [financeiro no backend](financeiro-backend.md).
