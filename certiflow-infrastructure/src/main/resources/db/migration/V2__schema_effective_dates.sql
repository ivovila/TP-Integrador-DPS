alter table schema_version add column effective_from timestamp(6) with time zone not null;
create unique index schema_effective_date_unique on schema_version(asset_type, effective_from);
