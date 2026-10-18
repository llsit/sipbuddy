begin;

create table if not exists public.hydration_entries (
  owner_id uuid not null references auth.users(id) on delete cascade,
  entry_id uuid not null,
  drink text not null check (drink in ('WATER','TEA','COFFEE','MILK','JUICE','OTHER')),
  amount_ml integer not null check (amount_ml between 1 and 5000),
  consumed_at bigint not null,
  deleted boolean not null default false,
  version bigint not null check (version > 0),
  last_operation_id uuid not null,
  updated_at timestamptz not null default now(),
  primary key (owner_id, entry_id)
);

alter table public.hydration_entries enable row level security;
alter table public.hydration_entries force row level security;
revoke all on public.hydration_entries from public, anon, authenticated;
grant select on public.hydration_entries to authenticated;
drop policy if exists hydration_entries_read_own on public.hydration_entries;
create policy hydration_entries_read_own on public.hydration_entries
  for select to authenticated using ((select auth.uid()) = owner_id);

-- Only this guarded RPC can write: direct INSERT/UPDATE/DELETE are not granted.
-- Versions are server-controlled. Stale writes return the canonical row instead of overwriting it.
create or replace function public.apply_hydration_change(
  p_owner uuid, p_entry uuid, p_drink text, p_amount integer, p_consumed bigint,
  p_deleted boolean, p_base_version bigint, p_operation uuid, p_restore boolean
) returns jsonb language plpgsql security definer set search_path = '' as $$
declare r public.hydration_entries;
begin
  if auth.uid() is null or p_owner is null or auth.uid() <> p_owner then
    raise exception 'Account mismatch' using errcode = '42501';
  end if;
  if p_base_version is null or p_base_version < 0 or p_operation is null or p_entry is null or p_deleted is null or p_restore is null then
    raise exception 'Invalid operation' using errcode = '22023';
  end if;
  if p_base_version = 0 then
    insert into public.hydration_entries
      (owner_id,entry_id,drink,amount_ml,consumed_at,deleted,version,last_operation_id)
      values (p_owner,p_entry,p_drink,p_amount,p_consumed,p_deleted,1,p_operation)
      on conflict (owner_id,entry_id) do nothing returning * into r;
    if found then return jsonb_build_object('applied',true,'record',to_jsonb(r)); end if;
  end if;
  select * into r from public.hydration_entries
    where owner_id=p_owner and entry_id=p_entry for update;
  if not found then raise exception 'Missing remote record' using errcode='P0002'; end if;
  if r.last_operation_id = p_operation then
    return jsonb_build_object('applied',true,'record',to_jsonb(r));
  end if;
  if r.version <> p_base_version or (r.deleted and not p_deleted and not p_restore) then
    return jsonb_build_object('applied',false,'record',to_jsonb(r));
  end if;
  update public.hydration_entries set drink=p_drink,amount_ml=p_amount,consumed_at=p_consumed,
    deleted=p_deleted,version=r.version+1,last_operation_id=p_operation,updated_at=now()
    where owner_id=p_owner and entry_id=p_entry returning * into r;
  return jsonb_build_object('applied',true,'record',to_jsonb(r));
end;
$$;
revoke all on function public.apply_hydration_change(uuid,uuid,text,integer,bigint,boolean,bigint,uuid,boolean) from public,anon;
grant execute on function public.apply_hydration_change(uuid,uuid,text,integer,bigint,boolean,bigint,uuid,boolean) to authenticated;

commit;
