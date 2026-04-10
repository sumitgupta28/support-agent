CREATE TABLE customers (
    id         VARCHAR(20)  PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    email      VARCHAR(150) NOT NULL UNIQUE,
    tier       VARCHAR(20)  NOT NULL DEFAULT 'standard',
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE orders (
    id               VARCHAR(30)   PRIMARY KEY,
    customer_id      VARCHAR(20)   NOT NULL REFERENCES customers(id),
    amount           NUMERIC(10,2) NOT NULL,
    status           VARCHAR(30)   NOT NULL,
    created_at       TIMESTAMP     NOT NULL,
    refund_eligible  BOOLEAN       NOT NULL DEFAULT FALSE,
    refunded         BOOLEAN       NOT NULL DEFAULT FALSE
);

CREATE TABLE escalations (
    id           SERIAL       PRIMARY KEY,
    session_id   VARCHAR(50)  NOT NULL,
    customer_id  VARCHAR(20),
    reason       TEXT,
    context_json TEXT,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    resolved     BOOLEAN      NOT NULL DEFAULT FALSE
);
