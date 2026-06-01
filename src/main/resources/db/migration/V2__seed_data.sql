INSERT INTO roles (role_name) VALUES
    ('ADMIN'),
    ('WAREHOUSE_MANAGER'),
    ('WAREHOUSE_STAFF'),
    ('VIEWER');

INSERT INTO warehouses (warehouse_code, warehouse_name, city, address_line) VALUES
    ('A', 'Warehouse A', 'Sofia', '1 Vitosha Logistics Park'),
    ('B', 'Warehouse B', 'Plovdiv', '8 Trakia Industrial Zone'),
    ('C', 'Warehouse C', 'Varna', '15 Port Supply Avenue');

INSERT INTO categories (category_name) VALUES
    ('CPU'),
    ('GPU'),
    ('Motherboard'),
    ('RAM'),
    ('SSD'),
    ('PSU');

INSERT INTO manufacturers (manufacturer_name) VALUES
    ('AMD'),
    ('Intel'),
    ('ASUS'),
    ('MSI'),
    ('Samsung'),
    ('Corsair'),
    ('Gigabyte'),
    ('Cooler Master'),
    ('Kingston');

INSERT INTO users (username, password_hash, full_name, role_id, warehouse_id, active)
SELECT 'admin', 'admin', 'System Admin', r.role_id, NULL, TRUE
FROM roles r
WHERE r.role_name = 'ADMIN';

INSERT INTO users (username, password_hash, full_name, role_id, warehouse_id, active)
SELECT 'manager_a', 'manager_a', 'Mila Petrova', r.role_id, w.warehouse_id, TRUE
FROM roles r
JOIN warehouses w ON w.warehouse_code = 'A'
WHERE r.role_name = 'WAREHOUSE_MANAGER';

INSERT INTO users (username, password_hash, full_name, role_id, warehouse_id, active)
SELECT 'manager_b', 'manager_b', 'Nikola Georgiev', r.role_id, w.warehouse_id, TRUE
FROM roles r
JOIN warehouses w ON w.warehouse_code = 'B'
WHERE r.role_name = 'WAREHOUSE_MANAGER';

INSERT INTO users (username, password_hash, full_name, role_id, warehouse_id, active)
SELECT 'staff_c', 'staff_c', 'Teodor Iliev', r.role_id, w.warehouse_id, TRUE
FROM roles r
JOIN warehouses w ON w.warehouse_code = 'C'
WHERE r.role_name = 'WAREHOUSE_STAFF';

INSERT INTO users (username, password_hash, full_name, role_id, warehouse_id, active)
SELECT 'viewer', 'viewer', 'Audit Viewer', r.role_id, w.warehouse_id, TRUE
FROM roles r
JOIN warehouses w ON w.warehouse_code = 'A'
WHERE r.role_name = 'VIEWER';

INSERT INTO products (product_code, manufacturer_id, category_id, model_name, description, unit_name, active)
SELECT 'CPU-INT-14700K', m.manufacturer_id, c.category_id, 'Intel Core i7-14700K', '14th gen desktop CPU', 'pcs', TRUE
FROM manufacturers m
JOIN categories c ON c.category_name = 'CPU'
WHERE m.manufacturer_name = 'Intel';

INSERT INTO products (product_code, manufacturer_id, category_id, model_name, description, unit_name, active)
SELECT 'GPU-ASUS-4070S', m.manufacturer_id, c.category_id, 'ASUS TUF RTX 4070 SUPER', 'High-end graphics card', 'pcs', TRUE
FROM manufacturers m
JOIN categories c ON c.category_name = 'GPU'
WHERE m.manufacturer_name = 'ASUS';

INSERT INTO products (product_code, manufacturer_id, category_id, model_name, description, unit_name, active)
SELECT 'RAM-COR-32D5', m.manufacturer_id, c.category_id, 'Corsair 32GB DDR5 Kit', '2x16GB DDR5 memory kit', 'pcs', TRUE
FROM manufacturers m
JOIN categories c ON c.category_name = 'RAM'
WHERE m.manufacturer_name = 'Corsair';

INSERT INTO products (product_code, manufacturer_id, category_id, model_name, description, unit_name, active)
SELECT 'SSD-SAM-990P2', m.manufacturer_id, c.category_id, 'Samsung 990 PRO 2TB', 'NVMe SSD 2TB', 'pcs', TRUE
FROM manufacturers m
JOIN categories c ON c.category_name = 'SSD'
WHERE m.manufacturer_name = 'Samsung';

