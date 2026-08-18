UPDATE members SET avatar = regexp_replace(avatar, '^https?://[^/]+/files/', '') WHERE avatar LIKE '%/files/%';
--;;
UPDATE groups SET avatar = regexp_replace(avatar, '^https?://[^/]+/files/', '') WHERE avatar LIKE '%/files/%';
