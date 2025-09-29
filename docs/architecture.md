## Architecture

The architecture of this project is based on Robert C. Martin's (Uncle Bob) Clean Architecture.
![Clean architecture diagram](docs/clean-architecture.png)

### Folder structure

The folders (Clojure namespaces) under `/src` are organized into groups of functionality to follow the [screaming architecture](https://blog.cleancoder.com/uncle-bob/2011/09/30/Screaming-Architecture.html) approach.
The functionality group (e.g. invitations, members or payments) has:
1. `adapters` - which transform the data coming from external services (web, database, Nostr, Bitcoin Lightning, etc) into formats suitable for the pure domain layers - use cases and entities
2. `domain` - this is where core business logic resides. The functions here are pure - they can't have source code dependencies on outer layers whose functionality gets injected instead by [Donut Party System](https://github.com/donut-party/system)
  - `use cases`: coordinate the different domain entities and code the core business logic
  - `entities`: are pure data and functions. Can't have source dependencies on outer layers
3. `external`: is the code interfacing with external world (mapping for incoming web requests, database connections, connections to Bitcoin or Nostr, etc)

Most of these functionality groups have the following sub-divisions:
```
src/
└── galt
    ├── core
    │   ├── adapters
    │   │   ├── db_access.clj
    │   │   ├── handlers.clj
    │   │   ├── postgres_db_access.clj
    │   │   └── view_models.clj
    │   ├── infrastructure
    │   │   ├── bitcoin
    │   │   └── web
    │   │       ├── middleware.clj
    │   │       ├── routes.clj
    │   │       └── server.clj
    │   └── system.clj                  - setting up the dependency graph
    ├── groups
    │   ├── adapters
    │   │   ├── db_group_repository.clj
    │   │   ├── handlers.clj
    │   │   ├── view_models.clj
    │   │   └── views.clj
    │   ├── domain
    │   │   ├── entities
    │   │   │   ├── group.clj
    │   │   │   └── group_membership.clj
    │   │   ├── group_repository.clj
    │   │   └── use_cases
    │   │       ├── add_group.clj
    │   │       ├── ...
    │   │       └── update_group.clj
    │   └── external
    │       └── routes.clj              - Ring route definitions for this functionality group
    ├── invitations
    ├── locations
    ├── main.clj                        - the entrypoint to the application
    ├── members
    ├── payments
    └── posts
```

