-- Add added_by_tutor_id column to lessons table to track who added the lesson
ALTER TABLE lessons ADD COLUMN IF NOT EXISTS added_by_tutor_id BIGINT;

-- Add foreign key constraint
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'lessons_added_by_tutor_fk') THEN
        ALTER TABLE lessons ADD CONSTRAINT lessons_added_by_tutor_fk FOREIGN KEY (added_by_tutor_id) REFERENCES tutors(id);
    END IF;
END $$;
