-- Storage bin locations (inspired by InvenTree's stock locations) and
-- per-warehouse reorder rules (inspired by Odoo's reordering rules)

ALTER TABLE stock_levels ADD COLUMN bin VARCHAR(64);
ALTER TABLE stock_levels ADD COLUMN min_quantity INT;
ALTER TABLE stock_levels ADD COLUMN max_quantity INT;
