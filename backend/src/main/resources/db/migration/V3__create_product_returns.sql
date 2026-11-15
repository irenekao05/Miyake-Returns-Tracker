CREATE TABLE product_returns (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    location_id BIGINT NOT NULL REFERENCES locations(id),
    business_date DATE NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity >= 0),
    UNIQUE (product_id, location_id, business_date)
);