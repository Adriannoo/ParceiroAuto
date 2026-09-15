ALTER TABLE user_company
    ALTER COLUMN fk_id_company TYPE bigint,
    ALTER COLUMN fk_id_user TYPE bigint;

ALTER TABLE bank_account
    ALTER COLUMN fk_id_company TYPE bigint;

ALTER TABLE transaction_category
    ALTER COLUMN fk_id_company TYPE bigint;

ALTER TABLE transaction
    ALTER COLUMN fk_id_company TYPE bigint,
    ALTER COLUMN fk_id_bank_account TYPE bigint,
    ALTER COLUMN fk_id_transaction_category TYPE bigint;

ALTER TABLE recurrence_rule
    ALTER COLUMN fk_id_transaction TYPE bigint;

ALTER TABLE company
    ALTER COLUMN id TYPE bigint;

ALTER TABLE app_user
    ALTER COLUMN id TYPE bigint;

ALTER TABLE user_company
    ALTER COLUMN id TYPE bigint;

ALTER TABLE bank_account
    ALTER COLUMN id TYPE bigint;

ALTER TABLE transaction_category
    ALTER COLUMN id TYPE bigint;

ALTER TABLE transaction
    ALTER COLUMN id TYPE bigint;

ALTER TABLE recurrence_rule
    ALTER COLUMN id TYPE bigint;

ALTER TABLE company
    ADD COLUMN IF NOT EXISTS state_registration varchar(30),
    ADD COLUMN IF NOT EXISTS legal_nature varchar(20) NOT NULL DEFAULT 'LTDA',
    ADD COLUMN IF NOT EXISTS tax_regime varchar(30) NOT NULL DEFAULT 'SIMPLES_NACIONAL',
    ADD COLUMN IF NOT EXISTS size varchar(20) NOT NULL DEFAULT 'ME',
    ADD COLUMN IF NOT EXISTS postal_code varchar(10) NOT NULL DEFAULT '00000-000',
    ADD COLUMN IF NOT EXISTS street varchar(100) NOT NULL DEFAULT 'Nao informado',
    ADD COLUMN IF NOT EXISTS street_number varchar(10) NOT NULL DEFAULT 'S/N',
    ADD COLUMN IF NOT EXISTS address_complement varchar(100),
    ADD COLUMN IF NOT EXISTS neighborhood varchar(80) NOT NULL DEFAULT 'Nao informado',
    ADD COLUMN IF NOT EXISTS city varchar(80) NOT NULL DEFAULT 'Nao informado',
    ADD COLUMN IF NOT EXISTS state varchar(2) NOT NULL DEFAULT 'PR',
    ADD COLUMN IF NOT EXISTS phone varchar(20) NOT NULL DEFAULT '0000000000',
    ADD COLUMN IF NOT EXISTS email varchar(120) NOT NULL DEFAULT 'nao-informado@example.com',
    ADD COLUMN IF NOT EXISTS active boolean NOT NULL DEFAULT true;
