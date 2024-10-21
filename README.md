# Synchro

### The backend for [Stable](https://makeitstable.com). 

The best way to think about this service is as both a schema and data management API built on [Exposed](https://github.com/JetBrains/Exposed). 

> Imagine standardized user-friendly messages for validation, formatting, object relationships, custom views, reporting, searching, database chores, performance optimizations, computations and more &mdash; all interfaced with easy to grok endpoints.

## Build & Run

Running `./gradlew migrate` or hitting the `/migrate` endpoint creates an additional schema in your database with metadata that serves human (and future LLM) users more efficiently than just a bunch of JSON schema and custom-built UI.


## Architecture

Synchro is a **purposefully dead simple ktor web service** with HTTP and gRPC endpoints however, there are a few noteworthy features:

### Writes are always idempotent

All write requests, which are entirely HTTP POST or gRPC are enveloped with a required `x-stable-event-id` header which serves as an idempotency guarantee.

As part of request contextualization, the logs table is checked to ensure events have not been processed.

### Write workloads are done in a single transaction 

#### ... including logging in the database and broadcasting event confirmation

All write endpoints process the following operations in an ACID compliant transaction: 

- Perform any db operations necessary (e.g. CREATE, ALTER , DROP)
- Perform any db writes to schema or data tables
- Write the log entry with event id
- Broadcast the event


The broadcast has the effect of notifying _other_ connected clients aside from the request originator of write confirmations. 
A failure to broadcast the event will result in a database ROLLBACK vs COMMIT.

