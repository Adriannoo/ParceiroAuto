# Testes da funcionalidade de empresas

Este documento explica os testes automatizados da funcionalidade de empresas no ParceiroAuto e como executá-los.

## Como os testes foram organizados

Os cenários seguem a mesma estrutura dos exercícios: **Arrange, Act, Assert** (preparar, executar e verificar). Primeiro os dados e dependências são preparados; depois o método ou endpoint é chamado; por fim, o resultado e os efeitos esperados são verificados com asserções significativas.

Nos testes unitários, Mockito substitui repositórios e serviços externos por mocks. Assim, cada teste de regra de negócio avalia a classe escolhida sem depender do PostgreSQL ou de uma chamada real à BrasilAPI. As verificações `verify` também conferem que ações como salvar, associar o proprietário e criar categorias padrão só acontecem nos cenários corretos.

Para as outras camadas, o mesmo padrão Arrange/Act/Assert é mantido com o recorte apropriado do Spring: `@WebMvcTest` testa o contrato HTTP do controller e `@DataJpaTest` testa consultas reais do repositório em H2.

## O que cada classe verifica

- `CompanyServiceTest`: criação de empresa com normalização de CNPJ e textos, criação do vínculo de proprietário e categorias padrão, CNPJ ausente/inválido/duplicado, validação de campos, atualização, prevenção de CNPJ duplicado, consultas por ID/CNPJ/usuário, exclusão e consulta à BrasilAPI. A consulta externa verifica o mapeamento da resposta e traduz CNPJ inexistente, falha de conexão, erro externo e resposta vazia em status HTTP apropriados.
- `CompanyAccessServiceTest`: leitura do usuário da sessão, rejeição de sessão ausente ou inválida, autorização e bloqueio de acesso a empresa, e associação do usuário como proprietário.
- `CompanyRepositoryTest`: consultas JPA que listam e localizam empresas pelo vínculo com o usuário. O cenário cria dois usuários e duas empresas para demonstrar que cada pessoa encontra apenas a própria empresa.
- `CompanyControllerTest`: respostas HTTP para criação, listagem, busca por ID e CNPJ, atualização, exclusão e consulta externa; também verifica validação `400`, conflito de CNPJ `409` e acesso negado tratado como `404`.

O projeto já tinha testes de integração relacionados a empresas. `UserAuthenticationTest` valida o fluxo real de criação e isolamento de empresas entre usuários. `BrasilApiIntegrationTest` valida a integração HTTP com a BrasilAPI usando um servidor local simulado, incluindo CNPJ não encontrado, falha externa, timeout e resposta vazia. Esses testes existentes foram mantidos.

`ApiDocumentationAndValidationTest` tambem foi incluido porque valida que os endpoints de criacao e atualizacao de empresas rejeitam corpos invalidos com `400 Bad Request`. A validacao do seed de Gustavo em `PostgresPersistenceTest` depende de um PostgreSQL de teste configurado pela propriedade `test.postgres.url`; sem essa configuracao, o teste e marcado para pular e nao usa o banco local do projeto.

## Executar os testes

No PowerShell, a partir da pasta do back-end:

```powershell
cd backend/parceiro_auto_back
.\mvnw.cmd "-Dtest=CompanyServiceTest,CompanyAccessServiceTest,CompanyRepositoryTest,CompanyControllerTest,UserAuthenticationTest,BrasilApiIntegrationTest,ApiDocumentationAndValidationTest" test
```

Para executar somente os testes unitários do serviço de empresa:

```powershell
.\mvnw.cmd -Dtest=CompanyServiceTest test
```

O relatório individual do Maven fica em `backend/parceiro_auto_back/target/surefire-reports/`.

Para executar a suíte completa e gerar o relatório JaCoCo, use `mvn clean verify` na pasta `backend/parceiro_auto_back`. O relatório HTML será gerado em `target/site/jacoco/index.html` e o XML em `target/site/jacoco/jacoco.xml`.

Na validação desta etapa, a suíte completa executou **113 testes, sem falhas ou erros; 4 foram ignorados** por dependerem de configuração externa. As classes de serviço de empresa, controle de acesso e controller tiveram, respectivamente, **97,22%, 100% e 100% de cobertura de linhas**. A cobertura global do back-end ficou em **62,47%**, pois inclui as demais funcionalidades ainda não cobertas nesta etapa.

## Limites desta etapa

Esta etapa cobre a funcionalidade de empresas e seus caminhos principais e de erro. A execução dessas classes, sozinha, não comprova a meta global de 90% de cobertura nem o Quality Gate do SonarQube. Para demonstrar essas métricas, é necessário executar o JaCoCo e analisar o projeto completo no SonarQube depois de concluir os testes das demais funcionalidades.
