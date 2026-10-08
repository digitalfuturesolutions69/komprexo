# Komprexo

Phase 0 — Technical Foundation only. A native Android Kotlin application with a
launch shell and debug APK CI. No product features, website, backend, database,
authentication, advertising, or billing are included.

## Identity and toolchain

- Name: Komprexo
- Proposed application ID / namespace: `com.komprexo.app` (final owner approval pending)
- Android minimum API 23; compile / target API 35
- JDK 17, Gradle Wrapper 8.11.1, Android Gradle Plugin 8.9.2, Kotlin 2.1.20

## Build and tests

Install JDK 17 and Android SDK platform 35 / build tools 35.0.0. Set
`ANDROID_HOME` or create an untracked `local.properties` containing `sdk.dir`.

```sh
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
./gradlew --no-daemon connectedDebugAndroidTest
```

The second command needs an API 23+ emulator or device. The instrumentation smoke
test verifies launching and recreating the activity, the displayed name, and the
proposed application ID. A Robolectric unit smoke test also verifies native screen inflation on API 35.
There is no domain logic in Phase 0.

The wrapper JAR and launch scripts come from the official Gradle v8.11.1 tag.
The JAR matches the official release checksum, and the distribution ZIP checksum
is pinned in wrapper properties. CI validates the wrapper before use.

GitHub Actions runs Kotlin compilation, unit tests, build/lint and API 35 emulator tests on pushes and pull
requests. The debug job uploads `app/build/outputs/apk/debug/app-debug.apk` as
`komprexo-debug-<commit SHA>` with 14-day retention. Download it from the workflow
run's Artifacts section. It is debug signed and is not a release APK.

## Phase boundary

Stop after this foundation. Owner approval is needed before finalizing the
application ID, merging into main, publishing releases, or deploying. Future
features, monetization and service integration belong to later phases.
