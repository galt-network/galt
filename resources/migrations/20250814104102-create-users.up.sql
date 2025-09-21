CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
--;;
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    pub_key TEXT UNIQUE,
    created_at timestamp default current_timestamp,
    last_login timestamp,
    deleted_at timestamp 
);
