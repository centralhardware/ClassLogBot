CREATE TABLE IF NOT EXISTS znatoki_statistic_time
(
    date_time DateTime,
    chat_id   BIGINT,
    subject   String,
    fio       String,
    amount    INT,
    photoId   String
)
    engine = MergeTree
        ORDER BY date_time