# GALT

A membership network for people, groups, events, and posts.

## Language

**Member**:
A public profile of a person in the network.
_Avoid_: User (login identity), account

**User**:
A login identity (session / pubkey). Distinct from Member.

**Group**:
A named community of Members.

**Event**:
A scheduled gathering, live or online.

**Post**:
Published writing, usually attached to a Group.

**Hit**:
One Connect-feed row for a Member, Group, Event, or Post.

**Connect**:
A mixed feed of Hits searchable from one place.

**activity_at**:
The latest of a Hit's creation and related comments or events. With no query, it orders Hits. With a query, it only breaks ties.

**Near me**:
A 100 km radius around the searcher. Uses the Member's location when present, otherwise the city inferred from the request IP. Loopback requests use the machine's public IP.

## Relationships

- A **User** may have one **Member**
- A **Member** may belong to many **Groups**
- An **Event** and a **Post** may have comments
- A **Post** inherits location from its **Group**
- **Connect** returns **Hits** drawn from Members, Groups, Events, and Posts

## Example dialogue

> **Dev:** "Does the searcher filter Users or Members?"
> **Domain expert:** "Members. User is only the login. Connect lists Members, Groups, Events, and Posts as Hits."

## Flagged ambiguities

- Spec said "user" for a searchable person — resolved: that is a **Member**.
- `activity_at` ordered every Hit — resolved: it orders the feed only when Connect has no query.
