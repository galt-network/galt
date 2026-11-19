# 04: Typo on a name or title

Status: ready-for-agent
Implemented in d154559.

Notes: a typo path applies only to query words of length >= 5 (`typo-min-length`), with edit distance <= 2 in the in-memory stand-in and the pg_trgm `<%` operator (default 0.6 threshold) in SQL; the length guard is needed because the threshold alone cannot separate a real typo from noise. Ranking gained a middle tier: `:title` (every query word in the name/title) > `:title-fuzzy` (some query word matches the name/title exactly or fuzzily) > `:body`. Fuzzy comparison is applied to name/title only. The text predicate is now an AND of per-word ORs, with a `cast(plainto_tsquery(...) as text) = ''` disjunct so a stopword inside a longer query is ignored, plus a whole-query lexeme guard so a stopword-only query matches nothing (as in 03). Prefix matching was deliberately not added: it would relax 03's "a word does not match as a substring buried inside an unrelated word". No migration was needed; 03 created pg_trgm and the four trigram indexes.

- [x] A mistyped Member name still returns that Member
- [x] A mistyped Group name, Event name, or Post title still returns that Hit
- [x] An exact or prefix name or title match ranks above a fuzzy-only match
- [x] A similar string in a description or Post body does not satisfy a typo
- [x] A fuzzy token may satisfy one word of a multi-word query when the other words match, and that typo still has to land on a name or title
- [x] Tests use fixture pairs that clearly match and clearly miss, not a numeric threshold
- [x] Name and title columns are indexed for trigram match. Descriptions and Post bodies are not
