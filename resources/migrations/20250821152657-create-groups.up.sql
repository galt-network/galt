CREATE TABLE groups (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  name VARCHAR(255) NOT NULL,
  description TEXT NOT NULL,
  avatar TEXT,
  created_at timestamp default current_timestamp
);
--;;
CREATE TABLE members (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  name VARCHAR(255) NOT NULL,
  avatar TEXT,
  description TEXT,
  slug VARCHAR(64) UNIQUE,
  created_at timestamp default current_timestamp,
  deleted_at timestamp 
);
--;;
CREATE TABLE group_memberships (
  member_id UUID NOT NULL REFERENCES members(id) ON DELETE CASCADE,
  group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
  role VARCHAR(255) NOT NULL,
  created_at timestamp default current_timestamp,
  PRIMARY KEY (member_id, group_id)
);
