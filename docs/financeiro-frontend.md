# Financeiro e Relatorios - Frontend

Este documento resume a parte de frontend das funcionalidades financeiras e de relatorios ajustadas nesta entrega.

## Commits relacionados

- `5b0ae9c` - integracao funcionalidade de transacoes, categorias e recorrencia
- `53a16b3` - integracao funcionalidade de relatorios
- `49f77ab` - adicionando icone na aba do navegador

## O que foi implementado

A area financeira do Angular passou a usar dados reais do backend para movimentacoes, categorias, recorrencias, relatorios e empresa atual.

Os arquivos principais ficam em:

```text
frontend/parceiro_auto_front/src/app/pages/transactions
frontend/parceiro_auto_front/src/app/pages/reports
frontend/parceiro_auto_front/src/app/pages/companies/company-categories
frontend/parceiro_auto_front/src/app/services/current-company.service.ts
```

## Movimentacoes

A tela de movimentacoes foi integrada com a API pelo service:

```text
frontend/parceiro_auto_front/src/app/pages/transactions/transaction.service.ts
```

As chamadas agora estao separadas por responsabilidade:

```text
TransactionService -> /api/transactions
TransactionCategoryService -> /api/transaction-categories
RecurrenceRuleService -> /api/recurrence-rules
```

Como o backend retorna `ApiResponse<T>`, os metodos usam `map(response => response.dados)` para entregar os dados diretamente aos componentes.

## Formulario de movimentacao

O formulario permite cadastrar e editar movimentacoes reais, informando:

- empresa;
- categoria;
- conta bancaria;
- tipo de movimentacao;
- descricao;
- valor;
- data;
- forma de pagamento;
- recorrencia opcional.

A categoria pode ser criada dentro do proprio formulario por um modal aberto no botao `+` ao lado do campo de categoria.

No modal de criacao rapida, o tipo da categoria acompanha o tipo da movimentacao selecionada. Assim, se a movimentacao for de entrada, a categoria criada ja nasce como entrada; se for saida, nasce como saida.

## Categorias por empresa

Foi criada uma tela para gerenciar categorias da empresa:

```text
frontend/parceiro_auto_front/src/app/pages/companies/company-categories
```

Nessa tela o usuario consegue:

- listar categorias de entrada e saida;
- criar categoria;
- editar categoria;
- excluir categoria.

A exclusao chama o backend para inativar a categoria, sem apagar fisicamente o registro.

## Empresa atual

Foi criado o service:

```text
frontend/parceiro_auto_front/src/app/services/current-company.service.ts
```

Ele guarda a empresa atual escolhida pelo usuario e e usado por telas como dashboard, movimentacoes e relatorios.

A selecao aparece na sidebar. Com isso, o usuario nao precisa escolher a empresa toda vez que for lancar ou consultar movimentacoes.

## Recorrencias

O formulario de movimentacao possui a opcao `Movimentacao recorrente`.

Quando marcada, o usuario escolhe a frequencia:

- diaria;
- semanal;
- mensal;
- anual.

O campo `Repetir ate` e opcional. Quando fica vazio, a regra fica sem data final. A API consulta as proximas datas previstas, mas nao gera novos lancamentos automaticamente.

A tela de movimentacoes tambem possui uma area de gerenciamento de recorrencias. Nela o usuario pode:

- ver as recorrencias da empresa selecionada;
- consultar a proxima data prevista;
- editar frequencia e data final em modal;
- encerrar a recorrencia sem apagar a movimentacao original.

## Dashboard

A dashboard foi ajustada para usar a empresa atual e dados reais do backend.

Tambem foi adicionado um bloco para mostrar as proximas tres movimentacoes recorrentes da empresa selecionada.

## Relatorios

A tela de relatorios foi integrada ao backend e deixou de usar dados locais.

Os arquivos principais ficam em:

```text
frontend/parceiro_auto_front/src/app/pages/reports/reports.ts
frontend/parceiro_auto_front/src/app/pages/reports/reports.html
frontend/parceiro_auto_front/src/app/pages/reports/reports.scss
```

A tela carrega movimentacoes, empresas, contas bancarias e categorias pela API.

## Filtros do relatorio

O relatorio permite filtrar por:

- empresa;
- tipo;
- categoria;
- conta bancaria;
- data inicial;
- data final;
- busca por texto.

A tabela exibida na tela usa exatamente o mesmo conjunto de dados que sera exportado.

## Exportacao XLS

A exportacao foi mantida no frontend.

O usuario clica em `Exportar XLS`, abre um modal e escolhe quais colunas quer exportar.

As colunas disponiveis sao:

- data;
- descricao;
- empresa;
- categoria;
- conta;
- tipo;
- forma de pagamento;
- valor;
- recorrencia.

O arquivo e gerado como `.xls` a partir de uma tabela HTML, formato que pode ser aberto pelo Excel ou LibreOffice. Essa abordagem evita instalar biblioteca extra apenas para exportacao.

## Menu lateral

O item `Relatorios` foi ativado no menu lateral.

O item `Gestao de acessos` foi removido do projeto para ficar fora desta entrega e voltar em uma proxima etapa.

Tambem foram corrigidos textos com caracteres quebrados no menu, como `Contas bancarias`, `Visao geral`, `Relatorios` e `Usuario`.

## Favicon

Foi adicionado um favicon com apenas o simbolo da marca Parceiro Auto, sem o texto lateral.

O arquivo fica em:

```text
frontend/parceiro_auto_front/public/favicon.svg
```

O `index.html` foi atualizado para usar esse SVG e o titulo da aba foi ajustado para `Parceiro Auto`.

## Testes e verificacoes

Foram executados:

```text
npm test -- --watch=false
npm run build
```

Os testes do front foram ajustados para mockar services que chamam a API, evitando chamadas reais durante `ng test`.

## Observacao sobre localStorage

Relatorios, movimentacoes, categorias e recorrencias nao usam `localStorage` para dados de negocio. Essas telas consomem dados reais do backend.

O uso restante de `localStorage` fica em areas especificas como autenticacao e selecao da empresa atual.
