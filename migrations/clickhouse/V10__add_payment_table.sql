CREATE TABLE IF NOT EXISTS znatoki_payment
(
    date_time DateTime,
    chat_id BIGINT,
    pupil_id INT,
    amount INT,
    photoId String,
    is_deleted BOOL DEFAULT false
)
ENGINE = MergeTree
ORDER BY date_time