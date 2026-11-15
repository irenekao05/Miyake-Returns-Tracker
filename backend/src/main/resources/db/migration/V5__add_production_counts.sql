ALTER TABLE product_returns
RENAME TO daily_product_counts;

ALTER TABLE daily_product_counts
RENAME COLUMN quantity TO returned_quantity;

ALTER TABLE daily_product_counts
ALTER COLUMN returned_quantity DROP NOT NULL;

ALTER TABLE daily_product_counts
ADD COLUMN produced_quantity INTEGER
    CHECK (produced_quantity >= 0);