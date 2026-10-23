# Testes e qualidade do backend

## Para que serve cada ferramenta

- **JUnit e Mockito:** executam testes e isolam dependencias, como repositories e clientes HTTP.
- **JaCoCo 0.8.15:** mede o codigo executado pelos testes. O Maven exige pelo menos 90% das linhas no `verify`.
- **SonarQube Community 26.10.0.133078:** analisa problemas, duplicacao e importa a cobertura do JaCoCo. Scanner Maven: 5.8.0.7211.
- **JMeter 5.6.3:** envia requisicoes HTTP e registra tempos e erros. Nao substitui testes unitarios e nao aumenta automaticamente a cobertura do JaCoCo.

## Medicao inicial

Executada em 22/10/2026 na branch `test/implementando-testes`, base `b70dce5`, com as configuracoes deste guia ainda sem commit.

| Criterio | Resultado / proximo passo |
|---|---|
| Cobertura minima de 90% | **Pendente:** 606 de 970 linhas, ou 62,47%, no JaCoCo. Condicoes: 46,55%. |
| Duplicacao maxima de 2% | **Atendido nesta analise:** 0%. |
| Zero bugs e vulnerabilidades | **Atendido nesta analise:** zero alertas de ambos. |
| Nota A e zero code smells | **Atendido nesta analise:** A e zero smells. |
| Assercoes relevantes | Existem verificacoes de retorno, excecoes e interacoes nos testes de empresas. Revisar os novos testes com esse mesmo cuidado. |
| Test slices | Ja existem `CompanyControllerTest` com `@WebMvcTest` e `CompanyRepositoryTest` com `@DataJpaTest`. Manter esse padrao para outras camadas. |
| Mocks | Os testes unitarios de empresas isolam repository, acesso e BrasilAPI com Mockito. |
| Caminhos tristes | Existem cenarios de validacao, acesso e dados inexistentes. Ainda precisamos ampliar para contas, recorrencias e lancamentos. |
| Nomes e AAA | Os testes de empresas usam nomes de cenario em ingles e Arrange/Act/Assert. Aplicar aos proximos testes. |
| Participacao | Depende da equipe: cada integrante deve implementar e explicar seus cenarios. Ferramentas nao comprovam dominio. |

JUnit: **113 testes contabilizados, 109 executados sem falhas e 4 ignorados**. Os quatro testes de PostgreSQL dependem de configuracao explicita e ainda precisam ser executados em banco de teste dedicado; H2 nao comprova comportamento de PostgreSQL/Flyway.

Sonar usou o perfil padrao **Sonar way core**, com 274 regras Java ativas, analisando 66 arquivos Java de producao e 12 de teste. Zero alertas se refere a essa analise e a esse perfil, nao garante ausencia absoluta de defeitos. Nenhuma exclusao de cobertura ou supressao de alerta foi adicionada.

O Quality Gate **ParceiroAuto - Entrega** avalia o codigo completo: cobertura >= 90%, duplicacao <= 2%, bugs/vulnerabilidades/smells = 0 e manutenibilidade A. O Sonar combina linhas e condicoes na metrica `coverage`: resultado **58,3%**, diferente dos 62,47% de linhas do JaCoCo. Por isso o gate do Sonar e mais exigente que apenas 90% de linhas. Atualmente ambos reprovam pela cobertura.

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

## Ordem sugerida para continuar

1. `BankAccountService`: 107 linhas ainda nao cobertas. Testar criacao, saldo, conta padrao, propriedade da empresa e dados inexistentes usando Mockito.
2. `RecurrenceRuleService`: 53 linhas pendentes. Testar datas, encerramento e regras invalidas.
3. `TransactionCategoryService`: 40 linhas pendentes. Testar criacao, atualizacao e inativacao.
4. Controllers de contas, lancamentos e recorrencias com `@WebMvcTest`, cobrindo sucesso, 400, 401, 403 e 404 conforme cada contrato.
5. Completar regras de lancamentos/usuarios e executar os testes de PostgreSQL em banco separado.

Em cada teste: **Arrange** prepara dados e mocks; **Act** chama a operacao; **Assert** verifica resultado, estado ou excecao. Uma chamada ao metodo sem verificar nada nao basta. Recalcule a cobertura a cada etapa e explique os cenarios para a equipe.
