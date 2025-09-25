CREATE TABLE services (
    id SERIAL PRIMARY KEY ,
    key TEXT,
    name TEXT,
    organization_id UUID REFERENCES organization(id)
)