INSERT INTO products (product_code, manufacturer_id, category_id, model_name, description, unit_name, active)
SELECT 'MB-MSI-B650T', m.manufacturer_id, c.category_id, 'MSI B650 Tomahawk', 'AM5 motherboard', 'pcs', TRUE
FROM manufacturers m
JOIN categories c ON c.category_name = 'Motherboard'
WHERE m.manufacturer_name = 'MSI';

INSERT INTO products (product_code, manufacturer_id, category_id, model_name, description, unit_name, active)
SELECT 'PSU-CM-850G', m.manufacturer_id, c.category_id, 'Cooler Master 850W Gold PSU', '80 Plus Gold power supply', 'pcs', TRUE
FROM manufacturers m
JOIN categories c ON c.category_name = 'PSU'
WHERE m.manufacturer_name = 'Cooler Master';

INSERT INTO products (product_code, manufacturer_id, category_id, model_name, description, unit_name, active)
SELECT 'CPU-AMD-7800X3D', m.manufacturer_id, c.category_id, 'AMD Ryzen 7 7800X3D', 'Gaming-focused desktop CPU', 'pcs', TRUE
FROM manufacturers m
JOIN categories c ON c.category_name = 'CPU'
WHERE m.manufacturer_name = 'AMD';

INSERT INTO products (product_code, manufacturer_id, category_id, model_name, description, unit_name, active)
SELECT 'GPU-GIG-4060', m.manufacturer_id, c.category_id, 'Gigabyte RTX 4060', 'Mid-range graphics card', 'pcs', TRUE
FROM manufacturers m
JOIN categories c ON c.category_name = 'GPU'
WHERE m.manufacturer_name = 'Gigabyte';

INSERT INTO products (product_code, manufacturer_id, category_id, model_name, description, unit_name, active)
SELECT 'RAM-KIN-32D5', m.manufacturer_id, c.category_id, 'Kingston Fury 32GB DDR5', '2x16GB DDR5 memory kit', 'pcs', TRUE
FROM manufacturers m
JOIN categories c ON c.category_name = 'RAM'
WHERE m.manufacturer_name = 'Kingston';

INSERT INTO inventory (warehouse_id, product_id, quantity)
SELECT w.warehouse_id, p.product_id, 14
FROM warehouses w
JOIN products p ON p.product_code = 'CPU-INT-14700K'
WHERE w.warehouse_code = 'A';

INSERT INTO inventory (warehouse_id, product_id, quantity)
SELECT w.warehouse_id, p.product_id, 8
FROM warehouses w
JOIN products p ON p.product_code = 'GPU-ASUS-4070S'
WHERE w.warehouse_code = 'A';

INSERT INTO inventory (warehouse_id, product_id, quantity)
SELECT w.warehouse_id, p.product_id, 19
FROM warehouses w
JOIN products p ON p.product_code = 'RAM-COR-32D5'
WHERE w.warehouse_code = 'A';

INSERT INTO inventory (warehouse_id, product_id, quantity)
SELECT w.warehouse_id, p.product_id, 22
FROM warehouses w
JOIN products p ON p.product_code = 'SSD-SAM-990P2'
WHERE w.warehouse_code = 'B';

INSERT INTO inventory (warehouse_id, product_id, quantity)
SELECT w.warehouse_id, p.product_id, 11
FROM warehouses w
JOIN products p ON p.product_code = 'MB-MSI-B650T'
WHERE w.warehouse_code = 'B';

INSERT INTO inventory (warehouse_id, product_id, quantity)
SELECT w.warehouse_id, p.product_id, 9
FROM warehouses w
JOIN products p ON p.product_code = 'PSU-CM-850G'
WHERE w.warehouse_code = 'B';

INSERT INTO inventory (warehouse_id, product_id, quantity)
SELECT w.warehouse_id, p.product_id, 16
FROM warehouses w
JOIN products p ON p.product_code = 'CPU-AMD-7800X3D'
WHERE w.warehouse_code = 'C';

INSERT INTO inventory (warehouse_id, product_id, quantity)
SELECT w.warehouse_id, p.product_id, 13
FROM warehouses w
JOIN products p ON p.product_code = 'GPU-GIG-4060'
WHERE w.warehouse_code = 'C';

