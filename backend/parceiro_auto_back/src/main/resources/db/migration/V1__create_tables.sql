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
    

)