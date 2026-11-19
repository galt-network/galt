# Connect search

Status: ready-for-agent

## Problem Statement

Connect is a public mixed feed of Hits. The search box looks like search, but a query is a single substring check on the name or title and the description. Word order matters, a typo misses, and a newer weak mention outranks the Member, Group, Event, or Post the person actually named. A specific Hit can also disappear because only a capped set of newer substring rows is kept, and that set is ordered by activity_at before relevance is considered.

A person opening Connect cannot find things by keywords. A Member cannot reliably look up a specific name or title. Near me, which is on by default, can hide a good match that sits outside 100 km and show an empty page with no explanation.

## Solution

A query matches the words on a Hit, in any order. English stems match. A name or title match outranks a body mention. A mistyped name or title still finds that Hit. activity_at only breaks ties while a query is present. An empty box is still the activity feed.

The page stays one text box plus the existing type checkboxes, date range, and Near me control. No new fields. If a query matches nothing inside Near me, Connect searches once more without Near me and says that it did. Each matching Hit shows a short snippet containing the matched words.

## User Stories

1. As a person who is not logged in, I want to type words into the Connect box and see Hits that contain those words, so that I can find Members, Groups, Events, and Posts without knowing how the page is organized.
2. As a person who is not logged in, I want the words to match in any order, so that a query like "meetup bitcoin" finds a Hit whose text says "Bitcoin Meetup".
3. As a person who is not logged in, I want an English stem to match the word I typed, so that "meetups" finds "Meetup".
4. As a person who is not logged in, I want matching to ignore case, so that capitalization does not hide a Hit.
5. As a person who is not logged in, I want a name or title match to rank above a Hit that only mentions the words in its description, so that the thing I named is not buried under a passing mention.
6. As a person who is not logged in, I want a snippet on each matching Hit that contains the matched words, so that I can see why it appeared.
7. As a person who is not logged in, I want an empty box to show a mixed feed of Hits ordered by activity_at, newest first, so that Connect is still a feed when I am not searching.
8. As a person who is not logged in, I want a blank or punctuation-only query to behave as an empty query, so that a clumsy search does not error and does not match every Hit.
9. As a Member, I want a search for a specific Member name to list that Member before a newer Post that only mentions the name in its body, so that I can open the profile I meant.
10. As a Member, I want a search for a specific Group name to list that Group before a newer weak mention, so that I can open the Group I meant.
11. As a Member, I want a search for a specific Event or Post title to list that Hit before a newer body mention, so that I can open the item I meant.
12. As a Member, I want an older exact name or title match to survive a large number of newer weak mentions, so that a cap on recent rows cannot hide the Hit I named.
13. As a Member, I want the first page to be the best matches, not the newest matches, so that paging does not start in the wrong place.
14. As a Member, I want further pages to continue that same order, so that infinite scroll does not reshuffle or restart the ranking.
15. As a person who is not logged in, I want a mistyped Member name to still return that Member, so that a typo does not yield nothing.
16. As a person who is not logged in, I want a mistyped Group name, Event name, or Post title to still return that Hit, so that a typo on a title still works.
17. As a Member, I want an exact or prefix name match to rank above a merely fuzzy name, so that the Hit I spelled correctly wins over a similar name.
18. As a person searching, I want a typo to be judged against names and titles, so that a fuzzy match does not depend on a similar string buried in a long description.
19. As a person searching, I want every word in a multi-word query to be satisfied, so that "bitcoin meetup" does not return a Hit that only says "bitcoin".
20. As a person searching, I want a multi-word query to succeed when one word is exact and another is a typo on the name or title, so that one misspelling does not discard an otherwise exact Hit.
21. As a person searching, I want to limit Hits to Members, Groups, Events, or Posts with the existing checkboxes, so that I can narrow the feed without a new form.
22. As a person searching, I want clearing every type checkbox to show all four kinds of Hit, so that I cannot empty the page by unchecking everything.
23. As a person searching, I want a date range to limit Events by start time and Posts by publish time, so that I can look at a period.
24. As a person searching, I want a date range to leave Members and Groups in the results, so that a named Member or Group is not hidden by a period chosen for Events and Posts.
25. As a person searching, I want the existing period choices to keep working, so that today, tomorrow, this week, this weekend, and next week still mean what they mean now.
26. As a person who is not logged in, I want Near me to limit Hits to 100 km of the city inferred from my IP, so that the default feed is local.
27. As a Member, I want Near me to use my Member location rather than my IP, so that my profile location wins.
28. As a User with no Member, I want Near me to use the IP city, so that a login without a profile still has a local radius.
29. As a person searching, I want a non-blank query that matches nothing inside Near me to be run again without Near me, so that a distant specific Hit is not silently hidden.
30. As a person searching, I want the page to say that Near me was dropped when that retry happens, so that I know the Hits are not local.
31. As a person searching, I want that notice even when the retry also finds nothing, so that I know the empty page is not caused by the radius.
32. As a person searching, I want an empty box with no local Hits to stay empty, so that Near me is not dropped just because the local feed is quiet.
33. As a person searching, I want Near me to stay on when the query already has local Hits, so that a successful local search is not widened.
34. As a person searching, I want to dismiss Near me myself, so that I can search everywhere on purpose.
35. As a person searching, I want to turn Near me back on, so that I can return to local Hits.
36. As a person searching, I want an explicit location I set to behave as Near me does, including the one retry when a query matches nothing there, so that a chosen place does not trap a specific search.
37. As a person searching, I want further pages of a search that dropped Near me to stay outside the radius, so that infinite scroll does not snap back to an empty local page.
38. As a person searching, I want each Hit to link to its Member, Group, Event, or Post, so that I can open it.
39. As a person searching, I want the type of each Hit to remain visible, so that I can tell a Member from a Group, Event, or Post.
40. As a person with no JavaScript, I want the Search button to submit the same query, so that the page still works without live updates.
41. As a person searching, I want hidden Posts and hidden Events left out, so that hidden items stay hidden.
42. As a person searching, I want a deleted Member left out, so that a removed profile is not a Hit.
43. As a person searching, I want a phrase that appears only in a comment to produce no Hit, so that Connect does not start returning comments.
44. As a person searching, I want changing the query to show the first page of the new search, so that an old offset does not skip the best matches.

