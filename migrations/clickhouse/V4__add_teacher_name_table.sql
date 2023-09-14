CREATE TABLE IF NOT EXISTS znatoki_statistic_teacher_name
(
    chat_id   BIGINT,
    fio       String,
)
    engine = MergeTree
        ORDER BY chat_id