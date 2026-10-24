# Testes e qualidade do backend

## Para que serve cada ferramenta

- **JUnit e Mockito:** executam testes e isolam dependencias, como repositories e clientes HTTP.
- **JaCoCo 0.8.15:** mede o codigo executado pelos testes. O Maven exige pelo menos 90% de linhas, instrucoes e decisoes no total e em cada classe analisada.
- **SonarQube Community 26.10.0.133078:** analisa problemas, duplicacao e importa a cobertura do JaCoCo. Scanner Maven: 5.8.0.7211.
- **JMeter 5.6.3:** envia requisicoes HTTP e registra tempos e erros. Nao substitui testes unitarios e nao aumenta automaticamente a cobertura do JaCoCo.

## Resultados atuais

Validacao executada em 23/10/2026 na branch `test/implementando-testes`, com as alteracoes locais desta entrega. Depois de alterar codigo ou testes, execute a suite e a analise novamente.

| Criterio | Resultado / proximo passo |
|---|---|
| Cobertura minima de 90% | **Atendido:** 965 de 965 linhas (100%), 99,89% das instrucoes e 97,99% das decisoes no JaCoCo. Nenhuma classe analisada ficou abaixo de 90%. |
| Duplicacao maxima de 2% | **Atendido nesta analise:** 0%. |
| Zero bugs e vulnerabilidades | **Atendido nesta analise:** zero alertas de ambos. |
| Nota A e zero code smells | **Atendido nesta analise:** A e zero smells. |
| Assercoes relevantes | Os testes verificam dados retornados, saldo, campos persistidos, excecoes e ausencia de escrita quando a entrada e invalida. |
| Test slices | Controllers usam `@WebMvcTest`; consultas usam `@DataJpaTest`. Os contextos completos ficam nos testes de integracao que precisam deles. |
| Mocks | Os services sao testados com repositories e dependencias simulados, sem banco real. Os testes de integracao verificam a persistencia separadamente. |
| Caminhos tristes | Ha testes de DTOs invalidos, ausencia de sessao, acesso a outra empresa, registros inexistentes, duplicacao e falhas de integracao/rollback. |
| Nomes e AAA | Os novos testes tem nomes de cenario em ingles e blocos Arrange/Act/Assert. Excecoes usam Act & Assert para capturar a falha. |
| Participacao | Depende da equipe: cada integrante deve implementar e explicar seus cenarios. Ferramentas nao comprovam dominio. |

JUnit: **329 testes executados, zero falhas, zero erros e nenhum ignorado**, incluindo PostgreSQL. A validacao usou um banco PostgreSQL temporario separado, removido depois. As migrations V1/V2, os dados demonstrativos e o rollback foram conferidos. O teste limpa apenas seus proprios registros entre cenarios, preservando a demonstracao e evitando dependencia da ordem.

Sonar usou o perfil padrao **Sonar way core**. Resultado: **Quality Gate aprovado, cobertura combinada 99,5%, duplicacao 0%, bugs 0, vulnerabilidades 0, code smells 0 e manutenibilidade A**. Os resultados se referem a essa analise e a esse perfil. Nenhuma exclusao de cobertura ou supressao de alerta foi adicionada.

O Quality Gate **ParceiroAuto - Entrega** avalia o codigo completo: cobertura >= 90%, duplicacao <= 2%, bugs/vulnerabilidades/smells = 0 e manutenibilidade A. O Sonar combina linhas e condicoes na metrica `coverage`, por isso mostra 99,5% enquanto a cobertura de linhas e 100%.

Os mappers possuem construtor privado porque so oferecem metodos estaticos e nao devem ser instanciados. O JaCoCo filtra automaticamente construtores privados vazios e codigo gerado pelo Lombok. Os metodos de conversao sao executados e verificados nos testes; nao ha configuracao para excluir classes.

## Executar os testes e ver a cobertura

Configure `JAVA_HOME` para seu JDK e execute na raiz do repositorio:

```powershell
$env:JAVA_HOME = 'C:\caminho\do\jdk-21-ou-25'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
cd backend/parceiro_auto_back
./mvnw.cmd clean verify
```

O comando executa os testes, gera `target/site/jacoco/index.html` e `jacoco.xml` e verifica a meta. Enquanto a cobertura estiver abaixo de 90%, terminar com erro e o comportamento esperado. O relatorio HTML continua disponivel. Para rodar so os testes de uma classe durante o desenvolvimento:

```powershell
./mvnw.cmd -Dtest=CompanyServiceTest test
```

Antes de analisar no Sonar, execute novamente `clean verify` para nao importar um relatorio antigo. Nao use exclusoes ou testes sem assercoes para subir a porcentagem.

### Incluir PostgreSQL na verificacao

Na execucao padrao, os quatro testes de PostgreSQL sao ignorados de proposito: eles precisam de um banco separado. Para repetir a verificacao completa, crie um banco vazio de testes, como `parceiro_auto_test`, e execute na pasta do backend:

```powershell
$env:TEST_POSTGRES_PASSWORD = 'senha-do-seu-banco-de-testes'
./mvnw.cmd clean verify '-Dtest.postgres.url=jdbc:postgresql://localhost:5432/parceiro_auto_test' '-Dtest.postgres.user=postgres'
Remove-Item Env:TEST_POSTGRES_PASSWORD
```

Substitua os exemplos pela sua conexao de testes. O teste cria um schema com nome aleatorio nesse banco e aplica as migrations nele. Nao use o banco da aplicacao. A senha vai pelo ambiente para nao aparecer nos argumentos do Maven; nao a coloque em arquivos versionados. H2 continua sendo suficiente para os testes unitarios e slices locais.

