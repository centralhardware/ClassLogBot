CREATE TABLE IF NOT EXISTS payment (
    id SERIAL PRIMARY KEY ,
    chat_id BIGINT,
    pupil_id INTEGER NOT NULL ,
    amount INTEGER NOT NULL ,
    photo_id TEXT,
    time_id UUID,
    org_id UUID NOT NULL ,
    date_time TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false
)