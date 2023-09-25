CREATE TABLE IF NOT EXISTS service (
    unique_id SERIAL PRIMARY KEY ,
    id UUID,
    chat_id BIGINT NOT NULL ,
    amount INTEGER NOT NULL ,
    photo_id TEXT,
    pupil_id INTEGER NOT NULL ,
    org_id UUID NOT NULL ,
    service_id INTEGER NOT NULL ,
    date_time TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false
);