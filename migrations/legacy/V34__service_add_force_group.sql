alter table service add column force_group boolean default false;
alter table service alter column force_group set not null ;