-- Change entity_id from INTEGER to VARCHAR to support different ID types (UUID, Integer, etc.)
ALTER TABLE audit_log ALTER COLUMN entity_id TYPE VARCHAR(255) USING entity_id::VARCHAR;
