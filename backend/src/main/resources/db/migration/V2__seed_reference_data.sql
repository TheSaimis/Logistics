-- Demo reference data (users are seeded by the application so passwords are bcrypt-encoded)

INSERT INTO categories (name, description) VALUES
    ('Electronics', 'Electronic devices and components'),
    ('Packaging', 'Boxes, pallets and packaging material'),
    ('Raw Materials', 'Unprocessed production inputs'),
    ('Spare Parts', 'Machine and vehicle spare parts'),
    ('Consumables', 'Office and warehouse consumables');

INSERT INTO suppliers (name, contact_email, phone, address) VALUES
    ('Baltic Components UAB', 'sales@balticcomponents.lt', '+370 600 11111', 'Savanoriu pr. 12, Vilnius'),
    ('NordPack OY', 'orders@nordpack.fi', '+358 40 2222222', 'Teollisuuskatu 5, Helsinki'),
    ('Rhein Metall GmbH', 'kontakt@rheinmetall-supply.de', '+49 221 333333', 'Industriestr. 8, Cologne'),
    ('Global Parts Ltd', 'support@globalparts.com', '+44 20 4444444', '12 Dock Road, London');

INSERT INTO warehouses (code, name, location, capacity) VALUES
    ('VNO-1', 'Vilnius Central', 'Vilnius, Lithuania', 12000),
    ('KUN-1', 'Kaunas Hub', 'Kaunas, Lithuania', 8000),
    ('RIX-1', 'Riga Distribution', 'Riga, Latvia', 15000);

INSERT INTO products (sku, name, description, category_id, supplier_id, unit_price, reorder_level) VALUES
    ('EL-0001', 'Barcode Scanner ZX-9', 'Rugged 2D barcode scanner', 1, 1, 189.99, 10),
    ('EL-0002', 'RFID Label Roll (1000pcs)', 'UHF RFID labels for pallets', 1, 1, 74.50, 25),
    ('PK-0001', 'EUR Pallet', 'Standard 1200x800 wooden pallet', 2, 2, 12.90, 200),
    ('PK-0002', 'Stretch Film 23um', '500mm x 300m machine stretch film', 2, 2, 8.40, 150),
    ('PK-0003', 'Cardboard Box L', '600x400x400 double-wall box', 2, 2, 1.75, 500),
    ('RM-0001', 'Steel Sheet 2mm', 'Cold-rolled steel sheet 1250x2500', 3, 3, 96.00, 40),
    ('SP-0001', 'Forklift Fork Set', 'Class II forks, 1200mm', 4, 4, 420.00, 4),
    ('SP-0002', 'Conveyor Belt Roller', '76mm gravity roller, 600mm', 4, 4, 15.30, 60),
    ('CN-0001', 'Thermal Labels 100x150', 'Zebra-compatible, roll of 500', 5, 1, 6.20, 100),
    ('CN-0002', 'Packing Tape 48mm', 'Brown PP tape, 66m', 5, 2, 0.95, 300);

INSERT INTO stock_levels (product_id, warehouse_id, quantity) VALUES
    (1, 1, 24), (1, 2, 8),
    (2, 1, 60), (2, 3, 15),
    (3, 1, 420), (3, 2, 150), (3, 3, 90),
    (4, 1, 210), (4, 2, 95),
    (5, 1, 1200), (5, 3, 300),
    (6, 2, 55), (6, 3, 20),
    (7, 1, 3),
    (8, 2, 140), (8, 3, 35),
    (9, 1, 85), (9, 2, 40),
    (10, 1, 900), (10, 3, 120);

INSERT INTO stock_movements (product_id, warehouse_id, type, quantity, reference, note, created_by) VALUES
    (1, 1, 'IN', 24, 'PO-2026-0101', 'Initial delivery', 'system'),
    (3, 1, 'IN', 500, 'PO-2026-0102', 'Quarterly pallet order', 'system'),
    (3, 1, 'OUT', 80, 'SO-2026-0201', 'Shipment to client A', 'system'),
    (5, 1, 'IN', 1200, 'PO-2026-0103', 'Box restock', 'system'),
    (7, 1, 'OUT', 1, 'SO-2026-0202', 'Maintenance dept.', 'system'),
    (10, 1, 'IN', 1000, 'PO-2026-0104', 'Tape restock', 'system'),
    (10, 1, 'OUT', 100, 'SO-2026-0203', 'Packing line', 'system');