INSERT INTO inventory (warehouse_id, product_id, quantity)
SELECT w.warehouse_id, p.product_id, 25
FROM warehouses w
JOIN products p ON p.product_code = 'RAM-KIN-32D5'
WHERE w.warehouse_code = 'C';

INSERT INTO requests (
    request_number,
    from_warehouse_id,
    to_warehouse_id,
    requested_by_user_id,
    approved_by_user_id,
    request_status,
    request_note,
    created_at,
    updated_at
)
SELECT 'REQ-1001',
       src.warehouse_id,
       dst.warehouse_id,
       requester.user_id,
       approver.user_id,
       'PENDING',
       'Need GPUs and SSDs for urgent replenishment',
       CURRENT_TIMESTAMP - INTERVAL '48 minutes',
       CURRENT_TIMESTAMP - INTERVAL '48 minutes'
FROM warehouses src
JOIN warehouses dst ON dst.warehouse_code = 'B'
JOIN users requester ON requester.username = 'manager_a'
LEFT JOIN users approver ON approver.username = 'manager_b'
WHERE src.warehouse_code = 'A';

INSERT INTO requests (
    request_number,
    from_warehouse_id,
    to_warehouse_id,
    requested_by_user_id,
    approved_by_user_id,
    request_status,
    request_note,
    created_at,
    updated_at
)
SELECT 'REQ-1002',
       src.warehouse_id,
       dst.warehouse_id,
       requester.user_id,
       approver.user_id,
       'APPROVED',
       'Approved motherboard transfer',
       CURRENT_TIMESTAMP - INTERVAL '4 hours',
       CURRENT_TIMESTAMP - INTERVAL '3 hours 45 minutes'
FROM warehouses src
JOIN warehouses dst ON dst.warehouse_code = 'C'
JOIN users requester ON requester.username = 'manager_b'
LEFT JOIN users approver ON approver.username = 'staff_c'
WHERE src.warehouse_code = 'B';

INSERT INTO requests (
    request_number,
    from_warehouse_id,
    to_warehouse_id,
    requested_by_user_id,
    approved_by_user_id,
    request_status,
    request_note,
    created_at,
    updated_at
)
SELECT 'REQ-1003',
       src.warehouse_id,
       dst.warehouse_id,
       requester.user_id,
       approver.user_id,
       'PENDING',
       'Requesting CPUs and RAM for incoming dispatch',
       CURRENT_TIMESTAMP - INTERVAL '6 hours',
       CURRENT_TIMESTAMP - INTERVAL '6 hours'
FROM warehouses src
JOIN warehouses dst ON dst.warehouse_code = 'A'
JOIN users requester ON requester.username = 'staff_c'
LEFT JOIN users approver ON approver.username = 'manager_a'
WHERE src.warehouse_code = 'C';

INSERT INTO requests (
    request_number,
    from_warehouse_id,
    to_warehouse_id,
    requested_by_user_id,
    approved_by_user_id,
    request_status,
    request_note,
    created_at,
    updated_at
)
SELECT 'REQ-1004',
       src.warehouse_id,
       dst.warehouse_id,
       requester.user_id,
       approver.user_id,
       'COMPLETED',
       'Completed SSD transfer',
       CURRENT_TIMESTAMP - INTERVAL '1 day',
       CURRENT_TIMESTAMP - INTERVAL '23 hours'
FROM warehouses src
JOIN warehouses dst ON dst.warehouse_code = 'A'
JOIN users requester ON requester.username = 'manager_b'
LEFT JOIN users approver ON approver.username = 'manager_a'
WHERE src.warehouse_code = 'B';

INSERT INTO request_items (request_id, product_id, quantity_requested, quantity_approved)
SELECT r.request_id, p.product_id, 2, NULL
FROM requests r
JOIN products p ON p.product_code = 'GPU-ASUS-4070S'
WHERE r.request_number = 'REQ-1001';

INSERT INTO request_items (request_id, product_id, quantity_requested, quantity_approved)
SELECT r.request_id, p.product_id, 5, NULL
FROM requests r
JOIN products p ON p.product_code = 'SSD-SAM-990P2'
WHERE r.request_number = 'REQ-1001';

INSERT INTO request_items (request_id, product_id, quantity_requested, quantity_approved)
SELECT r.request_id, p.product_id, 3, 3
FROM requests r
JOIN products p ON p.product_code = 'MB-MSI-B650T'
WHERE r.request_number = 'REQ-1002';

