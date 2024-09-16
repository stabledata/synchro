# Synchro

### The backend for [Stable](https://makeitstable.com). 

The best way to think about this service is as both a schema and data management API built on Postgres. 

Running `./gradlew migrate` (endpoint coming soon) creates an additional schema in your database with metadata which serves human (and future LLM) users more efficiently than JSON schema and custom-built UIs. 

Think about friendly messages for formatting, object relationships, views, joins, indexes, database chores, computations and more &mdash; expressed in easy to grok endpoints.
