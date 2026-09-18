ALTER TABLE recurrence_rule
    ADD COLUMN IF NOT EXISTS frequency varchar(20) NOT NULL DEFAULT 'MONTHLY';
