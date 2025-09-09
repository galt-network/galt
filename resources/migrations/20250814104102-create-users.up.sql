CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
--;;
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    pub_key TEXT,
    created_at timestamp default current_timestamp,
    deleted_at timestamp 
);
