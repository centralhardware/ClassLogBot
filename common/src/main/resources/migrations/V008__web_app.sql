-- Tables and columns behind the Telegram Mini App (bot/src/main/kotlin/.../web).

-- Tutor's share of what they billed, in percent; used for the payroll in the monthly report.
ALTER TABLE tutors ADD COLUMN IF NOT EXISTS rate INTEGER;

-- Whether a student still attends: active, paused or left, with the reason and since when.
ALTER TABLE students ADD COLUMN IF NOT EXISTS status VARCHAR(10) NOT NULL DEFAULT 'active';
ALTER TABLE students ADD COLUMN IF NOT EXISTS status_reason TEXT;
ALTER TABLE students ADD COLUMN IF NOT EXISTS status_since DATE;

CREATE TABLE IF NOT EXISTS student_notes (
    id         SERIAL PRIMARY KEY,
    student_id INTEGER   NOT NULL,
    tutor_id   BIGINT    NOT NULL,
    text       TEXT      NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_student_notes_student ON student_notes(student_id);

-- Weekly schedule: a slot repeats every week on `weekday` (0 = Monday), or happens once on `once_date`.
CREATE TABLE IF NOT EXISTS schedule_slots (
    id          SERIAL PRIMARY KEY,
    tutor_id    BIGINT    NOT NULL,
    subject_id  BIGINT    NOT NULL,
    student_ids INTEGER[] NOT NULL,
    weekday     SMALLINT,
    once_date   DATE,
    time        TIME      NOT NULL,
    amount      INTEGER   NOT NULL,
    is_deleted  BOOLEAN   NOT NULL DEFAULT false,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK ((weekday IS NULL) <> (once_date IS NULL))
);
CREATE INDEX IF NOT EXISTS idx_schedule_slots_tutor ON schedule_slots(tutor_id) WHERE NOT is_deleted;

-- What happened to a slot on a given day: 'done' (a lesson was entered from it) or 'skip'.
CREATE TABLE IF NOT EXISTS schedule_marks (
    slot_id   INTEGER     NOT NULL,
    date      DATE        NOT NULL,
    status    VARCHAR(10) NOT NULL,
    lesson_id UUID,
    PRIMARY KEY (slot_id, date)
);

CREATE TABLE IF NOT EXISTS reminder_settings (
    tutor_id  BIGINT PRIMARY KEY,
    evening   BOOLEAN NOT NULL DEFAULT true,
    at        VARCHAR(5) NOT NULL DEFAULT '21:00',
    month_end BOOLEAN NOT NULL DEFAULT true
);

-- A tutor's monthly report as handed to the director: submitted, accepted or returned.
CREATE TABLE IF NOT EXISTS report_submissions (
    tutor_id      BIGINT      NOT NULL,
    month         VARCHAR(7)  NOT NULL,
    status        VARCHAR(10) NOT NULL,
    note          TEXT,
    comment       TEXT,
    submitted_at  TIMESTAMP,
    decided_at    TIMESTAMP,
    decided_by    BIGINT,
    self_accepted BOOLEAN     NOT NULL DEFAULT false,
    PRIMARY KEY (tutor_id, month)
);

-- Small key/value settings, e.g. which audit alerts admins get.
CREATE TABLE IF NOT EXISTS app_settings (
    key   TEXT PRIMARY KEY,
    value TEXT NOT NULL
);

-- Audit entries now carry where they came from, a readable title and a field-by-field diff.
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS source VARCHAR(10);
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS title TEXT;
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS changes JSONB;
