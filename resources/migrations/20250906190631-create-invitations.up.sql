CREATE TABLE invitations (
  id UUID PRIMARY KEY,
  inviting_member_id UUID NOT NULL,
  target_group_id UUID NOT NULL,
  content TEXT,
  created_at timestamp default current_timestamp,
  expires_at timestamp NOT NULL,
  max_usages INTEGER,
  current_usages INTEGER default 0,
  FOREIGN KEY (inviting_member_id) REFERENCES members(id) ON DELETE CASCADE,
  FOREIGN KEY (target_group_id) REFERENCES groups(id) ON DELETE CASCADE
);
