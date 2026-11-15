CREATE TABLE locations (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    marketplace_name TEXT NOT NULL,
    location_name TEXT NOT NULL,
    UNIQUE (marketplace_name, location_name)
);