-- ========================
-- CREATE
-- ========================

-- COMPANY
CREATE TABLE IF NOT EXISTS company (
    id serial PRIMARY KEY,
    cnpj varchar(14) NOT NULL UNIQUE,
    legal_name varchar(50) NOT NULL,
    trade_name varchar(50) NOT NULL
);

-- USER
CREATE TABLE IF NOT EXISTS app_user (
    id serial PRIMARY KEY,
    login varchar(50) NOT NULL UNIQUE,
    password varchar(100) NOT NULL
);

-- USER COMPANY
CREATE TABLE IF NOT EXISTS user_company (
    id serial PRIMARY KEY,
    fk_id_company INTEGER NOT NULL REFERENCES company(id),
    fk_id_user INTEGER NOT NULL REFERENCES app_user(id),
    role varchar(20) NOT NULL DEFAULT 'VIEWER',
    UNIQUE (fk_id_company, fk_id_user)
);

-- BANK ACCOUNT
CREATE TABLE IF NOT EXISTS bank_account (
    id serial PRIMARY KEY,
    bank_name varchar(50) NOT NULL,
    branch varchar(4) NOT NULL,
    account_number varchar(13) NOT NULL,
    account_type varchar(20) NOT NULL,
    balance DECIMAL(19, 2) NOT NULL,
    default_account BOOLEAN NOT NULL,
    fk_id_company INTEGER NOT NULL REFERENCES company(id)
);

-- TRANSACTION CATEGORY
CREATE TABLE IF NOT EXISTS transaction_category (
    id serial PRIMARY KEY,
    name varchar(50) NOT NULL,
    fk_id_company INTEGER NOT NULL REFERENCES company(id),
    type varchar(50) NOT NULL,
    active BOOLEAN NOT NULL,
    UNIQUE(name, fk_id_company)
);

-- TRANSACTION
CREATE TABLE IF NOT EXISTS transaction (
    id serial PRIMARY KEY,
    fk_id_company INTEGER NOT NULL REFERENCES company(id),
    fk_id_bank_account INTEGER NOT NULL REFERENCES bank_account(id),
    fk_id_transaction_category INTEGER NOT NULL REFERENCES transaction_category(id),
    type varchar(50) NOT NULL,
    description varchar(255),
    value DECIMAL(19, 2) NOT NULL,
    date date NOT NULL,
    method varchar(50) NOT NULL
);

-- RECURRENCE RULE
CREATE TABLE IF NOT EXISTS recurrence_rule (
    id serial PRIMARY KEY,
    fk_id_transaction INTEGER NOT NULL UNIQUE REFERENCES transaction(id),
    method varchar(50) NOT NULL,
    start_date date NOT NULL,
    end_date date,
    last_execution date NOT NULL
);
