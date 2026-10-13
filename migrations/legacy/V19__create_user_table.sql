CREATE TABLE IF NOT EXISTS telegram_users (
    id BIGINT PRIMARY KEY ,
    role TEXT,
    org_id UUID,
    services TEXT
)