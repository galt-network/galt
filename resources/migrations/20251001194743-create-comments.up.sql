CREATE TABLE comments (
  id SERIAL PRIMARY KEY,
  parent_id INTEGER REFERENCES comments(id),
  content TEXT NOT NULL,
  author_id UUID NOT NULL REFERENCES members(id),
  created_at timestamp default current_timestamp
);
--;;
CREATE TABLE post_comments (
  post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
  comment_id INTEGER NOT NULL REFERENCES comments(id) ON DELETE CASCADE
);
--;;
CREATE TABLE event_comments (
  event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
  comment_id INTEGER NOT NULL REFERENCES comments(id) ON DELETE CASCADE 
);
