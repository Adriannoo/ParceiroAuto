# Alterações da branch `fix/funcionalidades-gerais`

Este documento registra as mudanças encontradas na branch e o resultado das verificações feitas em 21/10/2026.

## Autenticação e cadastro

- O login e o cadastro passaram a usar a API do backend, em vez do CRUD/localStorage simulado no navegador.
- O backend cria usuários com senha protegida por PBKDF2, confere a senha no login e mantém a sessão HTTP no cookie.
- Foram adicionados os endpoints de sessão `GET /api/users/me` e `POST /api/users/logout`; as rotas da API exigem uma sessão autenticada, exceto cadastro e login.
- O frontend verifica a sessão ao abrir, envia credenciais de cookie para a API e limpa os dados do mock legado do navegador.
- Depois de criar uma conta, o usuário é encaminhado ao formulário para cadastrar a primeira empresa.
- O rodapé com credenciais de demonstração foi removido da tela de login.

## Empresas e dados financeiros

- A migration V1 inclui o campo de nome do usuário.
- A migration V2 cria o usuário de demonstração Gustavo (`gustavo@empresa.com`, senha `123456`) e popula uma oficina com uma empresa, duas contas bancárias, dez categorias, sete movimentações e uma recorrência de aluguel.
- A empresa de demonstração é vinculada ao Gustavo como proprietário na tabela `user_company`; as rotas filtram empresas e dados financeiros pelo usuário da sessão.
- Cadastros feitos pela interface não criam automaticamente a empresa de exemplo; começam pelo formulário de empresa.
- O frontend centraliza a empresa selecionada e adapta painel, relatórios, listagens e formulários para exibir e consultar os dados da empresa atual.
- Categorias inativas continuam disponíveis nas consultas de histórico para que as movimentações antigas preservem o nome da categoria. As listas de seleção de novas movimentações usam categorias ativas.
- O cliente da BrasilAPI, o CORS local e o exemplo de configuração do backend foram ajustados para o fluxo da aplicação.

## Migrations e banco de dados

O Flyway tem duas migrations versionadas: `V1__create_tables.sql` e `V2__insert_initial_data.sql`. Como a V1 e a V2 foram alteradas durante esta branch, o banco de desenvolvimento precisa ser recriado antes de iniciar esta versão; bancos com histórico anterior podem falhar na validação de checksum do Flyway. O V2 insere dados uma vez, quando a migration é aplicada a um banco vazio.

## Verificações executadas

- Backend: `cmd.exe /d /c mvnw.cmd test` — **BUILD SUCCESS**, 60 testes aprovados, zero falhas/erros e quatro testes ignorados (64 casos contabilizados). Inclui verificação de que um usuário não lista nem acessa empresa de outra conta.
- Frontend: `npm test -- --watch=false` — **40 testes aprovados em 12 arquivos**.
- Frontend: `npm run build` — build concluído.
- `git diff --check` — sem erros de whitespace. O Git apenas avisou sobre conversão de finais de linha LF/CRLF em arquivos do workspace.

## Pendências e limites conhecidos

- Os quatro testes ignorados são os testes que precisam de PostgreSQL. Eles só rodam quando se informa `test.postgres.url` apontando para um banco/esquema separado. Portanto, a migration V2 ainda não foi exercitada por esses testes nesta revisão; recriar o banco de desenvolvimento executará a migration pelo Flyway.
- O build Angular avisa que o bundle inicial excede o limite configurado em cerca de 200,53 kB e que `sweetalert2` é CommonJS. O build terminou com sucesso; são avisos de otimização.
- Usuários só conseguem listar, consultar ou alterar empresas às quais estão associados. Empresas criadas durante a sessão são associadas ao criador como proprietário. As rotas de contas, categorias, movimentações e recorrências validam a empresa associada antes de retornar ou alterar dados.

## Como iniciar

1. Recrie o banco PostgreSQL de desenvolvimento, conforme o procedimento do [README principal](../README.md).
2. Configure `application.properties` usando `application.properties.example` e confirme URL, usuário e senha do PostgreSQL.
3. Inicie o backend na pasta `backend/parceiro_auto_back` com `./mvnw.cmd spring-boot:run`. O Flyway cria a estrutura e aplica os dados da V2.
4. Inicie o frontend na pasta `frontend/parceiro_auto_front` com `npm start` e abra `http://localhost:4200`.
5. Para a demonstração, entre com `gustavo@empresa.com` e senha `123456`.
