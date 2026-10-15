CREATE EXTENSION IF NOT EXISTS pg_trgm;
--;;
-- the tsvector expression must stay identical to doc-text in
-- src/galt/connect/adapters/db_search_repository.clj, or the planner ignores it.
CREATE INDEX IF NOT EXISTS members_search_idx ON members USING GIN (to_tsvector('english', COALESCE(name, '') || ' ' || COALESCE(description, '')));
--;;
CREATE INDEX IF NOT EXISTS groups_search_idx ON groups USING GIN (to_tsvector('english', COALESCE(name, '') || ' ' || COALESCE(description, '')));
--;;
CREATE INDEX IF NOT EXISTS events_search_idx ON events USING GIN (to_tsvector('english', COALESCE(name, '') || ' ' || COALESCE(description, '')));
--;;
CREATE INDEX IF NOT EXISTS posts_search_idx ON posts USING GIN (to_tsvector('english', COALESCE(title, '') || ' ' || COALESCE(content, '')));
--;;
CREATE INDEX IF NOT EXISTS members_name_trgm_idx ON members USING GIN (name gin_trgm_ops);
--;;
CREATE INDEX IF NOT EXISTS groups_name_trgm_idx ON groups USING GIN (name gin_trgm_ops);
--;;
CREATE INDEX IF NOT EXISTS events_name_trgm_idx ON events USING GIN (name gin_trgm_ops);
--;;
CREATE INDEX IF NOT EXISTS posts_title_trgm_idx ON posts USING GIN (title gin_trgm_ops);
