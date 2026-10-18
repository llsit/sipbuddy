# Offline account history and Supabase sync

Room remains the source of UI data. Local add/edit/delete/Undo commits before networking, and every mutation stores a persistent pending flag, operation UUID and local generation in the same Room transaction. WorkManager runs connected work after local changes, sign-in, app resume and periodic work (minimum 15 minutes; Android may defer it). Requests retry with exponential backoff. Manual sync replaces delayed work; operation IDs make retries safe after a lost HTTP response.

## Setup

Run the files in `supabase/migrations/` in filename order in your project's SQL Editor. Keep the existing ignored `supabase.properties` with the public URL/key. The Android client installs Auth and Postgrest. No service_role key belongs in the app. Run `supabase/tests/hydration_sync_security.sql` in SQL Editor to verify RLS and RPC behavior in a transaction that rolls back all synthetic users and data.

## Account isolation and guest history

Local rows are scoped by owner ID; cloud identity is `(owner_id, entry_id UUID)`. Local auto-increment IDs are never used as cloud identity. Every query/total/mutation uses the current owner. Switching accounts disposes hydration/sync ViewModels, drafts, snackbar Undo and saved screen state. Stale entry snapshots and queued adds carry the expected owner and cannot mutate the next account. A cached encrypted session retains the account's local workspace while the network/refresh is unavailable; it never authorizes a remote operation. Explicit logout returns to a separate guest workspace and retains private caches for later sign-in.

Room migration 1 → 2 preserves old IDs, timestamps, amounts and drink names, adds UUIDs and leaves legacy history in the guest workspace. It never assigns old/shared history automatically to whoever logs in first. Settings offers an explicit confirmed **Move guest history into this account** action. It moves live guest rows atomically, retains their UUIDs and marks them pending. Guest deletions stay local. Goals and reminder settings remain device-local; this feature syncs hydration history only.

## Consistency

The server RPC checks `p_owner = auth.uid()` itself and validates mandatory parameters. The table permits authenticated SELECT through an own-owner RLS policy; anonymous access and direct INSERT/UPDATE/DELETE are revoked. Only the guarded RPC writes server-controlled versions. Owner checks are repeated around each HTTP request and response, so changing accounts mid-flight cannot upload A's data as B.

Updates use the last acknowledged server version. If another device has already changed the same row, the RPC returns the canonical server row rather than allowing a stale write to overwrite it, and Settings shows a conflict notice. An edit made locally during an earlier request stays pending and is rebased onto the acknowledged server version. Deletes remain tombstones, not physical deletes; ordinary edits cannot resurrect a server tombstone. Explicit Undo retains the same UUID and restore intention, including when edited before the Undo upload finishes. Tombstones are retained without automatic garbage collection.

Uploads are processed in bounded batches. Downloads scan all account history, including tombstones, in UUID keyset pages and continue until an empty page. They do not use a wall-clock/timestamp watermark, so out-of-order server commits are not permanently skipped. New concurrent inserts behind the cursor are picked up by the next scan. Clean rows accept only newer server versions; pending local edits are never overwritten by a pull. This full scan favors correctness for the MVP; a larger deployment may need a server change log/cursor before history becomes very large.

## Verification

`gradlew.bat :app:assembleDebug :app:testQaUnitTest :app:lintDebug :app:connectedQaAndroidTest --no-configuration-cache`

Connected tests target `com.llsit.sipduddy.qa` only. Never change testBuildType to debug on a personal device. Room tests cover migration preservation, scoped reads/totals/writes, pending mutations during in-flight acknowledgements, tombstones, restore/edit intent, pull idempotency and explicit guest import. Unit tests cover network failure/retry, foreign owner responses, account changes, pagination, local account isolation and offline owner retention.

Live acceptance still requires sign-in on two devices or two isolated installations: offline add/edit/delete/Undo, reconnect, compare history; switch between two real accounts; terminate/reopen offline; verify background/boot retry and authenticated transport. SQL simulation and local fakes alone do not prove that complete device/account flow. The regular app's Google callback remains `sipbuddy://auth-callback`; QA uses `sipbuddy-qa://auth-callback` if intentionally testing authentication there.

References: https://supabase.com/docs/guides/database/postgres/row-level-security and https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work
