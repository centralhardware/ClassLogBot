alter table service add column update_time timestamp default now();
update service
set update_time = date_time