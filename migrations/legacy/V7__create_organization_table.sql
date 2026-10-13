CREATE TABLE organization (
    id UUID PRIMARY KEY,
    name TEXT,
    owner BIGINT
);
ALTER TABLE pupil ADD COLUMN organization_id uuid
CONSTRAINT organization_id_fk REFERENCES organization(id);