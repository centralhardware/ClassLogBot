-- When the payment reached Firefly; NULL means the export is still pending and the retry job picks it up.
ALTER TABLE payment ADD COLUMN IF NOT EXISTS firefly_exported_at TIMESTAMP;

-- Payments made before this column existed were exported (or deliberately left out) back then.
UPDATE payment SET firefly_exported_at = now() WHERE firefly_exported_at IS NULL;
