# Synchro

### The backend for [Stable](https://makeitstable.com). 

The best way to think about this service is as both a schema and data management API built on Postgres. 

Running `./gradlew migrate` (endpoint coming soon) creates an additional schema in your database with metadata that serves human (and future LLM) users more efficiently than just a bunch of JSON schema and custom-built UI. 

Imagine standardized user-friendly messages for validation, formatting, object relationships, custom views, reporting, searching, database chores, performance optimizations, computations and more &mdash; all interfaced with easy to grok endpoints.
