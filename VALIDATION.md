# Komprexo — PHASE 2 validation

Repository: digitalfuturesolutions69/komprexo. Authorized branch: work.
Starting local/remote HEAD verified: b8431dc198d9723a01cb97f6ff7aca6f3d8e491f.
The owner accepts previous phases and reports successful physical Samsung S9 and
itel testing of Phase1.6. That is owner-reported evidence, not a test performed by
this agent. New Phase2 workflows require owner physical review.

Implemented native/offline batch compression, format conversion, independent
resize, editable smart presets, individual/folder save and multi-image sharing.
The accepted compression engine/search and original protection are retained.
Application ID com.komprexo.app; min23 / target36. No backend, database, website,
account, telemetry, ads, billing, subscription, merge, release or publication.

[Processing contracts, limits, preset values and ownership](docs/PHASE2_DESIGN.md).
Previous reports are preserved in docs/PHASE16_VALIDATION.md and earlier archives.

## Local validation

JDK17, Gradle8.11.1, AGP8.9.2, Kotlin2.1.20, SDK36; native Robolectric graphics.
Supported temporary caches/proxy credentials, no secrets stored in the repository.

```sh
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

Actual completed local suite:68 tests, zero failures/errors/skips; all previous43
retained.25 shared checks exercise converters, alpha confirmation, real decoding,
resize ratios/invalid dimensions/orientation/device limits, editable deterministic
presets and enforced targets, sequential/partial/cancelled batches, duplicate
output names, native/concurrent guards, temporary cleanup, folder and multi-share
bytes/URIs, revoked permissions, export partial success/storage/cancellation and
share limits. SAF/gallery providers are test doubles, not OEM implementations.
Local instrumentation cannot run: /dev/kvm is unavailable. Four-API CI is required.

Early builds found an API24-only isTreeUri call and percent-string formatting;
these were corrected using AndroidX compatibility and plain wording, without
suppressing lint. An instrumentation fixture chained a void Intent setter; that
compilation error was corrected. Grouped result rendering required materializing
its indexed iterable as a list. These failed build attempts are not claimed PASS.
Final local build PASS: production/JVM/instrumentation Kotlin compilation,68 JVM
tests (0 failures/errors/skips), lint (0 errors;5 UseKtx suggestions, including2
existing), and app/test APK assemblies. APK identity/permissions/signature checked:
Komprexo / com.komprexo.app / min23 / target36; no Internet or storage permission.
Wrapper SHA256 matches trusted Gradle8.11.1:
2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046.
No lint suppression, test disabling or dependency/security bypass was added.

## Performance

The measured native-Robolectric10-image benchmark is saved in
[PHASE2_PERFORMANCE.json](docs/PHASE2_PERFORMANCE.json) after final local execution.
Measured1457ms for10 sequential640x480 JPEGs (336391bytes each), producing
2046260 output bytes in total under200KiB per-image targets. JVM heap snapshots
232564632→234104416bytes; max heap536870912bytes.
It repeats a versioned synthetic JPEG and measures compression/reinspection/output
IO, excluding selection/import/export/UI. Heap before/after are JVM snapshots,
not peak memory or native RSS. Results are not physical-device performance claims.
All full pixel processing remains sequential; native gate regression observes
maximum one concurrent injected decoder across four concurrent transform requests.

## CI and acceptance

Existing workflow/security checks are retained: Java17, SDK36, trusted/pinned
Gradle Wrapper validation, Kotlin, JVM tests, lint, app/test APKs and artifact upload.
Device matrix remains API23/26/28/36, compact320x569dp. Five new UI checks exercise
home tools, multiple picker results/removal/clear, partial success, folder save and
multi-share, converter consent/individual export, large-font resize/rotation and
Cancel. Earlier UI checks still execute the accepted Compress workflow after
explicitly navigating from the new Home; no assertions/tests are disabled.
CI outcome/artifacts are pending push and actual execution. No CI PASS claimed yet.

Known limits:20 images/32MiB each/256MiB selected imports are initial safety limits,
not monetization limits. Exact resize/converter dimensions can exceed heap or WebP
limits and fail visibly rather than silently downscale. File-size compression can
reduce resolution further; conversion may increase bytes. HEIF remains unsupported.
Cancellation is cooperative around native codecs/provider IO. Process-death resume
is not implemented. Failed SAF writes can leave a partial external document if
permission prevents cleanup. Sharing retains copies for24 hours within128MiB.
Do not infer physical/OEM compatibility or platform preset compliance from emulators.
Stop after PHASE2 and wait for owner approval; no subsequent phase implemented.
