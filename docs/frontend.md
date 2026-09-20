# Frontend - ParceiroAuto

Aplicacao do Projeto Integrador feita com Angular 21, TypeScript, Bootstrap, RxJS e SweetAlert2. Permite gerenciar empresas, contas, categorias, movimentacoes e regras de recorrencia, alem de consultar indicadores e relatorios.

## Como executar

Com Node.js e npm compativeis com Angular 21, entre em `frontend/parceiro_auto_front`:

```sh
npm ci
npm start
```

Acesse `http://localhost:4200`. Para as telas de negocio, mantenha o backend em `http://localhost:8080` e o PostgreSQL configurado conforme o README principal. Use `localhost`, pois a origem precisa ser aceita pelo CORS da API.

O login do frontend e simulado no `AuthService`, com usuarios e sessao no navegador. O acesso de demonstracao e `gustavo@empresa.com`, senha `123456`. Esse login nao chama a autenticacao do backend; a avaliacao permite mock.

## Organizacao

Os caminhos abaixo ficam dentro de `src/app`:

| Local | Responsabilidade |
|---|---|
| `components` | Login, cadastro e layout com menu lateral. |
| `pages` | Telas organizadas por funcionalidade. Cada area pode ter seu service e model. |
| `models` | Tipos compartilhados, como usuario e `ApiResponse<T>`. |
| `services` | Autenticacao simulada e selecao da empresa atual. |
| `guards` | Verificacao da sessao antes de entrar nas telas internas. |
| `app.routes.ts` | Rotas, incluindo `children` para empresas, contas e movimentacoes. |
| `app.config.ts` | Configuracao do Router, HttpClient e demais providers. |

O fluxo principal e: **tela -> service Angular -> API -> service Angular -> tela**. O componente controla formulario e exibicao; o service faz as chamadas HTTP; as interfaces definem o formato dos dados.

## Services e API

| Service | Rota base / funcao |
|---|---|
| `CompanyService` | `/api/companies`: empresas e consulta de CNPJ. |
| `BankAccountService` | `/api/bank-accounts`: contas e definicao da conta padrao. |
| `TransactionService` | `/api/transactions`: lancamentos. |
| `TransactionCategoryService` | `/api/transaction-categories`: categorias. |
| `RecurrenceRuleService` | `/api/recurrence-rules`: consulta, edicao e encerramento de recorrencias. |
| `AuthService` | Login e cadastro simulados no navegador. |
| `CurrentCompanyService` | Guarda a empresa selecionada usando signal e localStorage. |

Os tres services financeiros ficam em `pages/transactions`. Categorias e recorrencias foram retiradas do `TransactionService` para cada service atender um controller. Os componentes de dashboard, relatorios, categorias da empresa, lancamentos da empresa, formulario e lista de movimentacoes foram ajustados.

As respostas usam `{ mensagem, dados }`. O trecho `map(response => response.dados)` extrai os dados antes de entrega-los ao componente. O `subscribe` recebe o resultado em `next` e a falha em `error`.

## Metodos dos services financeiros

| Service | Metodos e finalidade |
|---|---|
| `TransactionService` | `list` lista lancamentos; `listByCompany` filtra por empresa; `findById` busca um registro; `create`, `update` e `delete` cadastram, alteram e excluem. |
| `TransactionCategoryService` | `listCategoriesByCompany` consulta categorias ativas; `createCategory` cria; `updateCategory` altera; `deleteCategory` solicita a inativacao na API. |
| `RecurrenceRuleService` | `listNextRecurringByCompany` consulta as proximas recorrencias com limite; `listRecurringByCompany` lista as regras da empresa; `updateRecurrence` altera frequencia e data final; `deleteRecurrence` encerra a regra. |

Os metodos e endpoints foram preservados na separacao. As regras de recorrencia consultam datas previstas, mas nao criam novos lancamentos automaticamente.

## Modais e SweetAlert2

Os modais Bootstrap foram mantidos. SweetAlert2 aparece somente no resultado da exclusao de empresas, em `company-list.ts`:

1. `openConfirmation(company)` guarda a empresa e abre o modal existente.
2. `closeConfirmation()` fecha o modal, exceto durante a requisicao.
3. `confirmDeletion()` carrega SweetAlert2 com `import()`, bloqueia novas tentativas e chama o DELETE.
4. No sucesso, fecha o modal, recarrega a lista e mostra o alerta. Na falha, libera o botao, mantem o modal e informa o erro.

O signal `deleting` controla o bloqueio dos botoes e o texto `Excluindo...`. A biblioteca so e carregada quando essa operacao e utilizada.

## Recursos do Angular usados

| Recurso | O que faz |
|---|---|
| `@Component` | Liga a classe ao HTML, estilos e imports da tela. |
| `@Injectable({ providedIn: 'root' })` | Disponibiliza o service para injecao na aplicacao. |
| `inject()` | Obtem uma dependencia, como um service ou HttpClient. |
| `signal()` / `computed()` | Guardam estado reativo e calculam valores derivados. |
| `HttpClient` / `Observable` | Enviam requisicoes e representam seus resultados assincronos. |
| `FormBuilder` / `Validators` | Montam formularios e validam os campos. |
| `@if` / `@for` | Exibem conteudo condicional e listas no template. |
| `children` | Organiza rotas filhas dentro de uma rota principal. |

## Estilos e relatorios

O Bootstrap e carregado uma unica vez pelo `angular.json`. A importacao repetida no `styles.scss` foi removida. Na verificacao realizada, o CSS de producao passou de aproximadamente 383 KB para 231 KB.

Relatorios usam dados da API e filtros no navegador. A exportacao gera uma tabela HTML com extensao `.xls`; nao e um arquivo XLSX nativo. Os modais de escolha de colunas continuam disponiveis.

## Como validar

Na pasta do frontend:

```sh
npm run build
npm test -- --watch=false
```

Na ultima validacao desta branch, a compilacao passou e os 15 testes passaram. Foram adicionados testes dos contratos HTTP dos services separados e da exclusao de empresas (cancelamento, sucesso, erro e bloqueio de requisicao duplicada). Os testes de dashboard e relatorios receberam mocks dos novos services.

No navegador, com backend local, foram conferidos login, cadastro de empresa e conta, conta padrao, criacao e edicao de categoria, criacao e edicao de lancamento, edicao e encerramento de recorrencia, consulta de relatorios, acionamento da exportacao e alertas de exclusao. Os registros temporarios foram removidos. Isso nao significa cobertura de todos os cenarios possiveis.

## Situacao para entrega

- Angular 21 e Bootstrap foram mantidos conforme orientacao aceita para o projeto.
- Modais, SweetAlert2, login, services, interfaces e rotas filhas estao implementados.
- Algumas consultas e exclusoes ainda precisam de mensagens de erro mais claras.
- Categorias inativas continuam vinculadas e podem impedir a exclusao de uma empresa no backend. O alerta informa a falha; a causa nao foi corrigida nesta etapa.
- A compilacao ainda avisa sobre tamanho do pacote inicial e formato CommonJS do SweetAlert2.
- Conferir contribuicoes reais dos integrantes, publicar a versao final e entregar ZIP e link do repositorio.

Detalhes por funcionalidade: [empresas](empresa-frontend.md), [contas bancarias](conta-bancaria-frontend.md) e [financeiro e relatorios](financeiro-frontend.md).
