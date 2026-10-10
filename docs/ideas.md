# What Galt is (and what it is not)

## What it is not

**Galt is not a replacement for X/Twitter** because its main functionality isn't short messages for continuous consumption.

**Galt is not a replacement for messaging programs like Signal/Telegram/WhatsApp** because these applications are already working very well and have highly advanced functionality and infrastructure. No reason to try to compete with them or to re-invent the wheel.

**Galt is not an online newspaper or TV station** because these need high volume of original content and there are no journalists or reporters on salary. The usefulness and cost to develop this functionality (both in human and productctive capital) doesn't make it worth (at least for now) to implement this.

## What it is

**Galt is a place to make connections with other libretarians.**
It provides easy way to find other libertarians who are near or not so near to the user. On other platforms there are all types of people, which makes it harder to find them. Galt makes organizing and keeping up to date of the doings your libertarian friends easy.

**Galt is a focused source of information about libertarianism**
It is building up a growing set of materials (original articles, downloadable books, links to external materials like videos, books or articles) for people who are at at different places in their libertarian journey. From beginners who just found out (or are about to find out) about the fundamentals (NAP, private property), intermediates (who have read couple of books and have a solid understanding) to advanced and professionals (who have studied it profoundly and/or made contributions to the field).

Galt offers all these types of users something useful and interesting. Beginners can learn new concepts. Intermediates can deepen their knowledge and clarify their doubts about certain concepts and situations. Advanced users can guide others and publish new materials.

**Galt is a community that creates, maintains and shares tools to advance the libertarian cause.**
Tools like deBS, FreedomRadar and more in the future (created by the community of contributors)

**Galt is a platform to organize events and share information.** One thing is to connect virtually with someone, another is to actually meet up and have interesting conversations with them. The people with more knowledge and initiative can set up local groups and events. Events can be organized by one or more individuals or groups. Galt gives tools for them to manage the RSVP and receive donations (some basic bitcoin infrastructure).

### In short
1. Connect
2. Learn
4. World map

# Main functionality for release

1. Welcome page
  - tells the story (addressed to each type of user)
  - each story is accompanied or followed by something interactive or informative
2. Find, connect & stay in touch
  - search users & groups and navigate into them
  - befriend (add to your friends list, show double-link if adds back)
3. Feedom radar (could be part / in format of interactive World Map)
  - imported headlines of freedom related news (regulation, privacy, taxes,
    residency)
  - imported tweets & Nostr posts
  - users can _put something on the radar_ (crawler gets it, analyzes and
    summarizes if passes the relevancy evaluation)
4. World map
  - see users on-line
  - leave them messages
  - claim tiles, exchange them, add objects
5. ***Learn*
  - section where the most important topics and concepts are covered
  - refers to other libertarian authors (YouTube) and sources (Mises Institute)
  - is available in multiple languages
  - translation on demand via AI translator of the original English article /
    text
  - stored in the DB or disk structure (as Markdown files in nested folders)
    - needs metadata to record last synchronization or update (compare with
      original)
  - should have some kind of manual editing capacity
    + original article can be in any language (translation-role: original-language)
    + enables humans to post and edit articles in their own language
    + also means users must have permissions (original author should be able to
      change the origial language text, the rest gets updated automatically on first read or pre-generated)
    + these can be left for future implementation (multiple user editing
      on-line)
  - MVP can have file upload via Github pull request and the metadata states
    translation-role (which is the original language)
    + alternatively the specific language files can be prefixed or extension
      based
    + it could even be HTML with images (one folder per article)?
    + depends how varied the content is (e.g. if Markdown can do it well enough
      or per-page customization is needed)
    + might be quite useful to create small interactive snippets with Datastar
      or Scittle in some lessons
  - maybe better a hybrid solution:
    + during startup, the articles are "registered" or "loaded"
    + sources can be functions in a namespace that return HTML
      (interactive via datastar or Scittle, could even use server resources)
    + or generated functions that return rendered Markdown (translated on
      demand). Buffered in memory to avoid repeated disk access


