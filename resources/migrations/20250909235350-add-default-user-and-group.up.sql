INSERT INTO users (pub_key) VALUES ('this-is-not-a-pub-key');
--;;
INSERT INTO members (user_id, name) SELECT id, 'Galt System' FROM users WHERE pub_key = 'this-is-not-a-pub-key' LIMIT 1;
--;;
INSERT INTO groups (name, description) VALUES ('Galt', 'This is the default group');
--;;
INSERT INTO group_memberships (member_id, group_id, role)
  SELECT members.id, groups.id, 'founder' FROM members
  JOIN users ON users.id = members.user_id
  CROSS JOIN groups
  WHERE groups.name = 'Galt' AND groups.description = 'This is the default group' AND users.pub_key = 'this-is-not-a-pub-key'
  LIMIT 1;
