-- auto-generated definition
create table session
(
    uuid        varchar(255) not null
        primary key,
    create_date timestamp,
    pupil       integer
        constraint fkh9xc6iar3tf960tuapkkntv991
        references pupil,
    update_by   bigint
);

alter table session
    owner to admin;

