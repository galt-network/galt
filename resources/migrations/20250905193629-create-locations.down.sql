ALTER TABLE groups DROP COLUMN location_id;
--;;
ALTER TABLE members DROP COLUMN location_id;
--;;
DROP TABLE locations;
--;;
DROP EXTENSION IF EXISTS earthdistance;
--;;
DROP EXTENSION IF EXISTS cube;
