# Arquitetura do backend

O projeto usa camadas: controller recebe HTTP, service aplica regras, repository acessa o banco e mapper prepara a resposta. Os controllers nao precisam repetir o tratamento de excecoes que ja existe no `GlobalExceptionHandler`.

## Organizacao dos DTOs

Dentro de `controller/dto`, os requests e responses ficam separados por assunto:

```text
dto/
  ApiResponse.java
  user/
  company/
  bankaccount/
  transaction/
  category/
  recurrence/
  mapper/
```

`ApiResponse` continua compartilhado. A mudanca de packages nao altera nomes de campos, URLs nem o JSON usado pelo front.

## Senhas

`PasswordConfig` fornece um `PasswordEncoder` do Spring Security Crypto. O cadastro usa `encode` para guardar um hash PBKDF2-HMAC-SHA256, com salt aleatorio de 16 bytes e 600 mil iteracoes. O login usa `matches` para conferir a senha recebida. Nao existe comparacao com senha em texto puro.

O salt faz duas senhas iguais produzirem hashes diferentes. Hash nao e criptografia reversivel: o sistema verifica a senha sem recuperar a original. O prefixo `{pbkdf2}` identifica o algoritmo, e o campo do banco aceita 255 caracteres. O hash nao aparece no DTO de resposta nem no `toString` de `User`.

O banco agora inicia do zero com duas migrations SQL em `src/main/resources/db/migration`: V1 cria toda a estrutura, incluindo `password varchar(255)`, e V2 insere os dados de demonstracao. Nao existe conversao de senhas antigas: os usuarios devem ser cadastrados novamente pela API, que ja gera o hash. Bancos com o historico anterior precisam ser recriados antes de iniciar esta versao.

O modulo de hash nao implementa sessao, token ou autorizacao das rotas. Essas funcionalidades continuam sendo uma etapa separada.

Referencia: [PasswordEncoder e PBKDF2 no Spring Security](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html).

## Categorias e Swagger

O GET de categorias apenas consulta, inclusive quando todas estao inativas. As dez categorias padrao sao criadas junto com uma nova empresa, na mesma transacao. A V2 insere essas categorias para a empresa de demonstracao, junto com uma conta de saldo zero, sem criar usuarios ou senhas fixas.

No Swagger, cadastro de categoria documenta `201`; inativacao de categoria e encerramento de recorrencia documentam `204` sem corpo.

## Transacoes e consultas

`TransactionApplicationService` coordena movimentacao, saldo e recorrencia. Seu `@Transactional` garante rollback conjunto quando alguma etapa falha. `TransactionService` mantem as regras de saldo e de vinculos entre empresa, conta e categoria.

As listagens de movimentacoes consultam as recorrencias em lote, evitando uma busca separada para cada item. A consulta das ultimas movimentacoes continua aplicando limite no banco. Categorias e recorrencias registram logs nas alteracoes; comentarios explicam regras e efeitos importantes sem repetir cada linha do codigo.

## Pontos que ainda precisam evoluir

- Saldo: ainda falta controle explicito de concorrencia para duas alteracoes simultaneas na mesma conta.
- Seguranca HTTP: o hash protege o armazenamento, mas nao restringe acesso aos dados de cada empresa.
- Consultas grandes: algumas listagens ainda nao possuem paginacao, e as proximas recorrencias sao calculadas em memoria.
- Algumas consultas ainda dependem de relacionamentos lazy acessados com Open Session in View. Desabilitar isso exige mover as leituras e o mapeamento para transacoes de leitura e validar os fluxos.
- Regras completas do cliente e comportamento do frontend precisam de validacao propria; esta revisao cobre o backend.

## Como verificar

Execute `./mvnw.cmd test` na pasta do backend. Ha testes de hash/login, categorias inativas, consulta em lote, Swagger, validacoes, Feign e rollback. Para incluir criacao do esquema e carga inicial em PostgreSQL, informe um banco separado conforme o [guia de testes](swagger-validacao.md).
