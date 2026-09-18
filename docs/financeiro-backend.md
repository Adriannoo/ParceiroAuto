# Financeiro - Backend

Este documento resume a parte de backend das funcionalidades financeiras ajustadas nesta entrega.

## Commits relacionados

- `5b0ae9c` - integracao funcionalidade de transacoes, categorias e recorrencia
- `93be258` - corrige ordem das migrations de recorrencia

## O que foi implementado

A area financeira passou a usar dados reais do backend para movimentacoes, categorias de movimentacao e regras de recorrencia.

Os arquivos principais ficam em:

```text
backend/parceiro_auto_back/src/main/java/br/edu/uniamerica/parceiro_auto/controller/TransactionController.java
backend/parceiro_auto_back/src/main/java/br/edu/uniamerica/parceiro_auto/controller/TransactionCategoryController.java
backend/parceiro_auto_back/src/main/java/br/edu/uniamerica/parceiro_auto/controller/RecurrenceRuleController.java
backend/parceiro_auto_back/src/main/java/br/edu/uniamerica/parceiro_auto/service/TransactionService.java
backend/parceiro_auto_back/src/main/java/br/edu/uniamerica/parceiro_auto/service/TransactionCategoryService.java
backend/parceiro_auto_back/src/main/java/br/edu/uniamerica/parceiro_auto/service/RecurrenceRuleService.java
```

As respostas seguem o padrao `ApiResponse<T>`, retornando a mensagem e o campo `dados`, igual nas outras funcionalidades integradas.

## Movimentacoes

As movimentacoes ficam expostas em `api/transactions`.

| Metodo | Endpoint | Funcao |
| --- | --- | --- |
| `POST` | `/api/transactions` | Criar movimentacao |
| `GET` | `/api/transactions` | Listar movimentacoes |
| `GET` | `/api/transactions/{id}` | Buscar movimentacao por ID |
| `GET` | `/api/transactions/company/{companyId}` | Listar movimentacoes de uma empresa |
| `GET` | `/api/transactions/company/{companyId}/last?limit=3` | Listar ultimas movimentacoes da empresa |
| `GET` | `/api/transactions/bank-account/{bankAccountId}` | Listar movimentacoes de uma conta bancaria |
| `PUT` | `/api/transactions/{id}` | Atualizar movimentacao |
| `DELETE` | `/api/transactions/{id}` | Excluir movimentacao |

A movimentacao possui vinculo com empresa, conta bancaria e categoria. O backend valida se os IDs informados existem e se pertencem a empresa correta.

## Categorias de movimentacao

As categorias ficam expostas em `api/transaction-categories`.

| Metodo | Endpoint | Funcao |
| --- | --- | --- |
| `GET` | `/api/transaction-categories/company/{companyId}` | Listar categorias ativas da empresa |
| `POST` | `/api/transaction-categories/company/{companyId}` | Criar categoria para uma empresa |
| `PUT` | `/api/transaction-categories/company/{companyId}/{categoryId}` | Editar categoria |
| `DELETE` | `/api/transaction-categories/company/{companyId}/{categoryId}` | Inativar categoria |

A exclusao da categoria e logica. O registro permanece no banco com `active = false`, evitando quebrar movimentacoes antigas que usam aquela categoria.

## Recorrencia

A recorrencia foi implementada por meio da tabela `recurrence_rule`, vinculada a uma movimentacao original.

Uma movimentacao recorrente possui:

- frequencia (`DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY`);
- data inicial;
- data final opcional;
- forma de pagamento;
- ultima execucao registrada.

Quando a data final fica vazia, a recorrencia e considerada sem prazo. Esse fluxo atende casos como aluguel, internet ou conta de luz mensal.

## Endpoints de recorrencia

| Metodo | Endpoint | Funcao |
| --- | --- | --- |
| `GET` | `/api/recurrence-rules/company/{companyId}/next?limit=3` | Listar proximas recorrencias da empresa |
| `GET` | `/api/recurrence-rules/company/{companyId}` | Listar recorrencias gerenciaveis da empresa |
| `PUT` | `/api/recurrence-rules/{id}` | Editar frequencia e data final da recorrencia |
| `DELETE` | `/api/recurrence-rules/{id}` | Encerrar recorrencia |

Encerrar a recorrencia remove apenas a regra de repeticao. A movimentacao original continua cadastrada.

## Migrations

As migrations foram reorganizadas para evitar duplicidade.

A migration duplicada `V3__convert_ids_to_bigint.sql` foi removida porque repetia a conversao para `bigint` que ja estava dentro da V2.

A migration de frequencia de recorrencia foi renomeada para manter a ordem simples:

```text
V1__create_tables.sql
V2__add_company_details.sql
V3__add_recurrence_frequency.sql
```

A V3 atual adiciona o campo `frequency` na tabela `recurrence_rule`:

```sql
ALTER TABLE recurrence_rule
    ADD COLUMN IF NOT EXISTS frequency varchar(20) NOT NULL DEFAULT 'MONTHLY';
```

## Validacoes

As validacoes principais ficam nos DTOs de request, usando Jakarta Validation.

Na movimentacao, os campos obrigatorios incluem empresa na criacao, conta bancaria, categoria, tipo, descricao, valor e metodo de pagamento.

Na recorrencia, a frequencia e obrigatoria ao editar uma regra. A data final pode ficar vazia para representar uma recorrencia sem prazo.

## Testes e verificacoes

Foi executado:

```text
mvn clean test
```

O backend tambem foi iniciado com PostgreSQL local para confirmar que o Flyway aplicava as migrations em ordem correta. O historico esperado ficou:

```text
1 - create tables
2 - add company details
3 - add recurrence frequency
```

## Observacao sobre localStorage

O backend nao usa `localStorage`. A persistencia fica no banco de dados via Spring Data JPA e Flyway controla a estrutura do banco.
