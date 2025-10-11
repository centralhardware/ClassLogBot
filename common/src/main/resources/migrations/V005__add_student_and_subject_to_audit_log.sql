ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS student_id INTEGER;
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS subject_id INTEGER;

CREATE INDEX IF NOT EXISTS idx_audit_log_student_id ON audit_log(student_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_subject_id ON audit_log(subject_id);
