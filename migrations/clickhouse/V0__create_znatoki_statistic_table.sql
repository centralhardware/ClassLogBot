CREATE TABLE IF NOT EXISTS znatoki_statistic
(
    date_time  DateTime,
    chat_id    BIGINT,
    username   Nullable(String),
    first_name Nullable(String),
    last_name  Nullable(String),
    lang       String,
    is_premium bool,
    action     String,
    text       VARCHAR(256)
)
    engine = MergeTree
        ORDER BY date_time