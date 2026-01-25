-- Таблица курсов валют
CREATE TABLE exchange_rates (
    id                  BIGSERIAL PRIMARY KEY,
    base_currency       VARCHAR(3) NOT NULL,
    target_currency     VARCHAR(3) NOT NULL,
    rate_date           DATE NOT NULL,
    rate                DECIMAL(18,8) NOT NULL,
    previous_rate       DECIMAL(18,8),
    source              VARCHAR(50),
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_exchange_rates_currency_date UNIQUE (base_currency, target_currency, rate_date)
);

CREATE INDEX idx_exchange_rates_currency_date ON exchange_rates (base_currency, target_currency, rate_date);
CREATE INDEX idx_exchange_rates_rate_date       ON exchange_rates (rate_date);

-- Таблица лимитов
CREATE TABLE limits (
    id              BIGSERIAL PRIMARY KEY,
    category        VARCHAR(20) NOT NULL CHECK (category IN ('PRODUCT', 'SERVICE')),
    limit_sum       DECIMAL(15,2) NOT NULL DEFAULT 1000.00,
    limit_datetime  TIMESTAMP WITH TIME ZONE NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'USD',

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_limits_category_datetime UNIQUE (category, limit_datetime)
);

CREATE INDEX idx_limits_category        ON limits (category);
CREATE INDEX idx_limits_limit_datetime  ON limits (limit_datetime);
CREATE INDEX idx_limits_created_at      ON limits (created_at);

-- Таблица транзакций
CREATE TABLE transactions (
    id                  BIGSERIAL PRIMARY KEY,
    account_from        VARCHAR(20) NOT NULL,
    account_to          VARCHAR(20) NOT NULL,
    currency_shortname  VARCHAR(3) NOT NULL,
    sum                 DECIMAL(15,2) NOT NULL,
    expense_category    VARCHAR(20) NOT NULL CHECK (expense_category IN ('PRODUCT', 'SERVICE')),
    datetime            TIMESTAMP WITH TIME ZONE NOT NULL,
    usd_amount          DECIMAL(15,2),
    limit_id            BIGINT,
    limit_exceeded      BOOLEAN NOT NULL DEFAULT FALSE,

    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_transactions_limit FOREIGN KEY (limit_id) REFERENCES limits(id)
);

CREATE INDEX idx_transactions_datetime          ON transactions (datetime);
CREATE INDEX idx_transactions_expense_category  ON transactions (expense_category);
CREATE INDEX idx_transactions_limit_exceeded    ON transactions (limit_exceeded);
CREATE INDEX idx_transactions_account_from      ON transactions (account_from);
CREATE INDEX idx_transactions_limit_id          ON transactions (limit_id);
CREATE INDEX idx_transactions_created_at        ON transactions (created_at);