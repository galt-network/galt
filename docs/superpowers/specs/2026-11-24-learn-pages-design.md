# Learn Pages in the New Design

Status: ready-for-human

## Problem Statement

GALT needs a Learn section: a curriculum page that lists modules and lessons, and a page for each lesson. The design exists only as Google Stitch screens (project `10058067017971123608`, exported to `docs/design/learn/`). It is built with Tailwind CSS, while the running site uses Bulma. Two earlier attempts imported the whole design in one large change (local branches `new-design`, `new-design-two`) and both were abandoned. We need a small first step that puts the two pages online and can be checked after every change.

## Solution

Add two public pages in a new, self-contained Tailwind v4 layout: `/learn` (curriculum) and `/learn/:module-slug/:lesson-slug` (lesson). Content comes from a static EDN file. Existing Bulma pages are not changed, except for one new link in their navigation bar.

## User Stories

1. As a visitor, I want to open Learn from the navigation bar, so that I can find the curriculum.
2. As a visitor, I want to see the modules with their lessons and status, so that I know what to study next.
3. As a visitor, I want to open a lesson, so that I can read it.
4. As a reader, I want a link to the next lesson and back to the curriculum, so that I can keep going.
5. As a member, I want to see my name and a link to my profile on these pages; as an anonymous visitor, I want a link to log in.

## Implementation Decisions

1. **Source of truth**: the HTML exports in `docs/design/learn/` (`learn-mobile.html`, `learn-desktop.html`, `lesson.html`) and `docs/design/learn/tailwind.css` (design tokens). One responsive template per page: mobile markup sits in `<div class="md:hidden">`, desktop markup in `<div class="hidden md:block">`.
2. **Tailwind**: v4.3.5, compiled by `@tailwindcss/cli` from `resources/css/tailwind.css` (a copy of the reference file) to `resources/public/css/tailwind.css`. The generated file is not committed. Dark theme only. Both design screens use the same type scale tokens with different sizes; we use the larger "Learn" scale everywhere.
3. **New layout only for new pages**: `galt.core.views.new-layout` and `web-helpers/with-new-layout`. `main-layout` and every Bulma page stay as they are.
4. **Navigation**: the new header and tab bar link only to pages that exist today: brand to `/`, Connect, Learn, Events, and the profile or login link. No About, search field, theme toggle or leaderboard.
5. **Content is static**: `resources/learn/curriculum.edn` holds 3 modules and 9 lessons. The text of "The Subjective Theory of Value" is ported to Hiccup; other lessons show "Lesson text coming soon." No database table and no repository protocol yet.
6. **Progress is placeholder**: rank, OBJ points, streak and exam accuracy are fixed text copied from the design. Chip counts and "done / total" per module are computed from the lesson statuses. Locked lessons are not links.
7. **Routes are public** (no `:min-role`).
8. **Images and assets**: lesson artwork is served from local files under `/assets/images/learn/`. Fonts and icons load from Google Fonts. Datastar 1.0.2 drives the notification toast. No FontAwesome, scittle or `lh3.googleusercontent.com` on new pages.

## Testing Decisions

- Pages are rendered through the real Reitit router with stub repositories. Tests assert status codes, text and `href` values in the HTML string; class names are asserted only for the two responsive wrappers.
- The curriculum file is tested as data (unique slugs and titles, required keys, lesson lookup).
- Visual fidelity is checked by a human at 390 px and 1280 px against the Stitch screens. The agent only checks that the pages return 200 and contain the expected markup.

## Out of Scope

Light theme, stored progress, exams and points, working search and filters, audio narration, login-gated content, migrating other pages to the new layout, self-hosted fonts, PWA features.

## Further Notes

- Implementation plan: `docs/superpowers/plans/2026-11-24-learn-pages.md`.
- Screenshots of the design are visible only in the Stitch UI (they are too large for git).
- The mock's own numbers are inconsistent (for example "Completed (4)" next to three completed lessons). Counts shown on the page are derived from the data; only the member statistics in the hero are copied verbatim.
- Decisions 1, 2, 4, 5 and 7 were picked by the planner as defaults and are not confirmed by the owner yet. Flip the status to `ready-for-agent` once they are.
