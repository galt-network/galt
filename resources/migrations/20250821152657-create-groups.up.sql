CREATE TABLE groups (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  name VARCHAR(255) NOT NULL,
  description TEXT NOT NULL,
  created_at timestamp default current_timestamp
);
--;;
CREATE TABLE group_memberships (
  member_id UUID NOT NULL,
  group_id UUID NOT NULL,
  role VARCHAR(255) NOT NULL,
  created_at timestamp default current_timestamp,
  PRIMARY KEY (member_id, group_id),
  FOREIGN KEY (member_id) REFERENCES users (id) ON DELETE CASCADE,
  FOREIGN KEY (group_id) REFERENCES groups (id) ON DELETE CASCADE
);
