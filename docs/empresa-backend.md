# Empresa - Backend

Este documento resume a parte de backend da funcionalidade de empresas.

## Commits relacionados

- `65442b5` - funcionalidade company integrada
- `60c21c8` - validacao de dados nos cadastros de conta bancaria e empresa
- `a487e03` - buscar dados da empresa pelo CNPJ
- `e9d9c89` - Swagger, OpenAPI, validacao de DTOs e ajustes de README

## O que foi implementado

A funcionalidade de empresas foi exposta pela API em `api/companies`, usando controller, service, DTOs, mapper, repository e entity.

O controller principal fica em:

```text
backend/parceiro_auto_back/src/main/java/br/edu/uniamerica/parceiro_auto/controller/CompanyController.java
```

As respostas seguem o padrao `ApiResponse<T>`, que encapsula uma mensagem e o campo `dados`. Esse formato facilita o uso no front, porque o Angular sempre acessa `response.dados`.

## Endpoints

| Metodo | Endpoint | Funcao |
| --- | --- | --- |
| `POST` | `/api/companies` | Cadastrar empresa |
| `GET` | `/api/companies` | Listar empresas |
| `GET` | `/api/companies/{id}` | Buscar empresa por ID |
| `GET` | `/api/companies?cnpj=00000000000000` | Buscar empresa cadastrada pelo CNPJ |
| `GET` | `/api/companies/lookup/{cnpj}` | Buscar dados do CNPJ na BrasilAPI |
| `PUT` | `/api/companies/{id}` | Atualizar empresa |
| `DELETE` | `/api/companies/{id}` | Excluir empresa |

## Consulta de CNPJ

O endpoint `/api/companies/lookup/{cnpj}` consulta dados externos pela BrasilAPI e retorna um DTO proprio para preencher o formulario no front.

A ideia foi manter o front sem chamar a BrasilAPI diretamente. O Angular chama o backend, e o backend fica responsavel por conversar com o servico externo.

## Validacoes

As validacoes ficam nos DTOs de request, usando annotations do Jakarta Validation, como `@NotBlank`, `@NotNull`, `@Size`, `@Pattern` e outras conforme o campo.

Isso impede que dados invalidos cheguem diretamente na regra de negocio.

## Observacao sobre localStorage

A funcionalidade de empresas nao usa mais `localStorage`. O front chama diretamente a API do backend.

