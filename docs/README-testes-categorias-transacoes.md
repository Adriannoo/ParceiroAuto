# Testes de categorias e movimentações

Este documento descreve os testes criados para `TransactionCategoryService`, `TransactionCategoryController` e `TransactionService`, seguindo o padrão Arrange/Act/Assert usado nos demais testes do projeto.

## Organização e isolamento

- `TransactionCategoryServiceTest` é unitário: Mockito simula o repositório de categorias. Os testes verificam resultados, alterações nas entidades e chamadas ao repositório, sem subir Spring ou usar banco.
- `TransactionCategoryControllerTest` usa `@WebMvcTest` e `MockMvc` para verificar o contrato HTTP do controller. Os serviços são mocks; as respostas e status HTTP são conferidos com asserções JSON.
- `TransactionServiceTest` é unitário: Mockito simula o repositório de movimentações. As contas, empresas, categorias e movimentações são objetos simples em memória para verificar efeitos nos saldos.

## Cenários cobertos

### `TransactionCategoryServiceTest`

- Lista apenas categorias ativas e, no método de histórico, inclui categorias inativas.
- Verifica que os dez nomes e tipos padrão são criados para a empresa e marcados como ativos.
- Busca categoria existente, rejeita ID nulo e converte ID inexistente em `ResourceNotFoundException`.
- Cria categoria nova com nome normalizado, reativa categoria inativa de mesmo nome e tipo, e rejeita conflito quando o mesmo nome já está associado a outro tipo.
- Rejeita empresa, nome e tipo inválidos antes de acessar o repositório.
- Inativa categoria pertencente à empresa e bloqueia categoria de outra empresa.
- Atualiza nome e tipo, reativa a categoria, permite manter o próprio nome e rejeita duplicidade, nome vazio, tipo nulo ou categoria de outra empresa.

### `TransactionCategoryControllerTest`

- Lista categorias ativas por padrão e inclui inativas apenas com `includeInactive=true`.
- Cria categoria e verifica status `201`, mensagem e dados retornados.
- Retorna `400` para corpo inválido antes de chamar serviços.
- Retorna `404` quando o usuário não tem acesso à empresa e confirma que o serviço de categoria não é chamado.
- Inativa categoria com `204 No Content` e verifica acesso à empresa antes da operação.
- Atualiza categoria com `200 OK`, verifica os dados atualizados e cobre erro de regra de negócio com `400`.

### `TransactionServiceTest`

- Verifica criação de entrada e saída, normalização da descrição, data informada ou atual, e atualização correta do saldo.
- Verifica primeira movimentação quando o saldo da conta é nulo.
- Rejeita campos obrigatórios inválidos, valor nulo/zero, relações com outra empresa e categoria inativa, sem salvar nem alterar saldo.
- Rejeita categoria cujo tipo não corresponde ao tipo da movimentação.
- Testa busca por ID, ID nulo ou inexistente, listagens gerais, por usuário, conta e empresa, além da validação de empresa e limite para últimas movimentações.
- Atualiza movimentação, desfaz o saldo anterior antes de aplicar o novo efeito, preserva a data no overload sem data e rejeita categoria incompatível sem alterar saldos.
- Rejeita atualização/exclusão com movimentação nula e verifica a reversão de saldo ao excluir entradas e saídas.

## Executar

No PowerShell, com o terminal na pasta `backend/parceiro_auto_back`, rode somente as três classes deste README:

```powershell
mvn "-Dtest=TransactionCategoryServiceTest,TransactionCategoryControllerTest,TransactionServiceTest" test
```

Para executar toda a suíte e gerar JaCoCo:

```powershell
mvn clean verify
```

O relatório HTML é gerado em `target/site/jacoco/index.html`. O `verify` aplica a meta de 90% de linhas para o projeto todo; enquanto a meta não for atingida, Maven pode terminar com `BUILD FAILURE` mesmo que todos os testes tenham passado.

## Resultado desta execução

As três classes focadas executaram **66 testes, sem falhas ou erros**. A suíte completa executou **223 testes, sem falhas ou erros; 4 foram ignorados** porque dependem de configuração externa de PostgreSQL.

Cobertura de linhas do JaCoCo após a suíte completa:

| Classe | Cobertura |
|---|---:|
| `TransactionCategoryService` | 100% (72/72 linhas) |
| `TransactionCategoryController` | 100% (20/20 linhas) |
| `TransactionService` | 100% (118/118 linhas) |
| Back-end completo | 81,34% (789/970 linhas) |

O limite global de 90% ainda não foi alcançado: faltam cobrir ao menos **84 linhas**. O relatório atual mostra que o maior próximo bloco é `BankAccountService` (107 linhas descobertas), seguido por `BankAccountController` (35). As classes solicitadas neste trabalho estão totalmente cobertas por linhas.

Os scripts de SonarQube e JMeter do projeto não substituem esses testes: o Sonar requer servidor e credencial local, e o JMeter verifica a API em execução por HTTP. Para validar as classes solicitadas, `mvn test` e a suíte Maven completa são a verificação apropriada.
