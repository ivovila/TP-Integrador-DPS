alter table schema_version add column cancelled_by_id varchar(64);
alter table schema_version add column cancelled_by_name varchar(255);
alter table schema_version add column cancelled_at timestamp(6) with time zone;
alter table schema_version add column cancellation_reason text;
drop index schema_effective_date_unique;
