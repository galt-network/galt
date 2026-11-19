# 05: Near me steps aside when a query has no local Hits

Status: ready-for-agent
Implemented in 3bc6fc4.

Blocked by: 01, 03

Parent: `.scratch/connect-search/spec.md`

**What to build:** Near me stays a hard 100 km filter, on by default. Member location still beats the IP city. A User with no Member still uses the IP city. Loopback still uses the machine's public IP. If a non-blank query matches nothing inside that radius, Connect searches once more without the location and the page says Near me was dropped, even when that retry is also empty. It does not retry again. An empty box with no local Hits stays empty. A query that already has local Hits stays local. Dismissing Near me still searches everywhere, and turning it back on returns to local Hits. An explicit location the person set follows the same rule, including the one retry. Later pages of a search that dropped Near me stay outside the radius, including live append, and the notice stays visible. The page only shows what the search result reports. It does not decide to drop Near me.

- [x] Near me still limits Hits to 100 km
- [x] A Member's location is used instead of the IP city
- [x] A User with no Member uses the IP city
- [x] A non-blank query with no Hits inside the location is searched once more without that location
- [x] The page says Near me was dropped when that retry happens, including when the retry finds nothing
- [x] The search does not retry a second time
- [x] An empty query never drops Near me, even when the local feed is empty
- [x] A query that already has local Hits stays inside the radius
- [x] A person can dismiss Near me and search everywhere, and is not given a location back
- [x] A person can turn Near me back on
- [x] An explicit location follows the same one-retry rule
- [x] Further pages of a dropped search stay outside the radius, and the notice stays visible

**Notes:** Only the search use case changed: `ranked` fetches and ranks, and when a non-blank query with a location yields no Hits the use case searches once more with `:location` removed and reports `:near-me-dropped?`. The drop is decided on the full ranked set before paging, so a later page neither widens nor snaps back. The handler needed no change because ticket 01 already renders the flag and patches `#connect-notice` unconditionally, which is what keeps the notice visible on live append. No migration and no repository change. Checkboxes 1–3 and 9–11 are covered by pre-existing tests in `test/galt/locations/adapters/geoip_test.clj` and the handler geo tests; checkbox 5's page half came from ticket 01.
