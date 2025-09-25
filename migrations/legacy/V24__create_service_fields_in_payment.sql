ALTER TABLE payment ADD COLUMN services INT
    CONSTRAINT services_fk REFERENCES services(id)