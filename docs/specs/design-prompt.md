## General page description

I want you to design a web application for GALT (Global Alliance for Libertarian Transformation).
It is a web page that teaches people about Bitcoin, libertarian philosophy (private property, non-aggression principle, voluntary cooperation) and Austrian-economics. It also allows fellow-libertarians (users of GALT platform) to find other freedom-minded people around them to connect and find ways to interchange ideas, organize events and convince more people to join the path of liberty and prosperity.

## Users and goals

There are main 2 types of users who visit the site:
1. wonderer - this user arrives to the page through a link on social media or web search and has no idea what the page is about. When this user arrives, they should get a quick understanding what the page is about and be able to playfully explore it. Their experience should be interactive and memorable.
2. member - they already have an account (paid in Bitcoin) and they return to the page either to check out events or to see who else is connected or to reference some article that we posted or to comment existing articles and events.

## Product surface

There are

## Layout and structure

The web application should be mobile-first as most of the users today use internet through their phones.
There are 2 main sections: Connect and Learn. Other two sections are Profile (where the user can see the messages other users have sent them and edit their profile) and About (that describes what the GALT plaform is about).

The Connect page has two main views that should be easily switchable:
  - map view that displays a map of globe with real time data about online users
  - scrollable list view that shows searchable, sortable and filterable list of users,
    groups and events (the same ones that are shown on the map)

The Learn page consists of a grid of articles. Each article is text and a short explanatory video. After the article there's a test (user clicks "Take a test") for the user to check their understanding of the explained topic.

## Aesthetic / taste

GALT membership provides access to an exclusive club of libertarian thinkers and entrepreneurs. The page design should express quality, tradition and prestige. The text should be easily readable on mobile phones. The color scheme should be inspired by anarcho-capitalist colors (black #000000 representing anarchism, yellow #FDFD00 representing gold).

## Constraints

GALT will mostly be used on mobile phones but it should also support desktop and tablets.
It will be developed as progressive web application (PWA) first but possibly in the future as a native mobile app. Server-sent events (SSE) and datastar (https://data-star.dev/) will be used to avoid front-end JS frameworks in order to keep the development simpler.
