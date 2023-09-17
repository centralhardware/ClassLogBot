ALTER TABLE znatoki_statistic_time ADD COLUMN pupil_id text default 0;
ALTER TABLE znatoki_statistic_time DROP COLUMN pupil_id;
ALTER TABLE znatoki_statistic_time ADD COLUMN pupil_id int default 0;