## Implementation Decisions

- Connect search stays in the existing Connect module. No new search service, no vector index, and no model on the request path or on write.
- The behavioral module is the search use case. A command goes in. The result reports the ordered Hits and whether Near me was dropped. Callers that only read a bare list of Hits must be updated to that result.
- The repository remains the storage seam. It filters by type, period, location, and visibility, and it applies the text predicate. It must not discard a stronger text match in favor of a newer weak one before the use case ranks. The old cap of newer substring rows, ordered by activity_at, must not remain.
- The use case orders and pages. With a query, order is match quality, then activity_at newest first. A name or title match outranks a body-only match. An exact or prefix name or title match outranks a fuzzy-only match. With no query, order is activity_at only.
- Keyword match is whole words in any order, all required, case-insensitive, with English stemming. It is not a substring buried inside an unrelated word.
- Typo match applies to Member name, Group name, Event name, and Post title only. It does not apply to descriptions or Post bodies. Use the same trigram word-similarity idea already used for Member name lookup. Tests should use fixture pairs that clearly match and clearly miss, not a numeric threshold.
- A fuzzy token may satisfy one word of a multi-word query when the other words match normally. The typo still has to land on a name or title.
- Production storage implements keyword match with Postgres full-text search in the English configuration, and typo match with pg_trgm. Add the pg_trgm extension in a migration. Member and country lookup already call trigram functions, and the extension is not created by current migrations. Index the search documents and the name and title columns. Do not trigram-index descriptions or Post bodies.
- Searchable text on a Hit is its name or title and its description or content. Do not join location name, author name, Group membership, or comment text into the document.
- Store activity_at, or otherwise stop recomputing it with per-row comment and event lookups on the search path. The observable order above is the requirement. The stored timestamp is still the latest of creation and related comments or events.
- Exclude hidden Posts, hidden Events, and Members with a deletion timestamp. A Hit the repository marks hidden stays excluded.
- Near me stays a hard 100 km filter. Radius, Member location, IP city, and the loopback public-IP fallback stay as they are. If the command has a non-blank query and a location, and ranking produces no Hits, the use case searches once more without that location and reports that Near me was dropped. It does not retry again. An empty query never drops Near me. A caller who turned location off is not given a location back.
- Once Near me is dropped, later pages of that search keep the relaxed command, including live append. The notice stays visible.
- The date range still limits Event start time and Post publish time only. Members and Groups are undated. Period names are unchanged.
- The page keeps one text box, the four type checkboxes, the date range, and the Near me control. Do not add author, city, Group, or tag fields. The submit button stays.
- The handler renders the use-case result. It does not decide ranking or whether to drop Near me. It shows the snippet and the dropped-Near-me notice from that result. A snippet is required only when a query is present, and it must contain the matched words.
- A blank or punctuation-only query is treated as no query. The page does not error on punctuation.
- Connect stays public. This spec does not add a role requirement.

