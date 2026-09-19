# Conta Bancaria - Backend

Este documento resume a parte de backend da funcionalidade de contas bancarias.

## Commits relacionados

- `fbe8253` - integracao da funcionalidade de contas bancarias
- `14fa7ad` - correcao da definicao de conta padrao
- `60c21c8` - validacao de dados nos cadastros de conta bancaria e empresa
- `e9d9c89` - Swagger, OpenAPI, validacao de DTOs e ajustes de README

## O que foi implementado

A funcionalidade de contas bancarias foi exposta pela API em `api/bank-accounts`.

Os arquivos principais ficam em:

```text
backend/parceiro_auto_back/src/main/java/br/edu/uniamerica/parceiro_auto/controller/BankAccountController.java
backend/parceiro_auto_back/src/main/java/br/edu/uniamerica/parceiro_auto/service/BankAccountService.java
```

Cada conta bancaria pertence a uma empresa. Por isso, os endpoints usam o `companyId` no caminho.

## Endpoints

| Metodo | Endpoint | Funcao |
| --- | --- | --- |
| `POST` | `/api/bank-accounts/company/{companyId}` | Cadastrar conta bancaria para uma empresa |
| `GET` | `/api/bank-accounts/company/{companyId}` | Listar contas de uma empresa |
| `GET` | `/api/bank-accounts/company/{companyId}/default` | Buscar a conta padrao da empresa |
| `GET` | `/api/bank-accounts/company/{companyId}/accounts/{id}` | Buscar uma conta especifica da empresa |
| `PUT` | `/api/bank-accounts/company/{companyId}/accounts/{id}` | Atualizar conta bancaria |
| `PATCH` | `/api/bank-accounts/company/{companyId}/accounts/{id}/default` | Definir conta como padrao |
| `DELETE` | `/api/bank-accounts/company/{companyId}/accounts/{id}` | Excluir conta bancaria |

## Regra da conta padrao

A conta padrao e definida pelo endpoint `PATCH`.

Quando uma conta e marcada como padrao, o service remove a marcacao das outras contas da mesma empresa e salva a conta selecionada como padrao.

Essa regra fica centralizada no backend para evitar que o front precise decidir quais contas desmarcar.

## Validacoes

As principais validacoes sao:

- a empresa precisa existir;
- a conta precisa pertencer a empresa informada;
- banco, agencia, numero da conta e tipo sao obrigatorios;
- agencia deve ter 4 digitos;
- numero da conta deve ter entre 4 e 13 digitos;
- nao pode existir outra conta com mesmo banco, agencia e numero para a mesma empresa.

## Observacao sobre localStorage

A funcionalidade de contas bancarias nao usa `localStorage`. Os dados sao persistidos no banco pelo backend.

