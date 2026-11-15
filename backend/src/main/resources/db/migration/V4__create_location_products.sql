CREATE TABLE location_products (
    location_id BIGINT NOT NULL REFERENCES locations(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    PRIMARY KEY (location_id, product_id)
);