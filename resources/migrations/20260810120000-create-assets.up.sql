CREATE TABLE assets (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  sha256 TEXT NOT NULL,
  content_type TEXT NOT NULL,
  size BIGINT NOT NULL,
  original_filename TEXT,
  storage_key TEXT NOT NULL UNIQUE,
  visibility TEXT NOT NULL CHECK (visibility IN ('public', 'private')),
  owner_member_id UUID REFERENCES members(id) ON DELETE SET NULL,
  module TEXT,
  created_at timestamp default current_timestamp
);
--;;
CREATE INDEX assets_sha256_idx ON assets (sha256);
--;;
CREATE INDEX assets_owner_member_id_idx ON assets (owner_member_id);
--;;
ALTER TABLE posts ADD COLUMN asset_ids jsonb;
--;;
ALTER TABLE events ADD COLUMN asset_ids jsonb;