INSERT INTO request_items (request_id, product_id, quantity_requested, quantity_approved)
SELECT r.request_id, p.product_id, 4, NULL
FROM requests r
JOIN products p ON p.product_code = 'CPU-INT-14700K'
WHERE r.request_number = 'REQ-1003';

INSERT INTO request_items (request_id, product_id, quantity_requested, quantity_approved)
SELECT r.request_id, p.product_id, 4, NULL
FROM requests r
JOIN products p ON p.product_code = 'RAM-COR-32D5'
WHERE r.request_number = 'REQ-1003';

INSERT INTO request_items (request_id, product_id, quantity_requested, quantity_approved)
SELECT r.request_id, p.product_id, 5, 5
FROM requests r
JOIN products p ON p.product_code = 'SSD-SAM-990P2'
WHERE r.request_number = 'REQ-1004';

INSERT INTO stock_movements (
    product_id,
    movement_type,
    quantity,
    source_warehouse_id,
    destination_warehouse_id,
    request_id,
    performed_by_user_id,
    details,
    movement_timestamp
)
SELECT p.product_id, 'DISPATCH', 2, src.warehouse_id, NULL, NULL, u.user_id,
       'Dispatched to partner retail floor', CURRENT_TIMESTAMP - INTERVAL '12 minutes'
FROM products p
JOIN warehouses src ON src.warehouse_code = 'A'
JOIN users u ON u.username = 'manager_a'
WHERE p.product_code = 'GPU-ASUS-4070S';

INSERT INTO stock_movements (
    product_id,
    movement_type,
    quantity,
    source_warehouse_id,
    destination_warehouse_id,
    request_id,
    performed_by_user_id,
    details,
    movement_timestamp
)
SELECT p.product_id, 'TRANSFER_OUT', 5, src.warehouse_id, dst.warehouse_id, r.request_id, u.user_id,
       'Approved REQ-1001', CURRENT_TIMESTAMP - INTERVAL '33 minutes'
FROM products p
JOIN warehouses src ON src.warehouse_code = 'B'
JOIN warehouses dst ON dst.warehouse_code = 'A'
JOIN requests r ON r.request_number = 'REQ-1001'
JOIN users u ON u.username = 'manager_b'
WHERE p.product_code = 'SSD-SAM-990P2';

INSERT INTO stock_movements (
    product_id,
    movement_type,
    quantity,
    source_warehouse_id,
    destination_warehouse_id,
    request_id,
    performed_by_user_id,
    details,
    movement_timestamp
)
SELECT p.product_id, 'TRANSFER_IN', 5, src.warehouse_id, dst.warehouse_id, r.request_id, u.user_id,
       'Approved REQ-1001', CURRENT_TIMESTAMP - INTERVAL '33 minutes'
FROM products p
JOIN warehouses src ON src.warehouse_code = 'B'
JOIN warehouses dst ON dst.warehouse_code = 'A'
JOIN requests r ON r.request_number = 'REQ-1001'
JOIN users u ON u.username = 'manager_b'
WHERE p.product_code = 'SSD-SAM-990P2';

INSERT INTO stock_movements (
    product_id,
    movement_type,
    quantity,
    source_warehouse_id,
    destination_warehouse_id,
    request_id,
    performed_by_user_id,
    details,
    movement_timestamp
)
SELECT p.product_id, 'SUPPLY', 10, NULL, dst.warehouse_id, NULL, u.user_id,
       'Morning supply intake', CURRENT_TIMESTAMP - INTERVAL '2 hours'
FROM products p
JOIN warehouses dst ON dst.warehouse_code = 'C'
JOIN users u ON u.username = 'staff_c'
WHERE p.product_code = 'CPU-AMD-7800X3D';

INSERT INTO stock_movements (
    product_id,
    movement_type,
    quantity,
    source_warehouse_id,
    destination_warehouse_id,
    request_id,
    performed_by_user_id,
    details,
    movement_timestamp
)
SELECT p.product_id, 'DISPATCH', 1, src.warehouse_id, NULL, NULL, u.user_id,
       'Bench replacement dispatch', CURRENT_TIMESTAMP - INTERVAL '5 hours'
FROM products p
JOIN warehouses src ON src.warehouse_code = 'B'
JOIN users u ON u.username = 'manager_b'
WHERE p.product_code = 'MB-MSI-B650T';
