CREATE TABLE roles (
    role_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE warehouses (
    warehouse_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    warehouse_code VARCHAR(10) NOT NULL UNIQUE,
    warehouse_name VARCHAR(120) NOT NULL UNIQUE,
    city VARCHAR(80) NOT NULL,
    address_line VARCHAR(200) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE users (
    user_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    role_id BIGINT NOT NULL REFERENCES roles(role_id),
    warehouse_id BIGINT REFERENCES warehouses(warehouse_id),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE categories (
    category_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_name VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE manufacturers (
    manufacturer_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    manufacturer_name VARCHAR(120) NOT NULL UNIQUE
);

CREATE TABLE products (
    product_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_code VARCHAR(40) NOT NULL UNIQUE,
    manufacturer_id BIGINT NOT NULL REFERENCES manufacturers(manufacturer_id),
    category_id BIGINT NOT NULL REFERENCES categories(category_id),
    model_name VARCHAR(160) NOT NULL,
    description TEXT,
    unit_name VARCHAR(20) NOT NULL DEFAULT 'pcs',
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE inventory (
    inventory_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(warehouse_id),
    product_id BIGINT NOT NULL REFERENCES products(product_id),
    quantity INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uq_inventory UNIQUE (warehouse_id, product_id),
    CONSTRAINT chk_inventory_quantity CHECK (quantity >= 0)
);

CREATE TABLE requests (
    request_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    request_number VARCHAR(30) NOT NULL UNIQUE,
    from_warehouse_id BIGINT NOT NULL REFERENCES warehouses(warehouse_id),
    to_warehouse_id BIGINT NOT NULL REFERENCES warehouses(warehouse_id),
    requested_by_user_id BIGINT NOT NULL REFERENCES users(user_id),
    approved_by_user_id BIGINT REFERENCES users(user_id),
    request_status VARCHAR(20) NOT NULL,
    request_note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_requests_warehouses CHECK (from_warehouse_id <> to_warehouse_id),
    CONSTRAINT chk_requests_status CHECK (request_status IN ('PENDING', 'APPROVED', 'REJECTED', 'COMPLETED'))
);

CREATE TABLE request_items (
    request_item_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    request_id BIGINT NOT NULL REFERENCES requests(request_id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(product_id),
    quantity_requested INTEGER NOT NULL,
    quantity_approved INTEGER,
    CONSTRAINT chk_request_items_requested CHECK (quantity_requested > 0),
    CONSTRAINT chk_request_items_approved CHECK (quantity_approved IS NULL OR quantity_approved >= 0)
);

CREATE TABLE stock_movements (
    movement_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(product_id),
    movement_type VARCHAR(20) NOT NULL,
    quantity INTEGER NOT NULL,
    source_warehouse_id BIGINT REFERENCES warehouses(warehouse_id),
    destination_warehouse_id BIGINT REFERENCES warehouses(warehouse_id),
    request_id BIGINT REFERENCES requests(request_id),
    performed_by_user_id BIGINT NOT NULL REFERENCES users(user_id),
    details VARCHAR(255),
    movement_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_stock_movements_type CHECK (movement_type IN ('SUPPLY', 'DISPATCH', 'TRANSFER_OUT', 'TRANSFER_IN')),
    CONSTRAINT chk_stock_movements_quantity CHECK (quantity > 0)
);
