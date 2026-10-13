alter table asset add column subsystems text not null;
alter table schema_version add column required_subsystems text not null;
alter table finding add column subsystem_code varchar(100);
alter table certificate alter column inspection_id drop not null;
alter table certificate alter column inspection_closed_at drop not null;
alter table certificate add column scope_type varchar(16) not null;
alter table certificate add column subsystem_code varchar(100);
alter table certificate add column basis_type varchar(16) not null;
alter table certificate add column schema_asset_type varchar(255) not null;
alter table certificate add column schema_number integer not null;
alter table certificate add column required_subsystems text not null;
alter table certificate add column global_policy_code varchar(100);
alter table certificate add column latest_inspection_closed_at timestamp(6) with time zone;
create index certificate_scope on certificate(asset_code, scope_type, subsystem_code);
create table certificate_backing (
    global_number varchar(64) not null references certificate(number),
    position integer not null,
    partial_number varchar(64) not null references certificate(number),
    primary key(global_number, position),
    unique(global_number, partial_number)
);
