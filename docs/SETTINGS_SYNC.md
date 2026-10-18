# Offline account goals and reminders

## Setup

Run `supabase/migrations/202610180003_account_settings_sync.sql` after migrations 001/002.
Run `supabase/tests/account_settings_sync_security.sql` in SQL Editor. The test transaction rolls back its synthetic users and data.
Only the public URL/key belongs in the Android configuration. Notification permission is never uploaded or granted by cloud settings.

## Local persistence and isolation

DataStore is the settings source of truth. Each owner has one JSON preference containing settings, acknowledged server version, local generation, pending flag and operation UUID; a save atomically writes the whole record before networking. Goals, enabled reminders, up to 12 reminder times, quiet hours and stop-at-goal sync together as one settings document. Reminder times represent local wall-clock time on each device, not a fixed timezone.

Existing unscoped preferences remain in the guest workspace. Signed-in accounts start with defaults unless they have cached or remote settings. Untouched defaults never overwrite remote settings. Guest settings never upload, and importing guest history only imports history. Account-specific caches survive sign-out for subsequent offline use with the restored account identity.

ViewModels bind the account at creation. Settings emissions carry their owner; queued saves pass the expected owner and DataStore checks it inside the atomic update. The overview rejects settings for another account even when that account has no drink records. Remote requests require the matching SDK user and the server validates `p_owner = auth.uid()` independently.

## Sync and conflict policy

The existing connected WorkManager jobs sync history and settings, with retry, periodic/foreground/manual triggers and a combined pending count. Local generation changes enqueue sync; acknowledgements and downloads do not increment generation, avoiding rescheduling loops. Settings remain pending across process death and failed requests. A run only reports success after both history and settings operations succeed.

`hydration_settings` has own-account SELECT RLS and denies direct writes. A security-definer RPC validates JSON fields and uses a locked server version plus operation UUID. Retries of the same latest operation are idempotent. An offline save based on a stale server version receives the canonical cloud document and increments the visible conflict notice. The document is atomic: concurrent edits to goal and reminder fields are not merged field by field.

If a new local save happens during upload, the acknowledgement advances only the base version and preserves the newer pending settings. A download never replaces pending changes and only replaces a clean record with a newer version. Account changes during HTTP requests reject responses before merging.

## Alarms and permissions

The coordinator observes the active account's persisted reminder settings and reschedules/cancels the Android alarm after an account change or cloud update. Reminder policy reads the current goal and quiet hours, including stop-at-goal. Saving checks the expected account again before scheduling. Notification permission remains an OS/device decision; receiving enabled reminders from another device does not bypass it. OEM battery restrictions and WorkManager scheduling may delay background delivery/sync.

## Verification

Run assembleDebug, testQaUnitTest, lintDebug and connectedQaAndroidTest. Instrumentation must stay on the isolated `.qa` package, never the personal debug app.

Unit cases cover guest legacy settings, account isolation, stale saves, persisted outbox/restart, offline retry, canonical conflicts, wrong-owner responses, untouched-default download, mutation during upload, and merge/ack generation behavior. SQL tests cover two accounts, anonymous denial, direct-write denial, duplicate retry, stale versions, malformed values and NULL versions.

Real acceptance still requires two signed-in devices: save goal/quiet hours/reminder times on A, sync B, check alarms with local permission; modify offline and reconnect; switch accounts/sign out; cold-start offline. Local tests and SQL simulation do not prove authenticated end-to-end transport or OEM background behavior.
