# Komprexo

Phase 2 — offline image tools for native Android.
Compress one image or a sequential batch of up to 20; convert JPEG/PNG/WebP;
resize without upscaling; edit smart presets; save copies or share multiple URIs.
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

Quality-first and Auto are recommended defaults. Balanced retains the lower-memory
legacy behavior. The compact UI follows system light/dark themes and keeps
Save/Share visible while comparing one preview.

Device CI tests API 23, 26, 28 and 36 at 320 × 569 dp, including 200% font scaling; debug CI uploads the debug APK for 14 days.
No tests, lint or wrapper validation are disabled. No merge or release is made.

See [engine design](docs/COMPRESSION_ENGINE.md) for byte units, memory limits,
quality search, format behavior, cancellation, metadata and temporary-file policy.
See [actual validation](VALIDATION.md) for test counts and verified CI evidence.
Phase 0 and Phase 1 reports are preserved in docs/ and Git. Phase 2 adds no services, accounts, telemetry, database, ads, billing or subscriptions.
See [Phase 2 contracts](docs/PHASE2_DESIGN.md) for presets, exact-dimension safety,
queue limits, secure folder export and temporary-file ownership.
