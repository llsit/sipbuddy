# Supabase authentication

Email/password sign-in, signup, password recovery and Google browser OAuth use the Auth module of supabase-kt 3.6.0 with Ktor OkHttp 3.4.3. Kotlin Compose compiler is aligned to 2.3.21, the SDK build version. Postgrest-backed history sync is documented in [HYDRATION_SYNC.md](HYDRATION_SYNC.md).

## Local setup

Copy `supabase.properties.example` to `supabase.properties` and enter your project URL and publishable key. This file is ignored by Git. Gradle properties or environment variables with the same names can supply CI values. Only public publishable/anon keys are accepted; never embed service_role or secret keys. The public key is necessarily present in the compiled Android client.

Without configuration the diary works offline and the account action is disabled. With configuration, open Settings > Account to sign in. Account state does not gate local Room access. Every account has a separate cached diary and syncs its own history. Guests have a separate local diary with an explicit import action after login. Signing out does not delete hydration data.

## Supabase Dashboard

1. Authentication > Providers: enable Email. Keep email confirmation enabled. Configure password policy and production SMTP as needed.
2. Authentication > URL Configuration > Redirect URLs: add `sipbuddy://auth-callback` and `sipbuddy://auth-callback?mode=recovery` exactly. For the isolated benchmark app, add the equivalent `sipbuddy-benchmark://auth-callback` URLs only if testing authentication there.
3. For Google, configure a Google OAuth **Web application** client in Google Cloud. Copy the Supabase callback URL shown in the Google provider settings into Google's Authorized redirect URIs (`https://YOUR_PROJECT.supabase.co/auth/v1/callback`). Add the Google client ID/secret to Supabase's Google provider, then enable it. The Google client secret belongs only in the Supabase Dashboard, never in Android or Git. Configure consent/test users while the Google application is in testing.
4. Keep email templates using Supabase's confirmation URL so PKCE codes return to the app. Verification and recovery links must be opened on the same device/app that requested them. New-password submission appears only after a successful recovery callback. Request a new link if expired or already used.

## Session and architecture

Repository/use cases/ViewModel/UI are separate files; Koin wires them in AuthModule. Domain types contain no Supabase SDK/session token types. SDK handles PKCE, session refresh and lifecycle. SessionManager encrypts persisted access/refresh tokens with AES-GCM and an Android Keystore key. Passwords stay in transient form state, are cleared on submission and are never saved in instance state. Backup is disabled in the existing manifest. No auth request bodies, credentials or tokens are logged.

Custom-scheme redirects are for this Android MVP. Before public distribution, consider verified HTTPS App Links for callbacks. Production account lifecycle (account deletion and associated cloud data policy) is a separate feature to finish before publishing a service that creates accounts.

## Verification

Run `gradlew.bat :app:assembleDebug :app:testQaUnitTest :app:lintDebug`.

Unit tests cover email normalization, password rules, duplicate submission, confirmation-required signup, errors/retry/cancellation, Google launch without inventing an authenticated user, missing configuration and password-recovery UI routing. Device/server verification still requires configured redirect URLs and Google provider: real signup/confirmation, valid login, wrong password, Google accept/cancel, reset completion, session restoration after process death, token refresh, offline diary, signout and switching accounts without misrepresenting local data as synced.

References: https://supabase.com/docs/reference/kotlin/installing and https://supabase.com/docs/guides/auth/social-login/auth-google

Instrumented tests target the separate debuggable `com.llsit.sipduddy.qa` package (`connectedQaAndroidTest`) so automatic test cleanup cannot uninstall the regular diary app. Never change testBuildType to debug on a device containing personal records.
