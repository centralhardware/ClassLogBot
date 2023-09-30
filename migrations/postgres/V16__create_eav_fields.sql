ALTER TABLE organization ADD COLUMN payment_custom_properties JSONB;
ALTER TABLE organization ADD COLUMN client_custom_properties JSONB;
ALTER TABLE organization ADD COLUMN service_custom_properties JSONB;

ALTER TABLE payment ADD COLUMN properties JSONB;
ALTER TABLE client ADD COLUMN properties JSONB;
ALTER TABLE services ADD COLUMN properties JSONB;

ALTER TABLE client ALTER COLUMN date_of_birth DROP NOT NULL;
ALTER TABLE client ALTER COLUMN date_of_record DROP NOT NULL;
ALTER TABLE client ALTER COLUMN telephone DROP NOT NULL;