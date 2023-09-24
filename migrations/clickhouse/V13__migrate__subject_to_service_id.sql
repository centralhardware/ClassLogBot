ALTER TABLE znatoki_statistic_time ADD COLUMN service_id INT

ALTER TABLE znatoki_statistic_time
UPDATE service_id = CASE subject
                        WHEN 'MATHEMATICS'THEN 1
                        WHEN 'RUSSIAN' THEN 2
                        WHEN 'LITERATURE' THEN 3
                        WHEN 'PHYSICS' THEN 4
                        WHEN 'ENGLISH' THEN 5
                        WHEN 'GERMAN' THEN 6
                        WHEN 'SOCIAL_SCIENCE' THEN 7
                        WHEN 'BIOLOGY' THEN 8
                        WHEN 'CHEMISTRY' THEN 9
                        WHEN 'PRIMARY_SCHOOL' THEN 10
                        WHEN 'PSYCHOLOGY' THEN 11
                        WHEN 'READING' THEN 12
                        WHEN 'ART' THEN 13
                        WHEN 'CAREER_GUIDANCE_TESTING' THEN 14
    END
WHERE true;

ALTER TABLE znatoki_statistic_time DROP COLUMN subject;
