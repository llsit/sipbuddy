# SipBuddy

Native Android hydration diary. Kotlin, Jetpack Compose Material 3, MVVM, StateFlow, Koin, Room and Preferences DataStore. Android 9+ (API 28). No account or internet permission.

## Structure

- `domain/model/`: one named model per file, including `DrinkType`, `DrinkEntry`, `HydrationSettings`, `ReminderSettings`, `ReminderTime` and `HydrationOverview`.
- `domain/repository/`: separate `DrinkRepository` and `HydrationSettingsRepository` contracts.
- `domain/usecase/`: one operation per `…UseCase.kt`, including adding/deleting drinks, observing the overview, calculating weekly totals/next reminder, saving settings and restoring/handling reminders.
- `domain/time/` and `domain/reminder/`: small contracts for date changes and platform reminder adapters.
- `data/local/`: separate Room entity, DAO, database and DataStore keys/provider.
- `data/repository/`: `RoomDrinkRepository` and `DataStoreHydrationSettingsRepository` implement the independent contracts.
- `data/mapper/`, `data/time/`, `data/reminder/`: storage mapping, date observation, alarm scheduling, notification publishing and receiver, each in a separate file.
- `presentation/model/`, `screen/`, `component/`, `mapper/`, `formatter/`, `notification/`: UI state, individual screens/components, display text/date formatting and Android permission handling.
- `di/`: separate data/domain/presentation Koin modules, composed by `AppModule.kt`.

Dependency direction: presentation → domain ← data. DI connects implementations to domain contracts. Layers are packages within the app module for this MVP.

## Naming and responsibilities

Use cases end in `UseCase`, repositories end in `Repository`, Room persistence models end in `Entity`, DAOs end in `Dao`, mappers end in `Mapper`, ViewModels end in `ViewModel`, and UI state ends in `UiState`. A top-level class/interface/model lives in its matching file. Related presentation extensions share a file only when they serve the same mapping responsibility.

The ViewModel only orchestrates use cases and UI state; it does not access Room/DataStore, read the system clock, aggregate records, or hold Composable callbacks. A `Clock` and `DateProvider` are injected, repositories have independent contracts, and platform reminders use small scheduler/publisher interfaces. Tests substitute these contracts with fakes without a mocking framework. Concrete use cases do not need duplicate interfaces merely for testing.

## Behavior

Home shows today's actual beverage volume and animated Canvas water-drop mascot. Add water, tea, coffee, milk, juice or another beverage (1–5,000 ml). All beverage types count their entered volume equally; no medical hydration coefficients. Goal accepts 250–10,000 ml. History shows today and six previous local calendar days; deleting requires confirmation. Room queries only the visible time range, using an inclusive start and exclusive end; records remain stored beyond that range. Local date/timezone refreshes while subscribed and changes re-query/regroup the records. Totals use Long to avoid integer overflow. Existing database/table/column names and DataStore keys are preserved.

Duplicate actions are rejected before launching a coroutine. Failed adds keep the sheet open; read failures have an explicit retry. Settings that persist successfully but fail to schedule an alarm produce a partial-success warning. Coroutine cancellation is propagated rather than shown as an error. Repeated snackbar messages have distinct IDs and acknowledge only the matching message. Unknown persisted drink types remain visible as another beverage and can still be deleted.

Settings use DataStore. Reminder is once a day at the selected local time, using an inexact idle-compatible alarm. Android may delay delivery. Permission denial does not block diary use. Open system notification settings if permission was permanently denied or the notification channel is disabled. Boot, timezone/time changes and app updates reschedule the alarm; force-stop requires reopening the app. No exact-alarm permission is required.

## Verify

`gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`

`gradlew.bat :app:connectedDebugAndroidTest` runs the Room in-memory DAO test on a connected device/emulator. Unit tests under `app/src/test/` cover use cases, mappers, repository adapters and ViewModel concurrency/retry/error handling. Room SQL and actual alarm delivery require a device/emulator.

Device checks: add each beverage, relaunch for persistence, delete a record, change goal, midnight rollover, grant/deny notifications, set reminder a few minutes ahead, reboot and verify rescheduling, large fonts and small screens.
