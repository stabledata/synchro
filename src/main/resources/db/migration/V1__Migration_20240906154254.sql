-- Migration V1 (Generated on 2024-09-06 15:42:54)

CREATE SCHEMA IF NOT EXISTS stable;

/* Default stores */
CREATE TABLE stable.logs (
    id uuid PRIMARY KEY,
    team_id text NOT NULL,
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
    team_id text NOT NULL,
    path text NOT NULL,
    type text,
    label text,
    icon text,
    description text
);

CREATE TABLE stable.fields (
   id uuid PRIMARY KEY,
   team_id text NOT NULL,
   collection_id uuid NOT NULL,
   path text NOT NULL,
   type text NOT NULL,

   label text,
   placeholder text,
   default_value text,

   realtime boolean,
   help json,
   disabled boolean,
   hidden boolean
);

CREATE TABLE stable.access (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    team_id VARCHAR(255) NOT NULL,
    type VARCHAR(5) CHECK (type IN ('grant', 'deny')) NOT NULL,
    role VARCHAR(255) NOT NULL,
    operation VARCHAR(255),
    path VARCHAR(255),
    CONSTRAINT either_operation_or_path CHECK (
        (operation IS NOT NULL AND path IS NULL) OR
        (path IS NOT NULL AND operation IS NULL)
    )
);

