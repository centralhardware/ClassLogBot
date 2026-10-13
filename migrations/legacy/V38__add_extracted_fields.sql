ALTER TABLE client
    ADD COLUMN klass INTEGER NULL,
    ADD COLUMN record_date DATE NULL,
    ADD COLUMN birth_date DATE NULL,
    ADD COLUMN source TEXT NULL,
    ADD COLUMN phone TEXT NULL,
    ADD COLUMN responsible_phone TEXT NULL,
    ADD COLUMN mother_fio TEXT NULL;

WITH v AS (
    SELECT
        c.id,
        max(j.value) FILTER (WHERE j.name = 'класс')                   AS "класс",
        max(j.value) FILTER (WHERE j.name = 'дата записи')             AS "дата записи",
        max(j.value) FILTER (WHERE j.name = 'дата рождения')           AS "дата рождения",
        max(j.value) FILTER (WHERE j.name = 'как узнал')               AS "как узнал",
        max(j.value) FILTER (WHERE j.name = 'телефон')                 AS "телефон",
        max(j.value) FILTER (WHERE j.name = 'телефон ответственного')  AS "телефон ответственного",
        max(j.value) FILTER (WHERE j.name = 'ФИО матери')              AS "ФИО матери"
    FROM client c
             LEFT JOIN LATERAL jsonb_to_recordset(c.properties::jsonb)
        AS j(name text, type text, value text)
                       ON TRUE
    GROUP BY c.id
)
UPDATE client c
SET
    klass = NULLIF(v."класс", '')::int,

    record_date = CASE
                      WHEN v."дата записи" IS NULL OR btrim(v."дата записи") = '' THEN NULL
                      WHEN btrim(v."дата записи") ~ '^\d{1,2}\s+\d{1,2}\s+\d{4}$'
                          THEN to_date(regexp_replace(btrim(v."дата записи"), '\s+', ' ', 'g'), 'FMDD FMMM YYYY')
                      WHEN btrim(v."дата записи") ~ '^\d{1,2}\.\d{1,2}\.\d{4}$'
                          THEN to_date(btrim(v."дата записи"), 'FMDD.FMMM.YYYY')
                      ELSE NULL
        END,

    birth_date = CASE
                     WHEN v."дата рождения" IS NULL OR btrim(v."дата рождения") = '' THEN NULL
                     WHEN btrim(v."дата рождения") ~ '^\d{1,2}\s+\d{1,2}\s+\d{4}$'
                         THEN to_date(regexp_replace(btrim(v."дата рождения"), '\s+', ' ', 'g'), 'FMDD FMMM YYYY')
                     WHEN btrim(v."дата рождения") ~ '^\d{1,2}\.\d{1,2}\.\d{4}$'
                         THEN to_date(btrim(v."дата рождения"), 'FMDD.FMMM.YYYY')
                     ELSE NULL
        END,

    source            = NULLIF(v."как узнал", ''),
    phone             = NULLIF(v."телефон", ''),
    responsible_phone = NULLIF(v."телефон ответственного", ''),
    mother_fio        = NULLIF(v."ФИО матери", '')
FROM v
WHERE v.id = c.id;



ALTER TABLE payment
    ADD COLUMN IF NOT EXISTS photo_report TEXT NULL;

UPDATE payment p
SET photo_report = sub.value
FROM (
    SELECT id,
           (
               SELECT elem->>'value'
               FROM jsonb_array_elements(p.properties) elem
               WHERE elem->>'name' = 'фото отчетности'
               LIMIT 1
           ) AS value
    FROM payment p
) AS sub
WHERE p.id = sub.id
  AND sub.value IS NOT NULL
  AND (p.photo_report IS NULL OR p.photo_report = '');


ALTER TABLE service
    ADD COLUMN IF NOT EXISTS photo_report TEXT NULL;

UPDATE service s
SET photo_report = sub.value
FROM (
    SELECT id,
           (
               SELECT elem->>'value'
               FROM jsonb_array_elements(s.properties) elem
               WHERE elem->>'name' = 'фото отчетности'
               LIMIT 1
           ) AS value
    FROM service s
) AS sub
WHERE s.id = sub.id
  AND sub.value IS NOT NULL
  AND (s.photo_report IS NULL OR s.photo_report = '');
