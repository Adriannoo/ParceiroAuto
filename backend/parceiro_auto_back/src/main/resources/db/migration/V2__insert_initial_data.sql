-- Usuario de exemplo para demonstrar o login; a senha esta protegida com PBKDF2.
INSERT INTO app_user (login, name, password)
VALUES (
    'gustavo@empresa.com',
    'Gustavo',
    '{pbkdf2}b0fb9d22ddb5a39214519e5ed57a1cafb48690aeff2a7f728790494031aed4be816fdd7daed5b3e418441d6e895a80c0'
);

-- Ambiente demonstrativo do usuario Gustavo. Novos cadastros nao recebem dados.
WITH demo_company AS (
    INSERT INTO company (
        cnpj, legal_name, trade_name, legal_nature, tax_regime, size,
        postal_code, street, street_number, neighborhood, city, state,
        phone, email, active
    ) VALUES (
        '11222333000181', 'Gustavo Auto Center Ltda', 'Gustavo Auto Center',
        'LTDA', 'SIMPLES_NACIONAL', 'ME', '85801-000', 'Avenida Brasil', '1234',
        'Centro', 'Cascavel', 'PR', '(45) 3030-1234', 'contato@gustavoautocenter.com.br', true
    ) RETURNING id
), demo_user AS (
    SELECT id FROM app_user WHERE login = 'gustavo@empresa.com'
)
INSERT INTO user_company (fk_id_company, fk_id_user, role)
SELECT demo_company.id, demo_user.id, 'OWNER' FROM demo_company CROSS JOIN demo_user;

WITH c AS (SELECT id FROM company WHERE cnpj = '11222333000181')
INSERT INTO transaction_category (name, fk_id_company, type, active)
SELECT seed.name, c.id, seed.type, true
FROM c CROSS JOIN (VALUES
    ('Vendas', 'ENTRADA'), ('Servicos', 'ENTRADA'), ('Pecas', 'ENTRADA'),
    ('Fornecedores', 'SAIDA'), ('Salarios', 'SAIDA'), ('Impostos', 'SAIDA'),
    ('Aluguel', 'SAIDA'), ('Energia', 'SAIDA'), ('Manutencao', 'SAIDA'), ('Outros', 'SAIDA')
) AS seed(name, type);

WITH c AS (SELECT id FROM company WHERE cnpj = '11222333000181')
INSERT INTO bank_account (bank_name, branch, account_number, account_type, balance, default_account, fk_id_company)
SELECT seed.bank_name, seed.branch, seed.account_number, seed.account_type, seed.balance, seed.is_default, c.id
FROM c CROSS JOIN (VALUES
    ('Banco do Brasil', '1234', '56789-0', 'CORRENTE', 16350.00::numeric, true),
    ('Nubank', '0001', '98765-4', 'CORRENTE', 1460.00::numeric, false)
) AS seed(bank_name, branch, account_number, account_type, balance, is_default);

WITH c AS (SELECT id FROM company WHERE cnpj = '11222333000181'),
accounts AS (SELECT id, account_number FROM bank_account WHERE fk_id_company = (SELECT id FROM c)),
categories AS (SELECT id, name FROM transaction_category WHERE fk_id_company = (SELECT id FROM c))
INSERT INTO transaction (fk_id_company, fk_id_bank_account, fk_id_transaction_category, type, description, value, date, method)
SELECT c.id, a.id, cat.id, seed.type, seed.description, seed.value, seed.date, seed.method
FROM (VALUES
    ('56789-0', 'Vendas', 'ENTRADA', 'Servicos de revisao e manutencao', 4850.00::numeric, DATE '2026-10-20', 'PIX'),
    ('56789-0', 'Pecas', 'ENTRADA', 'Venda de pecas automotivas', 3200.00::numeric, DATE '2026-10-18', 'CARTAO'),
    ('56789-0', 'Fornecedores', 'SAIDA', 'Compra de estoque de pecas', 2750.00::numeric, DATE '2026-10-17', 'PIX'),
    ('56789-0', 'Salarios', 'SAIDA', 'Folha de pagamento', 6200.00::numeric, DATE '2026-10-15', 'PIX'),
    ('98765-4', 'Vendas', 'ENTRADA', 'Diagnostico veicular', 1840.00::numeric, DATE '2026-10-19', 'PIX'),
    ('98765-4', 'Energia', 'SAIDA', 'Conta de energia', 460.00::numeric, DATE '2026-10-12', 'DINHEIRO')
) AS seed(account_number, category, type, description, value, date, method)
CROSS JOIN c
JOIN accounts a ON a.account_number = seed.account_number
JOIN categories cat ON cat.name = seed.category;

INSERT INTO transaction (fk_id_company, fk_id_bank_account, fk_id_transaction_category, type, description, value, date, method)
SELECT c.id, a.id, cat.id, 'SAIDA', 'Aluguel mensal da oficina', 2800.00, DATE '2026-10-05', 'PIX'
FROM company c
JOIN bank_account a ON a.fk_id_company = c.id AND a.account_number = '56789-0'
JOIN transaction_category cat ON cat.fk_id_company = c.id AND cat.name = 'Aluguel'
WHERE c.cnpj = '11222333000181';

INSERT INTO recurrence_rule (fk_id_transaction, method, start_date, end_date, last_execution, frequency)
SELECT t.id, 'PIX', DATE '2026-10-05', DATE '2027-10-05', DATE '2026-10-05', 'MONTHLY'
FROM transaction t WHERE t.description = 'Aluguel mensal da oficina'
AND t.fk_id_company = (SELECT id FROM company WHERE cnpj = '11222333000181');