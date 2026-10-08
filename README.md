# Komprexo

Phase 0 — Technical Foundation only. A native Android Java application with a
launch shell and debug APK CI. No product features, website, backend, database,
authentication, advertising, or billing are included.

## Identity and toolchain

- Name: Komprexo
- Proposed application ID / namespace: `com.komprexo.app` (final owner approval pending)
- Android minimum API 23; compile / target API 35
- JDK 17, Gradle 8.11.1, Android Gradle Plugin 8.9.2

## Build and tests

Install Gradle 8.11.1 and Android SDK platform 35 / build tools 35.0.0. Set
`ANDROID_HOME` or create an untracked `local.properties` containing `sdk.dir`.

```sh
gradle --no-daemon testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
gradle --no-daemon connectedDebugAndroidTest
```

The second command needs an API 23+ emulator or device. The instrumentation smoke
test verifies launching and recreating the activity, the displayed name, and the
proposed application ID. There is no domain logic or unit test suite yet.

GitHub Actions runs build/lint and API 35 emulator tests on pushes and pull
requests. The debug job uploads `app/build/outputs/apk/debug/app-debug.apk` as
`komprexo-debug-<commit SHA>` with 14-day retention. Download it from the workflow
run's Artifacts section. It is debug signed and is not a release APK.

## Phase boundary

Stop after this foundation. Owner approval is needed before finalizing the
application ID, merging into main, publishing releases, or deploying. Future
features, monetization and service integration belong to later phases.
