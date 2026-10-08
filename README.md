# Komprexo

Phase 1 — offline, single-image smart compression for native Android.
Select JPEG, PNG or supported WebP; set a maximum size; compare before/after;
then save a copy or share a content URI. Originals remain untouched.

## Build

JDK 17, checked-in Gradle Wrapper 8.11.1, AGP 8.9.2, Kotlin 2.1.20.
Android min API 23; compile/target API 36. Set ANDROID_HOME to your SDK.
Application ID `com.komprexo.app` is provisional pending final owner approval.

```sh
./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
./gradlew connectedDebugAndroidTest
```

Device CI tests API 23 and 36; debug CI uploads the debug APK for 14 days.
No tests, lint or wrapper validation are disabled. No merge or release is made.

See [engine design](docs/COMPRESSION_ENGINE.md) for byte units, memory limits,
quality search, format behavior, cancellation, metadata and temporary-file policy.
See [actual validation](VALIDATION.md) for test counts and verified CI evidence.
Phase 0 history is preserved in Git; Phase 1 does not add batch processing,
services, accounts, telemetry, database, ads, billing or other excluded features.