## Testing Decisions

- Test external behavior only: a command in, ordered Hits and a Near me-dropped flag out. Do not assert score formulas, index names, or generated SQL as the definition of relevance.
- The one behavioral seam is the search use case, with the in-memory repository as the stand-in, following the existing Connect search use-case tests. Those tests already cover mixed Hits, type filters, period, location, paging, and hidden Hits. Extend them for word order, stemming stand-in, title over body, typo on a name or title, exact over fuzzy, the activity_at tiebreak, no pre-rank loss of a strong older match, the Near me retry, and comments not appearing.
- The in-memory stand-in will not reproduce the Postgres English stemmer. Use fixture words whose stems obviously agree, and do not treat that stand-in as proof of stemmer parity.
- Handler tests stay render tests. Assert that a snippet and the dropped-Near-me notice from the use-case result appear on the page, and that the existing box, checkboxes, date range, and Near me control are still there. Do not re-test ranking in the handler.
- One adapter guard, beside the existing SQL-shape tests: the candidate query must not use substring `ILIKE` for the text predicate. Do not assert ranking by reading SQL text.
- Prior art is the existing Connect search use-case tests, the Connect page handler tests, and the database search repository row-mapping and SQL-shape tests.

## Out of Scope

- Comments as Hits, or searching comment text and returning the parent.
- A synonym list, thesaurus, or any other query expansion beyond English stemming and name or title typos.
- Putting location name, author name, or Group membership into the searchable document.
- Typo matching inside descriptions or Post bodies.
- Semantic search, embeddings, a model on the request, or a model that enriches text on write.
- A dedicated search engine.
- New form fields, a documented query syntax, and search-as-you-type.
- Non-English stemming or language detection.
- Changing the Near me radius, the date-range meaning for Members and Groups, or who may open Connect.
- Changing the database connection pool size.
- Invitations, map objects, or any entity that is not a Member, Group, Event, or Post.

## Further Notes

- The grill round that proposed these rules was not answered line by line. This spec locks the recommended answers from that round. A logged-out person is not given a new domain name here. The existing language still calls a login a User and a profile a Member.
- `CONTEXT.md` now says activity_at orders Hits only when Connect has no query. That used to say activity_at ordered Hits in general.
- The in-memory seam cannot prove that Postgres stemming matches the fixtures. The SQL guard only stops a silent return to substring search.
- pg_trgm may already be installed by hand on the production database. The migration should create it if it is missing.
- The pool defaults to 4 connections. That is a separate limit from this spec. Search should still return quickly for a handful of simultaneous searches once the pre-rank cap and the per-row activity lookups are gone.
- Connect has no minimum role. Search must keep excluding hidden and deleted rows because the page is public.
