-- Core schema for logistics inventory management

CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(32) NOT NULL UNIQUE
);

CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255),
    full_name   VARCHAR(255) NOT NULL,
    provider    VARCHAR(16)  NOT NULL DEFAULT 'LOCAL',
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE user_roles (
    user_id     BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id     BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    token       VARCHAR(64) NOT NULL UNIQUE,
    user_id     BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(128) NOT NULL UNIQUE,
    description VARCHAR(512)
);

CREATE TABLE suppliers (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255),
    phone         VARCHAR(64),
    address       VARCHAR(512)
);

CREATE TABLE warehouses (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(16)  NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    location    VARCHAR(255),
    capacity    INT
);

CREATE TABLE products (
    id            BIGSERIAL PRIMARY KEY,
    sku           VARCHAR(64)   NOT NULL UNIQUE,
    name          VARCHAR(255)  NOT NULL,
    description   VARCHAR(1024),
    category_id   BIGINT REFERENCES categories (id),
    supplier_id   BIGINT REFERENCES suppliers (id),
    unit_price    NUMERIC(12, 2) NOT NULL DEFAULT 0,
    reorder_level INT            NOT NULL DEFAULT 0,
    active        BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE TABLE stock_levels (
    id           BIGSERIAL PRIMARY KEY,
    product_id   BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    warehouse_id BIGINT NOT NULL REFERENCES warehouses (id) ON DELETE CASCADE,
    quantity     INT    NOT NULL DEFAULT 0,
    UNIQUE (product_id, warehouse_id)
);

CREATE TABLE stock_movements (
    id                  BIGSERIAL PRIMARY KEY,
    product_id          BIGINT      NOT NULL REFERENCES products (id),
    warehouse_id        BIGINT      NOT NULL REFERENCES warehouses (id),
    target_warehouse_id BIGINT REFERENCES warehouses (id),
    type                VARCHAR(16) NOT NULL,
    quantity            INT         NOT NULL,
    reference           VARCHAR(128),
    note                VARCHAR(512),
    created_by          VARCHAR(255),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_supplier ON products (supplier_id);
CREATE INDEX idx_stock_levels_product ON stock_levels (product_id);
CREATE INDEX idx_stock_levels_warehouse ON stock_levels (warehouse_id);
CREATE INDEX idx_movements_product ON stock_movements (product_id);
CREATE INDEX idx_movements_created ON stock_movements (created_at);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

INSERT INTO roles (name) VALUES ('ADMIN'), ('MANAGER'), ('VIEWER');
