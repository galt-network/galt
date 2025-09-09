CREATE TABLE invitations (
  id UUID PRIMARY KEY,
  inviting_member_id UUID REFERENCES members(id) NOT NULL,
  target_group_id UUID REFERENCES groups(id),
  content TEXT,
  created_at timestamp default current_timestamp,
  expires_at timestamp NOT NULL,
  max_usages INTEGER,
  current_usages INTEGER
);
--;;
CREATE TABLE invitation_requests (
  id SERIAL PRIMARY KEY,
  requesting_user_id UUID REFERENCES users(id) NOT NULL,
  target_member_id UUID REFERENCES members(id) NOT NULL,
  target_group_id UUID REFERENCES groups(id) NOT NULL,
  email TEXT NOT NULL,
  content TEXT NOT NULL,
  invitation_id UUID REFERENCES invitations(id),
  created_at timestamp default current_timestamp
);
