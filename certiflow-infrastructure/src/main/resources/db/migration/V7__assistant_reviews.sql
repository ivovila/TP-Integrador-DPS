create table assistant_review (
    id varchar(64) primary key,
    inspection_id varchar(64) not null references inspection (id),
    prompt_version varchar(64) not null,
    model_id varchar(255) not null,
    requested_by_id varchar(64) not null,
    requested_by_name varchar(255) not null,
    requested_at timestamp(6) with time zone not null,
    suggestions text not null,
    rejected_proposals text not null
);
create index assistant_review_inspection on assistant_review (inspection_id, requested_at);

create table suggestion_decision (
    review_id varchar(64) not null references assistant_review (id),
    position integer not null,
    kind varchar(16) not null,
    decided_by_id varchar(64) not null,
    decided_by_name varchar(255) not null,
    decided_at timestamp(6) with time zone not null,
    reason text,
    primary key (review_id, position)
);
