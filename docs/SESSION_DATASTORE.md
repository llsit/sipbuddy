# Encrypted session in Preferences DataStore

Application-owned storage now uses Room for hydration entries and DataStore for settings and Supabase sessions. No production path reads/writes SharedPreferences except the one-time legacy migration adapter. The Supabase SDK SessionManager interface remains unchanged.

## Storage and DI

`Context.authSessionDataStore` is the single process-wide Preferences DataStore delegate for `files/datastore/auth_session.preferences_pb`. Koin injects `EncryptedDataStoreSessionManager` as SessionManager and injects a separate SessionCipher implemented by AndroidKeystoreSessionCipher. The session manager handles serialization and atomic DataStore transactions; the cipher handles encryption and key management. Both execute on the manager's IO dispatcher.

Only Base64 IV and AES-GCM ciphertext are stored, together with a migration-completed marker. Refresh/access tokens never become plaintext preferences. Every encryption uses a fresh IV. JSON default values are explicitly serialized so the SDK does not recompute a time-dependent token expiry when restoring a session. The Android Keystore alias remains exactly `<application package>.auth_session`, matching the previous implementation. Keystore keys remain non-exportable; logout removes the encrypted session fields but does not rotate/delete the legacy key. Android backup remains disabled as before.

## Migration and logout safety

AndroidX SharedPreferencesMigration imports the complete `iv`/`session` pair from the old `auth_session` XML. A pure AuthSessionMigrationMapper never combines individual fields from different sources, never overwrites an existing DataStore payload, and never imports old tokens after a DataStore save/logout has set the completion marker. Unrelated old preferences are not migrated or removed.

The generic AndroidX migration is intentionally configured to run when old keys are present even if DataStore already has both fields. The convenience Preferences migration skips when all target keys exist, which could leave stale old tokens behind. Here, stale keys are cleaned only after the DataStore transaction succeeds. Migration/write failures leave the old source available for retry; a later cleanup retry sees the marker and preserves the committed DataStore payload.

Every DataStore save and delete sets the marker atomically. Deleting before the first read first initializes/migrates the store and then deletes the pair. Reopening after logout cannot resurrect an old identity even if stale XML reappears. Corrupt/missing payloads fail to load; no plaintext fallback or automatic destructive corruption handler is installed.

No Supabase schema changes, SQL steps, app-data clear or login reset are required for a valid legacy session with its original Keystore key still present.

## Verification

Unit tests cover encrypted round-trip, fresh IV, atomic failures/deletion, cancellation, tampering, concurrent saves and pure migration precedence/idempotency/logout rules. QA instrumentation covers real Android Keystore, disk reopen, independent legacy-format fixtures, migration cleanup, pre-read logout/save, migration failure/retry and stale XML after logout.

Run `formatKotlin checkKotlinFormat assembleDebug testQaUnitTest lintDebug compileQaAndroidTestKotlin`. Execute connected tests only with `connectedQaAndroidTest`, never the personal main package. Real Keystore/migration assertions require an Android device/emulator; JVM cipher tests alone do not verify Android Keystore or the installed user's session.

AndroidX migration API: https://developer.android.com/reference/kotlin/androidx/datastore/migrations/SharedPreferencesMigration
