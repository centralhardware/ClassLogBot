update payment_backup
set properties = jsonb_build_array(
        jsonb_build_object(
                'name', 'фото отчетности',
                'type', 'Photo',
                'value', payment_backup.photo_id
            )
    )

update service_backup
set properties = jsonb_build_array(
        jsonb_build_object(
                'name', 'фото отчетности',
                'type', 'Photo',
                'value', service_backup.photo_id
            )
    )

update client
set properties = jsonb_build_array(
    jsonb_build_object(
        'name', 'класс',
        'type', 'Integer',
        'value', client.class_number
        ),
    jsonb_build_object(
            'name', 'дата записи',
            'type', 'Date',
            'value', to_char(client.date_of_record, 'dd MM yyyy')
        ),
    jsonb_build_object(
            'name', 'дата рождения',
            'type', 'Date',
            'value', to_char(client.date_of_birth, 'dd MM yyyy')
        ),
    jsonb_build_object(
            'name', 'как узнал',
            'type', 'Enumeration',
            'value', (case when how_to_know = 'SIGNBOARD' then 'Вывеска'
                           when how_to_know = 'FROM_PASTE_YEAR' then 'С прошлых лет'
                           when how_to_know = 'FROM_FIENDS' then 'От знакомых'
                           when how_to_know = 'FROM_2GIS' then '2gis'
                           when how_to_know = 'ENTRANCE_ADVERTISE' then 'Реклама на подъезд'
                           when how_to_know = 'THE_ELDERS_WENT' then 'Ходили старшие'
                           when how_to_know = 'INTERNET' then 'Интернет'
                           when how_to_know = 'LEAFLET' then 'Листовка'
                           when how_to_know = 'AUDIO_ADVERTISE_IN_STORE' then 'Аудио Реклама в магазине'
                           when how_to_know = 'ADVERTISING_ON_TV' then 'Реклама на ТВ'
                           when how_to_know = 'INSTAGRAM' then 'инстаграм'
        end)
        ),
    jsonb_build_object(
            'name', 'телефон',
            'type', 'Telephone',
            'value', client.telephone
        ),
    jsonb_build_object(
            'name', 'телефон ответственного',
            'type', 'Telephone',
            'value', client.telephone
        )
    )