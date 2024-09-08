-- Migration V1 (Generated on 2024-09-06 15:42:54)

CREATE SCHEMA IF NOT EXISTS stable;

/* Default stores */
CREATE TABLE stable.logs (
    id UUID PRIMARY KEY,
    collection_slug text,
    field_id UUID,
    document_id UUID,
    event_type TEXT NOT NULL,
    actor_id TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    confirmed_at BIGINT,
    -- todo: more thoughts on log storage, diffs undo etc.
    content JSON
);

CREATE INDEX index_logs_collection_slug ON stable.logs (collection_slug);
CREATE INDEX index_logs_document_id ON stable.logs (document_id);

CREATE TABLE stable.collections (
    id uuid PRIMARY KEY,
    name text,
    slug text NOT NULL,
    icon text,
    type text,
    description text
);

CREATE TABLE stable.fields (
   id uuid PRIMARY KEY,
   collection_slug text,
   kind text NOT NULL,
   name text NOT NULL,
   default_value text,
   label text,
   realtime boolean,
   placeholder text,
   help json,
   disabled boolean,
   hidden boolean,
   width text,
   preview_type text,
   choices json
);

CREATE INDEX index_fields_collection_slug ON stable.fields(collection_slug text_ops);