## SonarQube local

Nesta maquina as ferramentas ja foram extraidas em `.tools/`, que fica fora do Git. Para outro integrante, baixe o ZIP do [SonarQube Community](https://www.sonarsource.com/products/sonarqube/downloads/) na mesma versao e extraia em `.tools/sonarqube-26.10.0.133078`. O servidor usa JDK 21 ou 25; o banco H2 interno serve apenas para estudo local.

No arquivo local `.tools/sonarqube-26.10.0.133078/conf/sonar.properties`, configure:

```properties
sonar.web.host=127.0.0.1
sonar.web.port=9000
sonar.search.host=127.0.0.1
```

Na raiz do repositorio, em um terminal:

```powershell
./scripts/qualidade/iniciar-sonar.ps1 -JavaHome $env:JAVA_HOME
```

Espere o servidor iniciar e, em outro terminal:

```powershell
./scripts/qualidade/configurar-sonar.ps1
./scripts/qualidade/analisar-sonar.ps1 -JavaHome $env:JAVA_HOME
```

`configurar-sonar.ps1` prepara uma instalacao local nova: troca a senha inicial do admin, cria o projeto privado, configura o gate e gera um token de analise. Pode ser executado novamente nessa mesma instalacao. Se voce ja tem outro Sonar configurado, nao use esse inicializador sobre ele.

A senha e o token ficam em `.quality-local/` protegidos pelo Windows para o usuario atual. Nao compartilhe nem versione esses arquivos. Para consultar a senha de acesso ao painel no seu proprio terminal:

```powershell
(Import-Clixml .quality-local/sonar-admin.xml).GetNetworkCredential().Password
```

Acesse [o painel do backend](http://127.0.0.1:9000/dashboard?id=parceiroauto-backend) com usuario `admin` e a senha local. A analise aguarda o gate e retorna erro se ele reprovar. Isso nao significa que a ferramenta deixou de funcionar: veja as condicoes no painel.

Para encerrar uma instancia iniciada pelo script, pressione Ctrl+C no terminal do servidor. A primeira instancia instalada nesta maquina foi iniciada em segundo plano: encerre apenas o processo Java cuja linha de comando contem `sonar-application-26.10.0.133078.jar` pelo Gerenciador de Tarefas. Nao encerre outros processos Java da aplicacao.

## JMeter

Para outra maquina, baixe o ZIP binario do [Apache JMeter](https://jmeter.apache.org/download_jmeter.cgi), versao 5.6.3, confira o SHA-512 publicado e extraia em `.tools/apache-jmeter-5.6.3`. O arquivo `src/test/jmeter/smoke-api.jmx` do backend pode ser aberto pela interface `bin/jmeter.bat` para estudar o plano.

Com a API local rodando na porta 8080, execute da raiz:

```powershell
./scripts/qualidade/testar-jmeter.ps1 -JavaHome $env:JAVA_HOME
```

O plano usa 2 usuarios, 2 repeticoes e duas requisicoes por repeticao: **8 requisicoes**. Verifica status 200 e conteudo OpenAPI em `/v3/api-docs`; depois verifica status 401 e mensagem de login em `/api/companies` sem sessao. Um 401 e sucesso neste cenario porque o objetivo e confirmar o bloqueio. Um 200 inesperado nessa rota reprova.

O script confere as assercoes no CSV, pois so o codigo de saida do JMeter nao garante que todas as requisicoes passaram. Os resultados ficam em `backend/parceiro_auto_back/target/jmeter/<execucao>/html/index.html`.

Validacao inicial: **8 requisicoes, 0 falhas**, em API temporaria na porta 18080 com H2 em memoria, sem Flyway e sem alterar PostgreSQL. Isso valida o plano inicial, nao comprova capacidade, consultas autenticadas ou todos os fluxos. Ainda precisamos combinar volume e tempo de resposta esperado, incluir login/cookies e testar consultas com dados de teste. Nao usar dados de producao.

## O que estudar nos testes

| Arquivo | O que demonstra |
|---|---|
| `BankAccountServiceTest` | Mockito, validacoes, conta duplicada e mudanca de conta padrao. |
| `BankAccountControllerTest` / `UserControllerTest` | Requisicoes com MockMvc, validacao de JSON, sessao e erros HTTP. |
| `UserServiceValidationTest` | Entradas invalidas e interrupcao antes de consultar ou gravar dados. |
| `TransactionApplicationServiceTest` | Coordenacao entre services e validacao antes de alterar a movimentacao. |
| `FinancialRepositoryTest` | Consultas JPA, isolamento por empresa, ordenacao, limite e busca em lote. |
| `CorsConfigTest` / `CnpjValidatorTest` | Casos validos e invalidos de origem HTTP e digitos verificadores. |
| `TransactionAtomicityTest` / `PostgresPersistenceTest` | Rollback real e migrations; exigem integracao e nao devem ser substituidos apenas por mocks. |

Os testes de autenticacao iniciam pelo `main` real usando H2. Isso confere a entrada da aplicacao sem adicionar outro contexto completo apenas para testar o metodo principal.

Em cada teste: **Arrange** prepara dados e mocks; **Act** chama a operacao; **Assert** verifica resultado, estado ou excecao. Uma chamada ao metodo sem verificar nada nao basta. Recalcule a cobertura a cada etapa e explique os cenarios para a equipe.
