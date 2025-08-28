CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
--;;
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    pub_key TEXT,
    created_at timestamp default current_timestamp,
    deleted_at timestamp 
);
--;;
CREATE VIEW active_users AS SELECT * FROM users WHERE deleted_at IS NULL;
