-- ============================================================================
-- Inventory Procurement API - initial schema
-- ============================================================================

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(100) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    name            VARCHAR(150) NOT NULL,
    role            VARCHAR(20)  NOT NULL CHECK (role IN ('USER', 'APPROVER')),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_username UNIQUE (username)
);

CREATE TABLE products (
    id              BIGSERIAL PRIMARY KEY,
    sku             VARCHAR(64)  NOT NULL,
    name            VARCHAR(255) NOT NULL,
    unit            VARCHAR(32)  NOT NULL,
    is_active       BOOLEAN      NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_products_sku UNIQUE (sku)
);

CREATE TABLE suppliers (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    email           VARCHAR(255),
    phone           VARCHAR(32),
    is_active       BOOLEAN      NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE warehouses (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(32)  NOT NULL,
    name            VARCHAR(255) NOT NULL,
    location        VARCHAR(255),
    is_active       BOOLEAN      NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_warehouses_code UNIQUE (code)
);

-- ----------------------------------------------------------------------------
-- Inventory
-- ----------------------------------------------------------------------------

CREATE TABLE inventory_balances (
    id              BIGSERIAL PRIMARY KEY,
    warehouse_id    BIGINT      NOT NULL REFERENCES warehouses(id),
    product_id      BIGINT      NOT NULL REFERENCES products(id),
    quantity        INTEGER     NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_inventory_balances_warehouse_product UNIQUE (warehouse_id, product_id)
);

CREATE TABLE inventory_movements (
    id              BIGSERIAL PRIMARY KEY,
    warehouse_id    BIGINT      NOT NULL REFERENCES warehouses(id),
    product_id      BIGINT      NOT NULL REFERENCES products(id),
    movement_type   VARCHAR(30) NOT NULL CHECK (movement_type IN ('PURCHASE_RECEIPT')),
    quantity        INTEGER     NOT NULL,
    reference       VARCHAR(64) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_inventory_movements_warehouse_product
    ON inventory_movements (warehouse_id, product_id);
CREATE INDEX idx_inventory_movements_reference
    ON inventory_movements (reference);

-- ----------------------------------------------------------------------------
-- Purchase Request
-- ----------------------------------------------------------------------------

CREATE TABLE purchase_requests (
    id                  BIGSERIAL PRIMARY KEY,
    request_number      VARCHAR(32)  NOT NULL,
    warehouse_id        BIGINT       NOT NULL REFERENCES warehouses(id),
    requested_by        BIGINT       NOT NULL REFERENCES users(id),
    status               VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
                         CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED')),
    approved_by          BIGINT       REFERENCES users(id),
    decision_at          TIMESTAMPTZ,
    decision_remarks     TEXT,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_purchase_requests_request_number UNIQUE (request_number)
);

CREATE TABLE purchase_request_items (
    id                    BIGSERIAL PRIMARY KEY,
    purchase_request_id   BIGINT  NOT NULL REFERENCES purchase_requests(id) ON DELETE CASCADE,
    product_id            BIGINT  NOT NULL REFERENCES products(id),
    quantity               INTEGER NOT NULL CHECK (quantity > 0),
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_pr_items_pr_product UNIQUE (purchase_request_id, product_id)
);

-- ----------------------------------------------------------------------------
-- Purchase Order
-- ----------------------------------------------------------------------------

CREATE TABLE purchase_orders (
    id                     BIGSERIAL PRIMARY KEY,
    po_number               VARCHAR(32) NOT NULL,
    purchase_request_id     BIGINT      NOT NULL REFERENCES purchase_requests(id),
    supplier_id             BIGINT      NOT NULL REFERENCES suppliers(id),
    warehouse_id            BIGINT      NOT NULL REFERENCES warehouses(id),
    status                  VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                             CHECK (status IN ('DRAFT', 'ORDERED', 'PARTIALLY_RECEIVED', 'RECEIVED', 'CANCELLED')),
    created_by              BIGINT      NOT NULL REFERENCES users(id),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_purchase_orders_po_number UNIQUE (po_number),
    -- Enforces "one Purchase Request produces at most one Purchase Order"
    -- as a database constraint, not just an application-level check.
    CONSTRAINT uq_purchase_orders_purchase_request UNIQUE (purchase_request_id)
);

CREATE TABLE purchase_order_items (
    id                   BIGSERIAL PRIMARY KEY,
    purchase_order_id     BIGINT  NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    product_id            BIGINT  NOT NULL REFERENCES products(id),
    ordered_quantity      INTEGER NOT NULL CHECK (ordered_quantity > 0),
    received_quantity     INTEGER NOT NULL DEFAULT 0 CHECK (received_quantity >= 0),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_po_items_po_product UNIQUE (purchase_order_id, product_id),
    CONSTRAINT chk_po_items_received_le_ordered CHECK (received_quantity <= ordered_quantity)
);

-- ----------------------------------------------------------------------------
-- Goods Receipt
-- ----------------------------------------------------------------------------

CREATE TABLE goods_receipts (
    id                  BIGSERIAL PRIMARY KEY,
    gr_number            VARCHAR(32) NOT NULL,
    purchase_order_id    BIGINT      NOT NULL REFERENCES purchase_orders(id),
    received_by          BIGINT      NOT NULL REFERENCES users(id),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_goods_receipts_gr_number UNIQUE (gr_number)
);

CREATE TABLE goods_receipt_items (
    id                  BIGSERIAL PRIMARY KEY,
    goods_receipt_id     BIGINT  NOT NULL REFERENCES goods_receipts(id) ON DELETE CASCADE,
    product_id           BIGINT  NOT NULL REFERENCES products(id),
    quantity              INTEGER NOT NULL CHECK (quantity > 0),
    CONSTRAINT uq_gr_items_gr_product UNIQUE (goods_receipt_id, product_id)
);
