-- Add added_by_tutor_id column to payment table to track who added the payment
ALTER TABLE payment ADD COLUMN IF NOT EXISTS added_by_tutor_id BIGINT;

-- Add foreign key constraint
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'payment_added_by_tutor_fk') THEN
        ALTER TABLE payment ADD CONSTRAINT payment_added_by_tutor_fk FOREIGN KEY (added_by_tutor_id) REFERENCES tutors(id);
    END IF;
END $$;
