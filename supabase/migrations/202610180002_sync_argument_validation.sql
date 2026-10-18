-- Tighten RPC argument validation. No history is deleted or changed.
begin;
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

commit;
