# SipBuddy coding instructions

- Preserve the MVI + SOLID architecture and Koin dependency injection. Keep models, repositories, mappers and use cases in separate files with their appropriate suffixes.
- After every source-file addition or code edit, reformat the changed files and optimize their imports before reporting completion. Remove unused/duplicate imports and order imports consistently. Preserve imports needed for Kotlin extension functions and Compose delegated properties.
- Run `./gradlew.bat formatKotlin checkKotlinFormat` for Kotlin and Gradle Kotlin files. The pinned formatter and `.editorconfig` define the shared style. Apply the same formatting/import cleanup to new files before finishing; do not defer it to a later task.
- Reformat edited Android XML with Android Studio Reformat Code (or its bundled command-line formatter). Do not format generated code, build outputs, caches, IDE metadata or private configuration.
- Run build/lint and relevant existing tests after bulk formatting or import cleanup. Use the isolated QA package for instrumentation; never uninstall or clear the user's regular app.
- Preserve unrelated user changes. Never commit `.idea`, local credentials, signing keys, generated builds or personal database files.

- Presentation uses `onIntent(...)` as the only UI command entry point, one immutable StateFlow per feature, and pure reducers for every state transition. Keep Intent, Mutation, Reducer and UI Effect contracts in separate named files. UI effects must be lifecycle-aware; retain actionable Snackbar/Undo effects until an intent acknowledges them. Do not add public action methods alongside onIntent.
