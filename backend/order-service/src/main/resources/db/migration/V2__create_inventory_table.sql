CREATE TABLE inventory_items (
    product_id VARCHAR(255) PRIMARY KEY,
    available_quantity INTEGER NOT NULL CHECK (available_quantity >= 0),
    reserved_quantity INTEGER NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

INSERT INTO inventory_items (
    product_id,
    available_quantity,
    reserved_quantity,
    version,
    updated_at
) VALUES
    ('product-001', 100, 0, 0, NOW()),
    ('product-002', 100, 0, 0, NOW()),
    ('product-003', 100, 0, 0, NOW());
