CREATE TABLE members (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  avatar TEXT,
  slug VARCHAR(64),
  created_at timestamp default current_timestamp
);
--;;
CREATE TABLE galt_memberships (
  user_id UUID REFERENCES users(id) NOT NULL,
  member_id UUID REFERENCES members(id) NOT NULL,
  PRIMARY KEY (user_id, member_id)
);
--;;
CREATE TABLE purchases (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID REFERENCES users(id) NOT NULL,
  "type" VARCHAR(16) NOT NULL,
  amount NUMERIC(14,3),
  unit VARCHAR(16) NOT NULL,
  completed_at timestamp,
  created_at timestamp default current_timestamp
);
--;;
CREATE INDEX purchases_type_idx ON purchases ("type");
--;;
CREATE INDEX purchases_user_id_idx ON purchases (user_id);
--;;
CREATE TABLE gifts (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID REFERENCES users(id),
  "type" VARCHAR(16) NOT NULL,
  created_at timestamp default current_timestamp,
  expires_at timestamp NOT NULL
);
--;;
CREATE INDEX gifts_user_id_idx ON gifts (user_id);
