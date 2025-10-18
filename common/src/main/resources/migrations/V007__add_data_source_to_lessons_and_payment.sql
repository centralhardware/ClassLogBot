-- Add data_source column to lessons table to track where the lesson was created (WEB or BOT)
ALTER TABLE lessons ADD COLUMN IF NOT EXISTS data_source VARCHAR(10);

-- Add data_source column to payment table to track where the payment was created (WEB or BOT)
ALTER TABLE payment ADD COLUMN IF NOT EXISTS data_source VARCHAR(10);
