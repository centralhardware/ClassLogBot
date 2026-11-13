-- Pending "sign in via the bot" tokens. The web app creates and polls them, the bot confirms them;
-- they live in the database since the two run as separate processes.
CREATE TABLE bot_login (
    token      TEXT PRIMARY KEY,
    created_at BIGINT NOT NULL,
    user_id    BIGINT
);
