-- ========================
-- CREATE
-- ========================

-- COMPANY
CREATE TABLE company (
    id bigserial PRIMARY KEY,
    cnpj varchar(14) NOT NULL UNIQUE,
    legal_name varchar(50) NOT NULL,
    trade_name varchar(50) NOT NULL,
    state_registration varchar(30),
    legal_nature varchar(20) NOT NULL DEFAULT 'LTDA',
    tax_regime varchar(30) NOT NULL DEFAULT 'SIMPLES_NACIONAL',
    size varchar(20) NOT NULL DEFAULT 'ME',
    postal_code varchar(10) NOT NULL DEFAULT '00000-000',
    street varchar(100) NOT NULL DEFAULT 'Nao informado',
    street_number varchar(10) NOT NULL DEFAULT 'S/N',
    address_complement varchar(100),
    neighborhood varchar(80) NOT NULL DEFAULT 'Nao informado',
    city varchar(80) NOT NULL DEFAULT 'Nao informado',
    state varchar(2) NOT NULL DEFAULT 'PR',
    phone varchar(20) NOT NULL DEFAULT '0000000000',
    email varchar(120) NOT NULL DEFAULT 'nao-informado@example.com',
    active boolean NOT NULL DEFAULT true
);

-- USER
CREATE TABLE app_user (
    id bigserial PRIMARY KEY,
    login varchar(50) NOT NULL UNIQUE,
    password varchar(255) NOT NULL
);

-- USER COMPANY
CREATE TABLE user_company (
    id bigserial PRIMARY KEY,
    fk_id_company bigint NOT NULL REFERENCES company(id),
    fk_id_user bigint NOT NULL REFERENCES app_user(id),
    role varchar(20) NOT NULL DEFAULT 'VIEWER',
    UNIQUE (fk_id_company, fk_id_user)
);

-- BANK ACCOUNT
CREATE TABLE bank_account (
    id bigserial PRIMARY KEY,
    bank_name varchar(50) NOT NULL,
    branch varchar(4) NOT NULL,
    account_number varchar(13) NOT NULL,
    account_type varchar(20) NOT NULL,
    balance DECIMAL(19, 2) NOT NULL,
    default_account BOOLEAN NOT NULL,
    fk_id_company bigint NOT NULL REFERENCES company(id)
);

-- TRANSACTION CATEGORY
CREATE TABLE transaction_category (
    id bigserial PRIMARY KEY,
    name varchar(50) NOT NULL,
    fk_id_company bigint NOT NULL REFERENCES company(id),
    type varchar(50) NOT NULL,
    active BOOLEAN NOT NULL,
    UNIQUE(name, fk_id_company)
);

-- TRANSACTION
CREATE TABLE transaction (
    id bigserial PRIMARY KEY,
    fk_id_company bigint NOT NULL REFERENCES company(id),
    fk_id_bank_account bigint NOT NULL REFERENCES bank_account(id),
    fk_id_transaction_category bigint NOT NULL REFERENCES transaction_category(id),
    type varchar(50) NOT NULL,
    description varchar(255),
    value DECIMAL(19, 2) NOT NULL,
    date date NOT NULL,
    method varchar(50) NOT NULL
);

-- RECURRENCE RULE
CREATE TABLE recurrence_rule (
    id bigserial PRIMARY KEY,
    fk_id_transaction bigint NOT NULL UNIQUE REFERENCES transaction(id),
    method varchar(50) NOT NULL,
    start_date date NOT NULL,
    end_date date,
    last_execution date NOT NULL,
    frequency varchar(20) NOT NULL DEFAULT 'MONTHLY'
);
