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

Home shows today's actual beverage volume and animated Canvas water-drop mascot. Add water, tea, coffee, milk, juice or another beverage (1–5,000 ml). All beverage types count their entered volume equally; no medical hydration coefficients. Goal accepts 250–10,000 ml. History shows today and six previous local calendar days; deleting requires confirmation and offers a snackbar Undo. Editing changes beverage/volume while preserving the original ID and timestamp. Undo restores the exact deleted record using a Room transaction and insert-on-conflict-ignore; it cannot overwrite another record. The latest Undo is available while its snackbar is shown and retained across rotation by the ViewModel, but not after process death. Room queries only the visible time range, using an inclusive start and exclusive end; records remain stored beyond that range. Local date/timezone refreshes while subscribed and changes re-query/regroup the records. Totals use Long to avoid integer overflow. Existing database/table/column names and DataStore keys are preserved.

Duplicate actions are rejected before launching a coroutine. Failed adds keep the sheet open; read failures have an explicit retry. Settings that persist successfully but fail to schedule an alarm produce a partial-success warning. Coroutine cancellation is propagated rather than shown as an error. Repeated snackbar messages have distinct IDs and acknowledge only the matching message. Unknown persisted drink types remain visible as another beverage and can still be deleted.

Settings use DataStore. Reminders support 1–12 distinct local times (default 10:00, 14:00, 18:00). An inexact idle-compatible alarm schedules the next eligible slot, then the receiver schedules the following slot. Configurable quiet hours default to 22:00–07:00 and support crossing midnight; their end is inclusive for reminders, while equal start/end means no quiet interval. The optional stop-at-goal setting defaults on: delivery checks current local-day Room totals and suppresses notifications at/above the goal, resuming on the following day or when edits/deletions put today below goal. Future alarms remain scheduled so reaching the goal does not permanently disable reminders. Existing installations retain their old single time until edited. Android may delay delivery. Permission denial does not block diary use. Open system notification settings if permission was permanently denied or the notification channel is disabled. Boot, timezone/time changes and app updates reschedule the alarm; force-stop requires reopening the app. No exact-alarm permission is required.

## Verify

`gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`

`gradlew.bat :app:connectedDebugAndroidTest --no-configuration-cache` runs the Room in-memory DAO tests on a connected device/emulator. Unit tests under `app/src/test/` cover use cases, mappers, repository adapters and ViewModel concurrency/retry/error handling. Tests include edit/Undo identity and retry, concurrent writes, multi-slot scheduling, DST, quiet boundaries, goal suppression, next-day resume and legacy preference migration. Room SQL and actual alarm delivery require a device/emulator.

Device checks: add each beverage, relaunch for persistence, delete a record, change goal, midnight rollover, grant/deny notifications, set reminder a few minutes ahead, reboot and verify rescheduling, large fonts and small screens.

Android Lint is provided by AGP and configured in `app/build.gradle.kts` and `app/lint.xml`. Errors fail the build, dependency checks and release checks are enabled, and HTML/XML reports are generated under `app/build/reports/`. No baseline or disabled checks hides findings. Run `:app:lintDebug` during development or `:app:lintDebug :app:lintRelease` to explicitly check both variants.

## Motion

Navigation uses a fade-through transition and preserves saved UI state for each screen. The mascot wave pauses behind dialogs and the add/edit sheet. Stable-ID diary rows use lazy-list item fade/placement animations for add/delete/Undo. Editing crossfades the beverage icon/title and animates the amount. Beverage and volume chips gently scale when selected, with animated scale read in the graphics layer. Durations are centralized in `presentation/animation/SipBuddyMotion.kt`; all motion uses Compose's animation clock and system duration scale, with no delay in database writes. No domain or ViewModel logic is coupled to animation completion.

`DrinkMotionTest` exercises selection during animation, editing followed by deletion, and deleting/reinserting the same ID on an Android device using the Compose v2 test rule.

## Physical-device frame measurements

`gradlew.bat :app:assembleBenchmark` creates an optimized, non-debuggable, locally signed `com.llsit.sipduddy.benchmark` APK. Its database/preferences and alarms are separate from the regular app. The benchmark manifest enables shell profiling. Test with synthetic data; the gesture driver adds, edits and deletes records in this isolated package.

Install `app/build/outputs/apk/benchmark/app-benchmark.apk`, put Android SDK platform-tools on PATH (or set `ADB` to adb.exe), then run `python -X utf8 tools/measure_animation.py before`. Run from the project root. Results go to `app/build/reports/frame-benchmark/frames-before`; override the parent with `SIPBUDDY_BENCH_OUTPUT`. After changes, repeat with label `after`. Analyze using `python tools/analyze_animation_frames.py`.

The driver warms destinations and uses real Android input/vsync, without Compose's virtual test clock. It records gfxinfo frame deadlines/render-latency percentiles and a recent SurfaceFlinger actual-presentation sample for the continuously moving mascot. FPS is meaningful for that continuous sample; idle pages do not need 60 new frames per second. Navigation and diary scenarios should be assessed using deadline misses and latency. Diary totals include accessibility-query/automation waits, so they are not per-action animation-only metrics. Keep device state, data and animator duration scale consistent, and repeat runs; one device/run does not establish a universal speedup.

Sources: [Android dumpsys](https://developer.android.com/tools/dumpsys), [Android rendering metrics](https://developer.android.com/topic/performance/vitals/render), [AOSP frame presentation timestamps](https://android.googlesource.com/platform/frameworks/native/+/dc057e52f5/services/surfaceflinger/FrameTracker.cpp).

## Material 3 UI

The cream and blue palette and original Canvas mascot remain. Screens use a centered Material 3 top app bar, icon navigation, themed cards and an extended floating action button for recording drinks. Bottom list padding keeps records accessible above the action button.
