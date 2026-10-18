-- Account-scoped goal/reminders. No existing history or preferences are deleted.
begin;
create function public.valid_hydration_settings(s jsonb)
returns boolean language plpgsql immutable set search_path = '' as $$
declare t jsonb; m integer; seen integer[] := '{}'; start_min integer; end_min integer; outside_quiet boolean := false;
begin
  if s is null or jsonb_typeof(s) <> 'object' then return false; end if;
  if jsonb_typeof(s->'goal_ml') is distinct from 'number' or
     jsonb_typeof(s->'reminder_enabled') is distinct from 'boolean' or
     jsonb_typeof(s->'quiet_enabled') is distinct from 'boolean' or
     jsonb_typeof(s->'stop_at_goal') is distinct from 'boolean' or
     jsonb_typeof(s->'quiet_start') is distinct from 'number' or
     jsonb_typeof(s->'quiet_end') is distinct from 'number' or
     jsonb_typeof(s->'reminder_times') is distinct from 'array' then return false; end if;
  if (s->>'goal_ml') !~ '^[0-9]+$' or (s->>'goal_ml')::integer not between 250 and 10000 then return false; end if;
  if (s->>'quiet_start') !~ '^[0-9]+$' or (s->>'quiet_end') !~ '^[0-9]+$' then return false; end if;
  start_min := (s->>'quiet_start')::integer; end_min := (s->>'quiet_end')::integer;
  if start_min not between 0 and 1439 or end_min not between 0 and 1439 then return false; end if;
  if jsonb_array_length(s->'reminder_times') not between 1 and 12 then return false; end if;
  for t in select value from jsonb_array_elements(s->'reminder_times') loop
    if jsonb_typeof(t) <> 'number' or t::text !~ '^[0-9]+$' then return false; end if;
    m := t::text::integer;
    if m not between 0 and 1439 or m = any(seen) then return false; end if;
    seen := array_append(seen,m);
    if not (s->>'quiet_enabled')::boolean or start_min = end_min or
       (start_min < end_min and (m < start_min or m >= end_min)) or
       (start_min > end_min and m >= end_min and m < start_min) then outside_quiet := true; end if;
  end loop;
  return not (s->>'reminder_enabled')::boolean or outside_quiet;
exception when others then return false;
end;
$$;
create table public.hydration_settings (
  owner_id uuid primary key references auth.users(id) on delete cascade,
  settings jsonb not null check (public.valid_hydration_settings(settings)),
  version bigint not null check (version > 0),
  last_operation_id uuid not null,
  updated_at timestamptz not null default now()
);
alter table public.hydration_settings enable row level security;
alter table public.hydration_settings force row level security;
revoke all on public.hydration_settings from public, anon, authenticated;
grant select on public.hydration_settings to authenticated;
create policy own_settings_read on public.hydration_settings for select to authenticated
using (owner_id = (select auth.uid()));
create function public.apply_hydration_settings_change(p_owner uuid, p_settings jsonb, p_base_version bigint, p_operation uuid)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare r public.hydration_settings;
begin
  if auth.uid() is null or p_owner is null or auth.uid() <> p_owner then
    raise exception 'Account mismatch' using errcode = '42501';
  end if;
  if p_base_version is null or p_base_version < 0 or p_operation is null or not public.valid_hydration_settings(p_settings) then
    raise exception 'Invalid settings operation' using errcode = '22023';
  end if;
  if p_base_version = 0 then
    insert into public.hydration_settings(owner_id,settings,version,last_operation_id)
      values(p_owner,p_settings,1,p_operation) on conflict(owner_id) do nothing returning * into r;
    if found then return jsonb_build_object('applied',true,'record',to_jsonb(r)); end if;
  end if;
  select * into r from public.hydration_settings where owner_id = p_owner for update;
  if not found then raise exception 'Missing settings record' using errcode = 'P0002'; end if;
  if r.last_operation_id = p_operation then return jsonb_build_object('applied',true,'record',to_jsonb(r)); end if;
  if r.version <> p_base_version then return jsonb_build_object('applied',false,'record',to_jsonb(r)); end if;
  update public.hydration_settings set settings=p_settings,version=r.version+1,
    last_operation_id=p_operation,updated_at=now() where owner_id=p_owner returning * into r;
  return jsonb_build_object('applied',true,'record',to_jsonb(r));
end;
$$;
revoke all on function public.apply_hydration_settings_change(uuid,jsonb,bigint,uuid) from public,anon;
grant execute on function public.apply_hydration_settings_change(uuid,jsonb,bigint,uuid) to authenticated;
commit;
