CREATE TABLE invitation (
    id SERIAL PRIMARY KEY,
    org_id UUID NOT NULL,
    services INT[],
    confirm_code TEXT
)