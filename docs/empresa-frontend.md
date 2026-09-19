# Empresa - Frontend

Este documento resume a parte de frontend da funcionalidade de empresas.

## Commits relacionados

- `65442b5` - funcionalidade company integrada
- `a487e03` - buscar dados da empresa pelo CNPJ
- `60c21c8` - validacao de dados nos cadastros de conta bancaria e empresa

## O que foi implementado

A funcionalidade de empresas foi integrada com o backend pelo Angular, removendo o uso de dados locais para cadastrar, listar, editar e excluir empresas.

Os arquivos principais ficam em:

```text
frontend/parceiro_auto_front/src/app/pages/companies
```

## Service

O arquivo principal de integracao e:

```text
frontend/parceiro_auto_front/src/app/pages/companies/company.service.ts
```

Ele usa `HttpClient` para chamar:

```text
http://localhost:8080/api/companies
```

Como o backend retorna `ApiResponse<T>`, todos os metodos usam `map(response => response.dados)` para entregar apenas os dados para os componentes.

## Metodos integrados

| Metodo no front | Endpoint chamado | Funcao |
| --- | --- | --- |
| `list()` | `GET /api/companies` | Listar empresas |
| `findById(id)` | `GET /api/companies/{id}` | Buscar empresa para edicao |
| `create(company)` | `POST /api/companies` | Cadastrar empresa |
| `update(company)` | `PUT /api/companies/{id}` | Atualizar empresa |
| `delete(id)` | `DELETE /api/companies/{id}` | Excluir empresa |
| `findByCnpjInBrasilApi(cnpj)` | `GET /api/companies/lookup/{cnpj}` | Buscar dados pela BrasilAPI |

## Formulario

O formulario de empresa usa `ReactiveFormsModule`, validators e listas de opcoes para campos como regime tributario, porte da empresa, natureza juridica e estado.

Tambem existe o botao de busca por CNPJ. O usuario informa o CNPJ, o front limpa a mascara e chama o backend. Quando a resposta chega, o formulario e preenchido com os dados retornados.

## Listagem

A listagem carrega as empresas pela API, exibe os dados principais e permite filtrar, editar, excluir e acessar movimentacoes da empresa.

## Observacao sobre localStorage

A funcionalidade de empresas nao usa mais `localStorage`. A busca feita no projeto encontrou `localStorage` apenas em autenticacao e transacoes.

