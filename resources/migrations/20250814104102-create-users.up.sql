CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
--;;
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    pub_key TEXT UNIQUE,
    created_at timestamp default current_timestamp,
    last_login timestamp,
    deleted_at timestamp 
);
