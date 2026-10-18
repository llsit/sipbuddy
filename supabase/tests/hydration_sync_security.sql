-- Run in the Supabase SQL Editor as postgres. All test identities/data are rolled back.
-- No signup endpoint, passwords or emails are used.
begin;
create temporary table sipbuddy_sync_test_context on commit drop as
select gen_random_uuid() as a,gen_random_uuid() as b,gen_random_uuid() as entry,
       gen_random_uuid() as op1,gen_random_uuid() as op2;
insert into auth.users(id,aud,role) select a,'authenticated','authenticated' from sipbuddy_sync_test_context;
insert into auth.users(id,aud,role) select b,'authenticated','authenticated' from sipbuddy_sync_test_context;
grant select on sipbuddy_sync_test_context to authenticated;
set local role authenticated;
do $$
declare t record; result jsonb; n integer;
begin
  select * into t from sipbuddy_sync_test_context;
  perform set_config('request.jwt.claim.sub',t.a::text,true);
  perform set_config('request.jwt.claims',jsonb_build_object('sub',t.a,'role','authenticated')::text,true);
  result:=public.apply_hydration_change(t.a,t.entry,'WATER',250,1000,false,0,t.op1,false);
  if not (result->>'applied')::boolean or (result#>>'{record,version}')::bigint <> 1 then raise exception 'Initial insert failed'; end if;
  result:=public.apply_hydration_change(t.a,t.entry,'WATER',250,1000,false,0,t.op1,false);
  if (result#>>'{record,version}')::bigint <> 1 then raise exception 'Retry duplicated write'; end if;
  result:=public.apply_hydration_change(t.a,t.entry,'TEA',500,1000,false,1,t.op2,false);
  if (result#>>'{record,version}')::bigint <> 2 then raise exception 'Edit failed'; end if;
  result:=public.apply_hydration_change(t.a,t.entry,'WATER',250,1000,false,1,gen_random_uuid(),false);
  if (result->>'applied')::boolean or (result#>>'{record,amount_ml}')::integer <> 500 then raise exception 'Stale write overwrote data'; end if;
  result:=public.apply_hydration_change(t.a,t.entry,'TEA',500,1000,true,2,gen_random_uuid(),false);
  if not (result#>>'{record,deleted}')::boolean then raise exception 'Delete failed'; end if;
  result:=public.apply_hydration_change(t.a,t.entry,'WATER',250,1000,false,3,gen_random_uuid(),false);
  if (result->>'applied')::boolean then raise exception 'Edit resurrected tombstone'; end if;
  result:=public.apply_hydration_change(t.a,t.entry,'TEA',500,1000,false,3,gen_random_uuid(),true);
  if not (result->>'applied')::boolean or (result#>>'{record,version}')::bigint <> 4 then raise exception 'Explicit undo failed'; end if;
  begin
    update public.hydration_entries set amount_ml=999 where owner_id=t.a;
    raise exception 'Direct table update was allowed';
  exception when insufficient_privilege then null;
  end;
  perform set_config('request.jwt.claim.sub',t.b::text,true);
  perform set_config('request.jwt.claims',jsonb_build_object('sub',t.b,'role','authenticated')::text,true);
  select count(*) into n from public.hydration_entries where owner_id=t.a;
  if n <> 0 then raise exception 'RLS leaked A to B'; end if;
  begin
    perform public.apply_hydration_change(t.a,t.entry,'WATER',999,1000,false,4,gen_random_uuid(),false);
    raise exception 'B could write A';
  exception when insufficient_privilege then null;
  end;
  result:=public.apply_hydration_change(t.b,t.entry,'MILK',300,1000,false,0,gen_random_uuid(),false);
  select count(*) into n from public.hydration_entries;
  if n <> 1 or (result#>>'{record,owner_id}')::uuid <> t.b then raise exception 'Owner isolation failed'; end if;
  raise notice 'PASS: own writes, idempotency, conflicts, delete/Undo, direct-write denial and cross-account RLS';
end;
$$;
set local role anon;
do $$
begin
  begin
    perform 1 from public.hydration_entries;
    raise exception 'Anonymous table read was allowed';
  exception when insufficient_privilege then null;
  end;
  begin
    perform public.apply_hydration_change(gen_random_uuid(),gen_random_uuid(),'WATER',250,0,false,0,gen_random_uuid(),false);
    raise exception 'Anonymous RPC execution was allowed';
  exception when insufficient_privilege then null;
  end;
  raise notice 'PASS: anonymous access denied';
end;
$$;
rollback;
