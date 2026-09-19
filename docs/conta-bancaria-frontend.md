# Conta Bancaria - Frontend

Este documento resume a parte de frontend da funcionalidade de contas bancarias.

## Commits relacionados

- `fbe8253` - integracao da funcionalidade de contas bancarias
- `14fa7ad` - correcao da definicao de conta padrao
- `60c21c8` - validacao de dados nos cadastros de conta bancaria e empresa

## O que foi implementado

A funcionalidade de contas bancarias foi integrada no Angular com telas de listagem e formulario.

Os arquivos principais ficam em:

```text
frontend/parceiro_auto_front/src/app/pages/bank-accounts
```

## Service

O arquivo principal de integracao e:

```text
frontend/parceiro_auto_front/src/app/pages/bank-accounts/bank-account.service.ts
```

Ele usa `HttpClient` para chamar:

```text
http://localhost:8080/api/bank-accounts
```

Como o backend retorna `ApiResponse<T>`, os metodos usam `map(response => response.dados)`.

## Metodos integrados

| Metodo no front | Endpoint chamado | Funcao |
| --- | --- | --- |
| `listByCompany(companyId)` | `GET /api/bank-accounts/company/{companyId}` | Listar contas da empresa |
| `findById(companyId, id)` | `GET /api/bank-accounts/company/{companyId}/accounts/{id}` | Buscar conta para edicao |
| `create(bankAccount)` | `POST /api/bank-accounts/company/{companyId}` | Cadastrar conta |
| `update(id, bankAccount)` | `PUT /api/bank-accounts/company/{companyId}/accounts/{id}` | Atualizar conta |
| `delete(id, companyId)` | `DELETE /api/bank-accounts/company/{companyId}/accounts/{id}` | Excluir conta |
| `setDefault(id, companyId)` | `PATCH /api/bank-accounts/company/{companyId}/accounts/{id}/default` | Definir conta padrao |

## Listagem

A listagem permite selecionar uma empresa, carregar as contas dela, filtrar por banco, agencia, numero e tipo de conta, editar, excluir e definir uma conta como padrao.

Quando o usuario define uma conta como padrao, o front chama o endpoint `PATCH` e atualiza a lista na tela para marcar apenas aquela conta como padrao.

## Formulario

O formulario permite cadastrar e editar:

- empresa vinculada;
- banco;
- agencia;
- numero da conta;
- tipo da conta.

A definicao de conta padrao nao fica no formulario. Ela e feita apenas pela listagem de contas bancarias, para manter o fluxo mais simples.

## Uso em transacoes

O formulario de transacoes ja usa as contas bancarias reais da empresa selecionada para preencher o campo de conta.

Mesmo assim, a funcionalidade de transacoes ainda nao foi totalmente integrada ao backend. Ela ainda possui service proprio com `localStorage`.

## Observacao sobre localStorage

A funcionalidade de contas bancarias nao usa `localStorage`. A busca feita no projeto encontrou `localStorage` apenas em autenticacao e transacoes.

