# 02: activity_at is stored, and a deleted Member is not a Hit

Status: ready-for-agent
Implemented in 8a222c3.

Implemented via the spec's alternative to a stored column: activity_at is aggregated
per search with grouped LEFT JOINs (`latest-created` + `with-joins` in
`db_search_repository.clj`), so the per-row correlated subqueries are gone and no
migration was added. A Member is also excluded when its login User is soft-deleted
(`users.deleted_at IS NULL`), in addition to `members.deleted_at IS NULL`.

Blocked by: None

Parent: `.scratch/connect-search/spec.md`

**What to build:** An empty Connect box is still a mixed feed of Hits, newest activity first. activity_at is stored, and it is still the latest of the Hit's creation and the related activity Connect already counts. A new comment or event moves that Hit up without a per-row lookup on the search path. A deleted Member does not appear. Hidden Posts and hidden Events stay hidden.

activity_at stays what it is today:

- A Member: latest of the Member's creation, events that Member authored, and comments that Member authored
- A Group: latest of the Group's creation, its Events, Posts targeted at that Group, and comments on those Events and Posts
- An Event: latest of the Event's creation and comments on that Event
- A Post: latest of the Post's creation, its publish time, and comments on that Post

- [x] With no query, Hits are ordered by activity_at, newest first
- [x] A new related comment or event moves that Hit up in the empty feed
- [x] Search does not recompute activity_at with a per-row lookup
- [x] A deleted Member does not appear in the feed or in search
- [x] A hidden Post or hidden Event does not appear
- [x] A comment is not a Hit
