# MVI presentation

The entire presentation layer uses Model–View–Intent. Android ViewModel remains the lifecycle container; it is not specific to MVVM. Domain use cases and repository interfaces, Room, DataStore, Supabase and Koin retain their responsibilities.

## Event flow

Compose sends an `AuthIntent`, `HydrationIntent` or `SyncIntent` to the matching store's only public command entry point, `onIntent`. A command invokes the existing use case, then emits an internal Mutation. The feature's pure Reducer computes a new immutable UiState. The store exposes one read-only StateFlow, collected with lifecycle awareness. Repository/session streams enter through the same reducer path, never by directly replacing state in screens.

Intent, Mutation, Reducer and Effect contracts live in separate files in their named presentation packages. Mutations and reducers are internal implementation details. ViewModels remain named `...ViewModel` for Android/Koin lifecycle integration. There are no parallel public save/delete/login command APIs.

Hydration state includes navigation, delete confirmation, editor, busy/loading/error state and persisted-data overview. Auth state includes session/form/mode/busy/error/confirmation feedback. Sync state includes server/local status and import progress/feedback. Local Compose state is limited to widget/form drafts, focus, animation and OS permission launchers; passwords are not stored in UiState, SavedStateHandle or logs.

## UI effects and lifecycle

HydrationEffect describes Snackbar presentation. A pending message and its Undo snapshot stay in immutable state until `MessageHandled(id)` or `UndoDelete(id)`. The effect flow projects this pending state, so a collector that stops while the app is backgrounded can resume the same unacknowledged message. `repeatOnLifecycle(STARTED)` and `collectLatest` cancel stale Snackbar work when the screen stops or a new effect supersedes it. An old acknowledgement cannot clear a newer message. Auth and Sync confirmations are rendered inline from state and do not need one-shot effects.

Duplicate writes and auth submissions are rejected synchronously before launching a coroutine. Hydration writes are mutex protected; a pressed Undo waits for an in-flight write. Cancellation is propagated and busy state is released in finally blocks. Effects never run repository writes themselves; a user action sends an Intent back to the store.

## Account isolation

The root uses AccountScopeViewModel as a lifecycle holder for its owner-specific ViewModelStore. It retains MVI state, navigation and in-flight work across rotation and disposes the old account stores on owner change or parent ViewModel clearing. Hydration filters both overview settings owner and every entry owner. Queued saves/adds/Undo retain the bound owner. Sync validates the bound/current owner before manual requests/imports and rejects foreign status emissions. No Room schema, cloud RPC, preference format or SQL migration changes are required.

## Verification

Existing hydration/auth behavior tests now drive the same Intent API as Compose. Additional tests cover pure reducer transitions, stale message acknowledgements, effect resubscription/consumption, navigation/delete state, import deduplication/cancellation and stale sync stores. Run `formatKotlin checkKotlinFormat assembleDebug testQaUnitTest lintDebug compileQaAndroidTestKotlin`. Connected UI checks must use the isolated QA package only.
