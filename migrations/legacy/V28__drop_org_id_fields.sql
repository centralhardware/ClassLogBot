alter table payment drop column org_id;
alter table client drop column organization_id;
alter table telegram_users drop column org_id;
alter table services drop column organization_id;
alter table service drop column org_id;