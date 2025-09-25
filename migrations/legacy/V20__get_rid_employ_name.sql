ALTER TABLE telegram_users ADD COLUMN name TEXT;
update telegram_users
set name = (select fio from employ_name where  id = chat_id limit 1);
drop table employ_name;