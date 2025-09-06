CREATE EXTENSION IF NOT EXISTS cube;
--;;
CREATE EXTENSION IF NOT EXISTS earthdistance;
--;;
CREATE TABLE locations (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    country_code character(2),
    city_id bigint
);
--;;
ALTER TABLE groups ADD COLUMN location_id INTEGER;
--;;
ALTER TABLE groups ADD CONSTRAINT fk_groups_location_id FOREIGN KEY (location_id) REFERENCES locations (id);
--;;
ALTER TABLE members ADD COLUMN location_id INTEGER;
--;;
ALTER TABLE members ADD CONSTRAINT fk_members_location_id FOREIGN KEY (location_id) REFERENCES locations (id);
