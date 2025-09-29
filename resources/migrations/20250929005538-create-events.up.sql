CREATE TABLE events (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  name TEXT NOT NULL,
  description TEXT NOT NULL,
  start_time timestamp NOT NULL,
  end_time timestamp default current_timestamp,
  author_id UUID NOT NULL REFERENCES members(id),
  group_id UUID REFERENCES groups(id),
  location_id INTEGER REFERENCES locations(id),
  type VARCHAR(10),
  comments_policy VARCHAR(10),
  hidden BOOLEAN default false,
  publish_at timestamp default current_timestamp,
  created_at timestamp default current_timestamp
);
