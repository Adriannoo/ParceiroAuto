# Guia de estudo: testes e qualidade do ParceiroAuto

Este guia resume o que apresentar sobre os testes automatizados do back-end, como eles são organizados e o que mostram JaCoCo e SonarQube. Os resultados numéricos abaixo são os registrados na documentação da análise feita em 23/10/2026; podem mudar se o código, os testes ou a configuração forem alterados.

## Roteiro de apresentação (cerca de 5 minutos)

### 1. Objetivo dos testes

Os testes verificam se as regras de negócio funcionam nos cenários válidos e inválidos. Eles não apenas chamam os métodos: conferem retornos, alterações de saldo e estado, exceções, chamadas a dependências e respostas HTTP.

### 2. Como organizamos os testes

- **Testes unitários de serviços:** JUnit e Mockito. Os repositórios e dependências são simulados para testar a regra de negócio sem banco de dados.
- **Testes de controller:** `@WebMvcTest` e `MockMvc` exercitam as rotas e conferem status HTTP e conteúdo JSON, com os serviços simulados.
- **Testes de repositório:** `@DataJpaTest` verifica consultas e persistência JPA em um contexto menor que a aplicação inteira.
- **Testes de integração:** usados quando precisamos verificar a aplicação ou o PostgreSQL real, por exemplo migrations e rollback.
- **Padrão AAA:** Arrange prepara dados e mocks; Act executa a operação; Assert verifica o resultado. Os nomes dos testes descrevem o cenário esperado.

### 3. Exemplo: categorias e movimentações

Nos testes de `TransactionCategoryService`, verificamos criação, atualização, reativação e inativação de categorias, além de nomes duplicados, dados inválidos e acesso à empresa errada.

Em `TransactionCategoryController`, verificamos respostas como `201 Created`, `200 OK`, `204 No Content`, `400 Bad Request` e `404 Not Found`.

Em `TransactionService`, verificamos entradas e saídas, impacto no saldo, edição e exclusão de movimentações, além de valores inválidos, categoria inativa ou incompatível e relações pertencentes a outra empresa.

Assim, cobrimos caminhos de sucesso e caminhos de erro, observando também se operações inválidas deixam o estado sem alteração.

### 4. JaCoCo e SonarQube

- **JaCoCo** mede quais linhas, instruções e decisões foram executadas pelos testes. O relatório pode ser consultado em `backend/parceiro_auto_back/target/site/jacoco/index.html`.
- **SonarQube** analisa estaticamente o código para encontrar problemas como bugs, vulnerabilidades, code smells e duplicação. Ele importa o XML de cobertura gerado pelo JaCoCo; não substitui a execução dos testes.
- O **Quality Gate** é a regra de aprovação da análise. No projeto configuramos cobertura mínima de 90%, duplicação máxima de 2%, zero bugs, vulnerabilidades e code smells, e manutenibilidade A.

Na análise documentada, o JaCoCo marcou 100% de cobertura de linhas, 99,89% de instruções e 97,99% de decisões. O Sonar mostrou 99,5% de cobertura combinada, duplicação de 0%, nenhum bug, vulnerabilidade ou code smell, manutenibilidade A e Quality Gate aprovado. As porcentagens de JaCoCo e Sonar podem diferir porque são métricas calculadas de formas diferentes.

### 5. Fechamento

Os testes dão evidência de que os comportamentos escolhidos estão corretos e que os casos inválidos são tratados. JaCoCo mostra a extensão do código exercitada; Sonar ajuda a avaliar a qualidade estática e aplicar os critérios do gate. A cobertura alta, sozinha, não prova que um teste é bom: por isso usamos asserções que verificam resultados e efeitos reais.

## Como executar

No PowerShell, a partir da raiz do repositório:

```powershell
cd backend/parceiro_auto_back
./mvnw.cmd clean verify
```

O comando executa a suíte e gera os relatórios JaCoCo. O `verify` também aplica as metas configuradas no Maven; se alguma meta ainda não for atingida, o build pode terminar com falha mesmo que os testes tenham passado.

Para executar somente as três classes de categorias e movimentações:

```powershell
./mvnw.cmd "-Dtest=TransactionCategoryServiceTest,TransactionCategoryControllerTest,TransactionServiceTest" test
```

Para analisar no Sonar, primeiro gere um relatório atualizado com os testes. Com o servidor iniciado e as credenciais configuradas, execute o scanner Maven na pasta do backend:

```powershell
./mvnw.cmd org.sonarsource.scanner.maven:sonar-maven-plugin:sonar "-Dsonar.host.url=http://localhost:9000" "-Dsonar.token=$env:SONAR_TOKEN"
```

O comando envia a análise Maven para o servidor em `http://localhost:9000`. O servidor é iniciado separadamente; o scanner Maven é quem analisa o código.

## Perguntas que podem surgir

### Qual a diferença entre cobertura de linhas e branches/decisões?

Cobertura de linhas mede se as linhas executáveis foram alcançadas. Cobertura de decisões mede se cada resultado de uma condição foi exercitado, como verdadeiro e falso em um `if`. Uma linha pode ser executada sem que todos os caminhos daquela condição tenham sido testados.

### O que significa um teste com mock?

Um mock substitui uma dependência, como um repositório, por um objeto controlado pelo teste. Isso permite testar a regra do serviço sem depender de banco ou de serviços externos. O teste pode conferir tanto o resultado quanto se a dependência foi chamada corretamente.

### Por que não usar `@SpringBootTest` em todos os testes?

Ele sobe o contexto completo e tende a deixar a suíte mais lenta. Testes unitários e slices como `@WebMvcTest` e `@DataJpaTest` isolam a camada necessária. O contexto completo fica para cenários que realmente precisam integrar várias partes.

### O Sonar executa os testes?

Não. O Maven/JUnit executa os testes, o JaCoCo mede a cobertura e gera o XML, e o SonarQube analisa o código e importa esse relatório.

### Como explicar 100% no JaCoCo e 99,5% no Sonar?

São métricas calculadas de maneira diferente. O JaCoCo apresenta métricas separadas de linhas, instruções e decisões. O Sonar combina linhas cobertas e condições cobertas na métrica de cobertura. Por isso os percentuais não precisam ser iguais.

## Arquivos úteis para demonstrar

- `docs/README-testes-empresa.md`: cenários de teste da empresa.
- `docs/README-testes-categorias-transacoes.md`: cenários das categorias e movimentações.
- `docs/testes-qualidade.md`: comandos, configuração de JaCoCo, SonarQube e JMeter, além dos resultados documentados.
- `backend/parceiro_auto_back/src/test/java`: classes de teste do back-end.
- `backend/parceiro_auto_back/target/site/jacoco/index.html`: relatório HTML do JaCoCo após executar a suíte.

