# 03: Keyword search

Status: ready-for-agent
Implemented in 6b82349.

Blocked by: 01, 02

Parent: `.scratch/connect-search/spec.md`

**What to build:** A person types words into the Connect box and sees Hits that contain those words, in any order. English stems match. Case does not matter. A name or title match ranks above a body mention. activity_at only breaks ties. An empty, blank, or punctuation-only box is still the activity feed, and it does not error or match every Hit. The first page is the best matches, and later pages continue that order. An older exact Hit is not dropped because newer weak mentions filled a cap. Each matching Hit shows a snippet that contains the matched words. The existing type checkboxes, date range, and period choices keep their current meaning. Searchable text is the name or title and the description or content only.

- [x] "meetup bitcoin" finds a Hit whose text says "Bitcoin Meetup"
- [x] An English stem matches the typed word, using fixture words whose stems obviously agree. The in-memory stand-in is not proof of Postgres stemmer parity
- [x] Matching ignores case
- [x] A word does not match as a substring buried inside an unrelated word
- [x] Every word is required. "bitcoin meetup" does not return a Hit that only says "bitcoin"
- [x] A name or title match ranks above a Hit that only mentions the words in its description or content
- [x] A specific Member, Group, Event, or Post name or title ranks above a newer body mention of that name
- [x] With a query, activity_at only breaks ties, newest first
- [x] With no query, order is activity_at only
- [x] A blank or punctuation-only query behaves as no query
- [x] The first page is the best matches, not the newest matches
- [x] Further pages continue that same order and do not reshuffle
- [x] Changing the query shows the first page of the new search
- [x] An older exact name or title match survives a large number of newer weak mentions. The old cap of newer substring rows must not remain
- [x] When a query is present, each matching Hit shows a snippet containing the matched words. A snippet is not required when there is no query
- [x] Type checkboxes still limit Hits, and clearing every checkbox shows all four kinds
- [x] A date range still limits Events by start time and Posts by publish time, and leaves Members and Groups in the results
- [x] Today, tomorrow, this week, this weekend, and next week still mean what they mean now
- [x] A phrase that appears only in a comment produces no Hit
- [x] Location name, author name, and Group membership are not part of the searchable text
- [x] The production text predicate is not a substring `ILIKE`. Do not treat generated SQL as the definition of ranking

Notes:
- Ranking lives in the use case; the repository annotates each candidate with `:match` (`:title`/`:body`) and `:snippet`, so storage keeps its stemming knowledge. Postgres uses `to_tsvector('english', ...) @@ plainto_tsquery('english', ...)`, a title-only predicate for `:title_match`, and `ts_headline` for the snippet.
- The 200-row pre-rank cap is gone; the no-query feed now fetches every row and sorts in memory. See the `shortcut:` comment in `candidates-sql`.
- The migration adds `pg_trgm` plus the trigram indexes that ticket 04 needs, as the spec requires; FTS uses only the four GIN full-text indexes.
- Judgement call: `ts_headline` defaults (`MaxWords=35`) can omit one matched word when the matched terms sit far apart in a long document. Accepted as inherent to a short snippet.
