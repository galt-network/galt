-- Index on countries table
CREATE INDEX IF NOT EXISTS idx_countries_name ON countries (name);
--;;
CREATE INDEX IF NOT EXISTS idx_countries_iso2 ON countries (iso2);
--;;
-- Index on cities table
CREATE INDEX IF NOT EXISTS idx_cities_name ON cities (name);
--;;
CREATE INDEX IF NOT EXISTS idx_cities_country_code ON cities (country_code);
--;;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
--;;
CREATE INDEX IF NOT EXISTS idx_countries_lower_name_trgm ON countries USING gin (lower(name) gin_trgm_ops);
--;;
CREATE INDEX IF NOT EXISTS idx_cities_lower_name_trgm ON cities USING gin (lower(name) gin_trgm_ops);
