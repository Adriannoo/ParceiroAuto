-- Dados de demonstracao para o projeto. Nenhuma senha fixa e criada.
INSERT INTO company (
    cnpj, legal_name, trade_name, legal_nature, tax_regime, size,
    postal_code, street, street_number, neighborhood, city, state, phone, email
) VALUES (
    '11222333000181', 'Empresa de demonstracao', 'ParceiroAuto Demo',
    'LTDA', 'SIMPLES_NACIONAL', 'ME', '85800-000', 'Rua de demonstracao',
    '1', 'Centro', 'Cascavel', 'PR', '45999999999', 'demo@example.com'
);

INSERT INTO bank_account (
    bank_name, branch, account_number, account_type, balance, default_account, fk_id_company
)
SELECT 'Banco de demonstracao', '0001', '123456', 'CORRENTE', 0, true, id
FROM company WHERE cnpj = '11222333000181';

-- Os IDs sao obtidos pela empresa, sem depender de valores fixos da sequencia.
INSERT INTO transaction_category(name, fk_id_company, type, active)
SELECT defaults.name, company.id, defaults.type, true
FROM company
CROSS JOIN (VALUES
    ('Vendas', 'ENTRADA'), ('Servicos', 'ENTRADA'), ('Pecas', 'ENTRADA'),
    ('Fornecedores', 'SAIDA'), ('Salarios', 'SAIDA'), ('Impostos', 'SAIDA'),
    ('Aluguel', 'SAIDA'), ('Energia', 'SAIDA'), ('Manutencao', 'SAIDA'), ('Outros', 'SAIDA')
) AS defaults(name, type)
WHERE company.cnpj = '11222333000181';
