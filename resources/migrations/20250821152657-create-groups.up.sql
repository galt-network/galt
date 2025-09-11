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
  user_id UUID,
  name VARCHAR(255) NOT NULL,
  avatar TEXT,
  slug VARCHAR(64),
  created_at timestamp default current_timestamp,
  deleted_at timestamp 
);
--;;
CREATE TABLE group_memberships (
  member_id UUID NOT NULL,
  group_id UUID NOT NULL,
  role VARCHAR(255) NOT NULL,
  created_at timestamp default current_timestamp,
  PRIMARY KEY (member_id, group_id),
  FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE,
  FOREIGN KEY (group_id) REFERENCES groups (id) ON DELETE CASCADE
);
