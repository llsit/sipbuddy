-- Run after migration 003. Temporary test users/data are rolled back.
begin;
create temporary table settings_test_context on commit drop as
select gen_random_uuid() a,gen_random_uuid() b,gen_random_uuid() op;
insert into auth.users(id,aud,role) select a,'authenticated','authenticated' from settings_test_context;
insert into auth.users(id,aud,role) select b,'authenticated','authenticated' from settings_test_context;
grant select on settings_test_context to authenticated;
set local role authenticated;
do $$
declare t record; s jsonb := '{"goal_ml":2000,"reminder_enabled":true,"reminder_times":[600,840,1080],"quiet_enabled":true,"quiet_start":1320,"quiet_end":420,"stop_at_goal":true}'; r jsonb; n integer;
begin
 select * into t from settings_test_context;
 perform set_config('request.jwt.claim.sub',t.a::text,true);
 perform set_config('request.jwt.claims',jsonb_build_object('sub',t.a,'role','authenticated')::text,true);
 if public.valid_hydration_settings(null) or public.valid_hydration_settings('{}') or
    public.valid_hydration_settings(s || '{"reminder_times":[600,600]}') or
    public.valid_hydration_settings(s || '{"reminder_times":[60]}') or
    public.valid_hydration_settings(s || '{"goal_ml":2000.5}') then raise exception 'Invalid values accepted'; end if;
 r := public.apply_hydration_settings_change(t.a,s,0,t.op);
 if not (r->>'applied')::boolean or (r#>>'{record,version}')::integer <> 1 then raise exception 'Create failed'; end if;
 r := public.apply_hydration_settings_change(t.a,s,0,t.op);
 if (r#>>'{record,version}')::integer <> 1 then raise exception 'Retry duplicated update'; end if;
 r := public.apply_hydration_settings_change(t.a,s || '{"goal_ml":3000}',1,gen_random_uuid());
 if (r#>>'{record,version}')::integer <> 2 then raise exception 'Update failed'; end if;
 r := public.apply_hydration_settings_change(t.a,s,1,gen_random_uuid());
 if (r->>'applied')::boolean or (r#>>'{record,settings,goal_ml}')::integer <> 3000 then raise exception 'Stale update accepted'; end if;
 begin
  perform public.apply_hydration_settings_change(t.a,s,null,gen_random_uuid());
  raise exception 'Null version accepted';
 exception when invalid_parameter_value then null;
 end;
 begin
  perform public.apply_hydration_settings_change(t.a,'{}',2,gen_random_uuid());
  raise exception 'Invalid payload accepted';
 exception when invalid_parameter_value then null;
 end;
 begin
  update public.hydration_settings set settings=s where owner_id=t.a;
  raise exception 'Direct update allowed';
 exception when insufficient_privilege then null;
 end;
 perform set_config('request.jwt.claim.sub',t.b::text,true);
 perform set_config('request.jwt.claims',jsonb_build_object('sub',t.b,'role','authenticated')::text,true);
 select count(*) into n from public.hydration_settings where owner_id=t.a;
 if n <> 0 then raise exception 'RLS exposed A'; end if;
 begin
  perform public.apply_hydration_settings_change(t.a,s,2,gen_random_uuid());
  raise exception 'B wrote A';
 exception when insufficient_privilege then null;
 end;
 r := public.apply_hydration_settings_change(t.b,s,0,gen_random_uuid());
 if (r#>>'{record,owner_id}')::uuid <> t.b then raise exception 'B owner mismatch'; end if;
end;
$$;
reset role;
set local role anon;
do $$
begin
 begin
  perform * from public.hydration_settings;
  raise exception 'Anonymous read allowed';
 exception when insufficient_privilege then null;
 end;
 begin
  perform public.apply_hydration_settings_change(gen_random_uuid(),'{}',0,gen_random_uuid());
  raise exception 'Anonymous RPC allowed';
 exception when insufficient_privilege then null;
 end;
end;
$$;
reset role;
rollback;
