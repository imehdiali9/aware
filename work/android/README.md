# Present — native Android attendance tracker

A Kotlin / Jetpack Compose app for personal attendance tracking. No WebView, account, network permission, or remote backend.

## Screens

- Introduction: name, college, department, programme, admission year and semester.
- Semester setup: subjects and a personal attendance target.
- Day: weighted overall attendance, forecasts, date selector and present / absent / cancelled controls. Tap the selected status again to undo it.
- Subjects: individual percentages, forecasts and editable starting counts.
- Week: recurring periods, native Android time/date pickers and schedule editing. Historical attendance is preserved when a period is removed.
- You: profile and target editing, JSON export and confirmed restore through the Android system file picker.

Data is saved atomically in the app's private files directory. The app supports Android 8.0 (API 26) and later. It starts empty; test/demo records are never bundled in the APK.

## Open and build

Open this folder in Android Studio. Use **JDK 21** as the Gradle JDK. The checked-in wrapper uses Gradle 8.14.5 with Android Gradle Plugin 8.13.2. Do not independently upgrade only one of these versions.

Let Android Studio create `local.properties` for the installed Android SDK, then sync the project.

```text
gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

The installable development APK is produced at `app/build/outputs/apk/debug/app-debug.apk`. This is a debug-signed development build, not a Play Store release.

UI tests run on a dedicated emulator. They reset this app's data and use fictional records; don't run them on a phone with attendance you want to keep.

```text
gradlew.bat :app:connectedDebugAndroidTest
```

## Scope

The attendance target is user-selected, not an authoritative interpretation of CUSAT regulations. No condonation or exam eligibility decision is made. Starting counts should include only periods before recording began in the app. Cancelled/unmarked periods are excluded. A future semester is not automatically reset; keep a backup before reorganising your subjects.

The Android JSON backup format is versioned independently from the earlier web prototype.
