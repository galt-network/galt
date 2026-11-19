# 01: Search result can carry a snippet and a Near me notice

Status: ready-for-agent
Implemented in d33d2a8.

Blocked by: None

Parent: `.scratch/connect-search/spec.md`

**What to build:** Connect still searches as it does today. The search result can include a snippet on each Hit and whether Near me was dropped, and the page shows both when the result sets them. Nothing sets them yet. Callers that only read a bare list of Hits must read this result instead. The page does not decide ranking or whether to drop Near me.

- [x] A search command returns ordered Hits and whether Near me was dropped
- [x] Near me was dropped is false for today's searches
- [x] When a Hit in the result has a snippet, the card shows that snippet
- [x] When the result says Near me was dropped, the page says so, including when the Hit list is empty
- [x] When the result does not say Near me was dropped, the page does not say so
- [x] The page still has one text box, the four type checkboxes, the date range, the Near me control, and the Search button
- [x] Each Hit still shows its type and still links to that Member, Group, Event, or Post
- [x] Existing Connect search behavior is unchanged: substring match, activity_at order, type filters, period, location, paging, and hidden Hits
