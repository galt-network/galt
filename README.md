# Galt Promo

A single-page ClojureScript presence demo. A Macchiato server streams user snapshots over SSE, and a re-frame/Bulma UI renders each user's generated alliterative name, browser, IP address, and connection status.

Disconnected users remain visible with a gray status dot for 10 seconds, then disappear. Browser tabs use a persistent local-storage client ID so an SSE reconnect keeps the same name.

## Setup

```sh
npm install
```

## Develop

Start the shadow-cljs watches:

```sh
npm run dev
```

In another terminal, run the server:

```sh
npm start
```

Open <http://localhost:3000>. Open additional isolated browser windows or profiles to see multiple users. Set `PORT` to change the server port.

The UI watch rebuilds `resources/public/js/main.js`; the server watch rebuilds `target/server.js`. Restart `npm start` after server changes.

## REPL

Start a browser-connected ClojureScript REPL while the UI watch and app are running:

```sh
npx shadow-cljs cljs-repl ui
```

For the server build:

```sh
npx shadow-cljs cljs-repl server
```

Editors can instead connect to the shadow-cljs nREPL server and select the `ui` or `server` build. Use `shadow-cljs watch` output for the nREPL port when it is chosen dynamically.

## Test and build

```sh
npm test
npm run build
npm start
```

Tests cover name generation, the 10-second presence boundary, live HTTP/SSE connect and disconnect behavior, the index route, and re-frame state/subscriptions. `npm run build` produces release builds for both application targets.

## Edit names

The bundled adjective/noun dictionary is in `src/galt_promo/names.cljs`. Names are unique among users currently connected or in their disconnect grace period.
