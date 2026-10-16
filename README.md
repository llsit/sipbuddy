# SipBuddy

Native Android hydration diary. Kotlin, Jetpack Compose Material 3, MVVM, StateFlow, Koin, Room and Preferences DataStore. Android 9+ (API 28). No account or internet permission.

## Structure

- `domain/`: Android-independent models, repository/scheduler contracts, use cases, daily aggregation and time calculations.
- `data/`: Room entities/DAO/database, repository implementation, DataStore and Android alarm/notification integration.
- `presentation/`: lifecycle-aware StateFlow ViewModel and Compose screens.
- `di/`: Koin composition root. Application starts Koin once; database, DAO, repository and scheduler are singletons.

Dependency direction: presentation → domain ← data. DI connects implementations to domain contracts. Layers are packages within the app module for this MVP.

## Behavior

Home shows today's actual beverage volume and animated Canvas water-drop mascot. Add water, tea, coffee, milk or juice (1–5,000 ml). All beverage types count their entered volume equally; no medical hydration coefficients. Goal accepts 250–10,000 ml. History shows today and six previous local calendar days; deleting requires confirmation. Records remain in Room beyond the visible seven-day window. Local date/timezone refreshes while the screen is subscribed.

Settings use DataStore. Reminder is once a day at the selected local time, using an inexact idle-compatible alarm. Android may delay delivery. Permission denial does not block diary use. Open system notification settings if permission was permanently denied or the notification channel is disabled. Boot, timezone/time changes and app updates reschedule the alarm; force-stop requires reopening the app. No exact-alarm permission is required.

## Verify

`gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`

Device checks: add each beverage, relaunch for persistence, delete a record, change goal, midnight rollover, grant/deny notifications, set reminder a few minutes ahead, reboot and verify rescheduling, large fonts and small screens. Unit tests cover domain rules; Room and actual alarm delivery require a device/emulator.
