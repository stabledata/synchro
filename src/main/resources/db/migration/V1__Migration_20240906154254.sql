-- Migration V1 (Generated on 2024-09-06 15:42:54)

CREATE SCHEMA IF NOT EXISTS stable;

/* Default stores */
CREATE TABLE stable.logs (
    id uuid PRIMARY KEY,
    event_type text NOT NULL,
    actor_id text NOT NULL,
    path text,
    collection_id uuid,
    document_id uuid,
    created_at bigint NOT NULL,
    confirmed_at bigint
);

CREATE INDEX index_logs_collection_id ON stable.logs (collection_id);
CREATE INDEX index_logs_document_id ON stable.logs (document_id);

CREATE TABLE stable.collections (
    id uuid PRIMARY KEY,
    path text NOT NULL,
    type text,
    label text,
    icon text,
    description text
);

CREATE TABLE stable.fields (
   id uuid PRIMARY KEY,
   collection_id uuid NOT NULL,
   path text NOT NULL,
   kind text NOT NULL,

   label text,
   placeholder text,
   default_value text,

   realtime boolean,
   help json,

   disabled boolean,
   hidden boolean
);

CREATE INDEX index_fields_collection_slug ON stable.fields(collection_id);