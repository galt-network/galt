CREATE TABLE posts (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  title TEXT NOT NULL,
  content TEXT NOT NULL,
  author_id UUID NOT NULL REFERENCES members(id),
  target_type VARCHAR(10),
  target_id UUID,
  comments_policy VARCHAR(10),
  hidden BOOLEAN default false,
  publish_at timestamp default current_timestamp,
  created_at timestamp default current_timestamp
)
