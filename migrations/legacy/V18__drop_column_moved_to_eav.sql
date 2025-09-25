ALTER TABLE payment DROP COLUMN photo_id;

ALTER TABLE service DROP COLUMN photo_id;

ALTER TABLE client DROP COLUMN date_of_birth;
ALTER TABLE client DROP COLUMN date_of_record;
ALTER TABLE client DROP COLUMN class_number;
ALTER TABLE client DROP COLUMN how_to_know;
ALTER TABLE client DROP COLUMN telephone;
ALTER TABLE client DROP COLUMN telephone_responsible;
ALTER TABLE client DROP COLUMN mother_name;

CREATE SEQUENCE payment_id_sequence START 320;
ALTER TABLE payment ALTER COLUMN id SET DEFAULT nextval('payment_id_sequence');

CREATE SEQUENCE service_id_sequence START 712;
ALTER TABLE service ALTER COLUMN unique_id SET DEFAULT nextval('service_id_sequence')

CREATE SEQUENCE client_id_sequence START 269;
ALTER TABLE client ALTER COLUMN id SET DEFAULT nextval('client_id_sequence');

alter table payment alter column is_deleted set default false;
alter table service alter column is_deleted set default false;