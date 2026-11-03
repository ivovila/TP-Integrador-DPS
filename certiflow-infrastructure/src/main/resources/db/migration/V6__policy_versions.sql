alter table certificate add column global_policy_version integer;
update certificate set global_policy_version = 1 where global_policy_code is not null;
update certificate set decision = replace(decision, '"policyCode":', '"policyVersion":1,"policyCode":')
    where decision not like '%"policyVersion":%';
