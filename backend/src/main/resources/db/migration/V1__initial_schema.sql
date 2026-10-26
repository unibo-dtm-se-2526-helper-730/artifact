-- V1: initial schema.

CREATE TABLE instruments (
    ticker                   VARCHAR(20)  PRIMARY KEY,
    isin                     VARCHAR(12)  CHECK (isin ~ '^[A-Z]{2}[A-Z0-9]{9}[0-9]$'),
    name                     VARCHAR(255),
    asset_class              VARCHAR(30)  CHECK (asset_class IN (
                                 'STOCK', 'BOND', 'GOVERNMENT_BOND_WL', 'GOVERNMENT_BOND_NON_WL',
                                 'UCITS_ETF', 'NON_UCITS_ETF', 'ETC_ETN')),
    government_bond_share    NUMERIC(5, 4) CHECK (government_bond_share BETWEEN 0 AND 1),
    foreign_withholding_rate NUMERIC(5, 4) CHECK (foreign_withholding_rate BETWEEN 0 AND 1)
);

CREATE TABLE import_batches (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    filename    VARCHAR(255) NOT NULL,
    broker      VARCHAR(30)  NOT NULL,
    imported_at TIMESTAMPTZ  NOT NULL,
    imported    INTEGER      NOT NULL CHECK (imported >= 0),
    duplicates  INTEGER      NOT NULL CHECK (duplicates >= 0),
    errors      INTEGER      NOT NULL CHECK (errors >= 0)
);

CREATE TABLE transactions (
    id              CHAR(64)       PRIMARY KEY,
    executed_at     TIMESTAMPTZ    NOT NULL,
    operation_type  VARCHAR(30)    NOT NULL CHECK (operation_type IN (
                        'BUY', 'SELL', 'DIVIDEND', 'COUPON',
                        'CASH_TOP_UP', 'CASH_WITHDRAWAL', 'CUSTODY_FEE')),
    ticker          VARCHAR(20)    REFERENCES instruments (ticker),
    quantity        NUMERIC(24, 10),
    price_per_share NUMERIC(19, 6),
    total_amount    NUMERIC(19, 4) NOT NULL,
    currency        CHAR(3)        NOT NULL,
    file_row        INTEGER        NOT NULL,
    batch_id        BIGINT         NOT NULL REFERENCES import_batches (id)
);

CREATE INDEX idx_transactions_executed_at ON transactions (executed_at);
CREATE INDEX idx_transactions_ticker ON transactions (ticker);

CREATE TABLE year_end_prices (
    id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tax_year SMALLINT       NOT NULL,
    ticker   VARCHAR(20)    NOT NULL REFERENCES instruments (ticker),
    price    NUMERIC(19, 6) NOT NULL CHECK (price > 0),
    CONSTRAINT uq_year_end_prices UNIQUE (tax_year, ticker)
);

CREATE TABLE carried_losses (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- year in which the loss was realized; amount stored as a positive number
    loss_year SMALLINT       NOT NULL UNIQUE,
    amount    NUMERIC(19, 2) NOT NULL CHECK (amount > 0)
);