## Additional functionality for the future

### Rewards
1. for inviting new users
  - https://galt.is/join
  - every new member through user X's invitation adds points to that user (will get rewards for it)

# New modules and pages to implement

1. Tools sections
2. deBS
3. Libertarian (or Galt) Radar
  - finds libertarian accounts and communities on social networks
  - groups and orders them
  - provides a stream of their content
4. Libertarian (or Galt) RAG
  - builder: ingresses books
  - MCP server: for integration with AI tools
  - API

# TODO priorities

1. Implement libertarian signals import to display live activity
  - Twitter
  - Nostr
  - integrate them to the welcome page live feed
  - integrate them to the map as dynamic messages
  - https://grok.com/c/2a396355-7b45-4614-9c4c-9e5c16c34c7b?rid=1fe8677a-d0a3-44ab-abea-c2f2f9433def
2. Redesign Members, Groups, Posts, Events pages
  - style should be more uniform
  - look for examples on existing social networks and similar web pages
  - implement useful search & filtering (what way users want can can find other
    users most easily?)
  - better content editing + image uploading (Easy Markdown)
  - https://grok.com/share/bGVnYWN5LWNvcHk_fb4a22c5-53aa-4b0e-b078-57b561a83a0d
3. Review user registration process
  - what can regular visitor, user and member do?
  - update the UI or documentation to make the benefits clear
4. Coherent map objects handling
  - objects ownership
  - object stocks and permissions (who can place what objects where)
  - tighter integration between map and rest of Galt data
5. **STARTED** Assets storage
  - S3 style but on CloudFlare
  - https://grok.com/share/bGVnYWN5LWNvcHk_c0020450-7305-4594-b027-3df6a323a828
6. Telemetry signals, storage and monitoring
7. AI Security audit
  - https://grok.com/share/bGVnYWN5LWNvcHk_bf4b78a9-aa73-4b74-b444-cd1b1912cdaa
8. Payment system
  - accept Bitcoin, Bitcoin Lightning and USDC
  - do initial simple use cases: membership payment, exclusive object purchasing
9. Deployment, packaging and always-online


## Braindump

Most users who visit the site are probably passive (lurkers)
  - they should always find something interesting when they come back
  - they should be able to interact with other users and the content and it
    should be fun
  - once they familiarize with the website, maybe they'll become contributers
  - SOLUTION: import from external sources
Design the architecture and visuals so that once it goes live, feedback can be applied quickly
  - build in ways to give feedback and invite people to use it
  - feedback ends up as Github issues
  - feedback should also be able to be sent via messages (directly from within
    Galt)

### How to keep the users coming back

1. Updates from external sources (Twitter mainly)
2. Activities on the map
  - users get new objects daily that they can drop on the map
  - these objects can be transferred
  - daily object price probably will end up low as there's enough supply
  - users can buy up adjacent hexagons as property on the map
  - then they can place objects on their land
  - they can also sell their land
3. Badges
  - membership length (small stars for months, big stars for years)
  - membership activity (based on number of posts and events)
  - land ownership (based on hexagons owned)

## Land ownership



## Map functionality

Messages are unique that users can leave for others.
On the map they can be as signposts or as messageboard.
  - 3D signposts on an interactive 3D map
  - though development time could be difficult
Users can put signs (messages) on the map.
Non-logged in users' messages stay up for 1 hour.
Logged-in user's messages stay up for 1 day.
When at least 3 logged in users unite, they can create a group for free.
  - when (a,b,c) have created one group for free, they can create another when
    the "founders" set is different, meaning they change out one user or invite fourth
Groups can have one permanent clubhouse (headquarters?) on the map.
  - messages posted on clubhouse, stay there as long as the owners want (auto
    expiry, manual deletion, voting, etc).

The world is very big and also because in the ancap world the property borders are different.
So putting objects on the map or displaying users or their messages on the map should not attempt
scale of reality.
Find the scale of object by assuming for example how big should person of 1.8m be for me to see him over the curvature of earth including his toes (3cm from the ground) if he his 300 km away.
