DROP TABLE config;

ALTER TABLE client RENAME TO students;
ALTER TABLE service RENAME TO lessons;
ALTER TABLE services RENAME TO subjects;
ALTER TABLE telegram_users RENAME TO tutors;

ALTER TABLE students RENAME COLUMN klass TO school_class;

ALTER TABLE payment RENAME COLUMN chat_id TO tutor_id;
ALTER TABLE payment RENAME COLUMN pupil_id TO student_id;
ALTER TABLE payment RENAME COLUMN services TO subject_id;

ALTER TABLE lessons RENAME COLUMN chat_id TO tutor_id;
ALTER TABLE lessons RENAME COLUMN pupil_id TO student_id;
ALTER TABLE lessons RENAME COLUMN service_id TO subject_id;

ALTER TABLE tutors RENAME COLUMN services TO subjects;