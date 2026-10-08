# Komprexo Phase 1 — actual validation

Date: 2026-10-08 (Asia/Jakarta). Existing repository:
https://github.com/digitalfuturesolutions69/komprexo. Branch: `work`.
Starting commit: `c9fef373d085b75a3944fd8bb5c6aa75dab3385a`.
Verified Phase 0 source: `f1f247d80dd65d57f5300919b5cff969bcff53a1`.
Phase 0 report is retained in docs/PHASE0_VALIDATION.md and Git history.

## Implemented Phase 1

Single-image Compose selection, target presets/custom binary KB, local encoding,
normalized original/result previews, actual byte sizes/reduction/dimensions/format
and target status, cancellation, SAF save, and FileProvider share. No original is
opened for writing. All typed failure messages have English and Indonesian text.

Algorithm and limitations: docs/COMPRESSION_ENGINE.md. Quality is searched
exhaustively from 100 to 35; then dimensions shrink by 0.8, always from normalized
source pixels. PNG uses one quality trial. Auto preserves alpha with WebP; explicit
JPEG uses white background and warns about transparency removal. Actual bytes,
overflow flags and decoded output bounds must pass before accepting a result.

## Actual local results

Command (JDK 17, existing wrapper, supported network grant, writable /tmp caches):

```sh
./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

BUILD SUCCESSFUL. A local-only init script configures Robolectric's writable home
and inherited proxy/CA trust; application source contains no environment-specific
proxy configuration.

- Wrapper: PASS, official Gradle 8.11.1 JAR SHA-256
  `2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046`.
  The retained wrapper distribution checksum is pinned; wrapper previously ran
  successfully and is used for these checks. CI wrapper-validation remains enabled.
- Kotlin compilation: PASS, production/unit/instrumentation sources compile.
- JVM: 27 tests, 0 failures, 0 errors, 0 skipped (26 native-graphics engine/storage
  checks plus 1 Compose host/identity smoke test).
- Target-size checks: all six presets, custom 17389 bytes, actual file length,
  highest fitting integer JPEG quality, dimension reduction and unreachable 1-byte
  PNG target. Output is really encoded/decoded, not mocked.
- Formats: JPEG, PNG, WebP, alpha-preserving Auto, explicit JPEG alpha-to-white,
  small source, corrupted JPEG and unsupported GIF fixture rejection.
- EXIF: rotated fixture dimensions/color placement and all 8 transforms checked
  against a four-color pixel oracle; orientation metadata is absent/normal after
  encoding. Aspect ratio checked with integer rounding tolerance.
- Memory: versioned 4096x2048 fixture sampled before decode; actual bitmap
  allocation <=2 million pixels x 4 bytes, sampling edge cases and >32 MiB source
  rejection checked. Current available heap can further reduce this ceiling.
- Errors/storage: cancellation removes job output, unreachable targets leave no
  accepted files, blocked output directory, ENOSPC copy failure, invalid provider,
  content-URI save/import, native encoder ENOSPC stream fault injection and concurrent output isolation checked.
- Integrity/security: outputs decode with reported dimensions; source bytes remain
  unchanged; secure shared content URI read matches output bytes; FileProvider
  rejects access to private result directories.
- Lint: 0 errors, 4 warnings (2 PluralsCandidate, 2 UseKtx) and 1 informational
  AutoboxingStateCreation recommendation. No lint checks were suppressed/disabled.
- Debug APK and test APK assembly: PASS. Local app APK:
  `/workspace/komprexo/app/build/outputs/apk/debug/app-debug.apk`.
  SHA-256: `c41ad68023e0f0e65c34deb023d38ae5e39d5c475dd7ca3c4202053578daf33e`.
  apksigner verification succeeds; META-INF/JAR metadata warnings are retained.
- APK identity: Komprexo; `com.komprexo.app`; minSdk 23; compile/target 36;
  versionName 0.2.0. Final owner application-ID approval remains pending.
- Permissions: merged APK declares only AndroidX's app-specific signature-level
  `com.komprexo.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. No INTERNET,
  READ/WRITE_EXTERNAL_STORAGE, READ_MEDIA_IMAGES or other broad media access.
  FileProvider is unexported and scopes sharing to cache/shared. Backup and device
  extraction are excluded. No network image code, telemetry, backend or database.
- Static checks: fixtures' versioned SHA-256 manifest matches all files. Wrapper
  shell syntax and Git whitespace checks pass.

## Confirmed CI evidence and artifact

Verified application/workflow source commit:
`86200a360685fc74282c59a27b6654b7531df284`.

https://github.com/digitalfuturesolutions69/komprexo/actions/runs/37758485541

GitHub returned completed / success. All three jobs succeeded:

- debug: wrapper validation, Kotlin, JVM tests, lint, app/test APK assembly and upload.
- device-test (23): 29 tests executed, BUILD SUCCESSFUL.
- device-test (36): 29 tests executed, BUILD SUCCESSFUL.

The CI report artifact was downloaded and inspected: JVM index records 27 tests,
0 failures, 0 ignored. Local JUnit XML also records 27 tests, 0 failures/errors/skips.
Device logs explicitly record 29 tests started/finished on each API emulator.
System activity-result dialogs are stubbed in the Compose workflow check; codecs,
engine, URI storage, preview state, original integrity and save/share execute for
real. Local device tests are not claimed: /dev/kvm is unavailable.

Confirmed debug APK artifact:
`komprexo-debug-86200a360685fc74282c59a27b6654b7531df284`

- Artifact ID: 11541153251, ZIP size 9226786 bytes, not expired at verification.
- Download: https://github.com/digitalfuturesolutions69/komprexo/actions/runs/37758485541/artifacts/11541153251
- Upload path: app/build/outputs/apk/debug/app-debug.apk.
- Retention: 14 days; expires 2026-10-22 16:44:11 Asia/Jakarta.
- ZIP digest: sha256:d2a0d85464001707305b8eb8228ed9919e4f82af59f5da56bafc7db59b54bb93.
- JVM/lint and both device report artifacts also exist on the run.

The earlier implementation run 37757242876 also succeeded (26 JVM tests and
28 device tests per API before adding the native ENOSPC test). No test/lint check
was disabled to pass. Existing Gradle Wrapper and CI structure are retained, with
SDK 36 and API 23/36 matrix coverage.

Technical Phase 1 acceptance: PASS for the verified application/workflow source.
This follow-up changes only delivery documentation; application, tests, resources,
Gradle and workflow are identical to that verified source. The delivery commit
SHA is obtained with git rev-parse HEAD and compared with origin/work. Its own
CI will also be checked before the final execution report is delivered. This
file does not fabricate a not-yet-existing delivery-run ID or self commit hash.

## Known limitations

- Native codec calls/provider reads cannot always stop immediately; cancellation
  is cooperative and cleans owned files when control returns.
- If the process is killed, encoding cannot resume; retained saved-state marks
  interrupted work and the user must select/recompress. Android cache eviction
  can remove unsaved outputs.
- Input limits: 32 MiB, 128 MP, 32768 pixels per side. Decoded resolution <=2048
  per edge and <=2 MP (possibly lower under heap pressure). This trades detail for
  memory safety. The budget bounds bitmap allocations, not total native RSS.
- UI custom targets: integer 1–10240 KB. Tiny engine targets can be unreachable.
  Some small sources grow after fresh encoding; reductions remain accurate.
- WebP uses device support and exports one decoded frame. SDR ARGB normalization
  does not promise HDR/wide-gamut/high-bit-depth fidelity.
- Shared copies remain usable for receivers and are purged at next launch after
  24 hours, capped at 128 MiB; externally saved copies are never pruned.
- A failed provider save can leave a partial destination document; externally
  owned files are not deleted automatically.
- System dialog/OEM UX, arbitrary adversarial files and physical-device memory
  pressure need broader QA beyond these synthetic fixtures and emulator checks.
- gh CLI credential check is still invalid; authorized Git transport/connector
  work with supported environment credentials. No token was exposed or replaced.

No batch mode, website, backend, database, account, cloud storage, PDF features,
AdMob, Billing, Play publication, production release, force push or branch merge.
Stop after Phase 1. Phase 2 requires owner approval.

## Delivery file inventory

Compared with starting commit c9fef373d085b75a3944fd8bb5c6aa75dab3385a:

### Files created (26)

- app/src/androidTest/java/com/komprexo/app/CompressionDeviceTest.kt
- app/src/androidTest/java/com/komprexo/app/LaunchTest.kt
- app/src/androidTest/java/com/komprexo/app/WorkflowTest.kt
- app/src/main/java/com/komprexo/app/compression/AndroidCompressionEngine.kt
- app/src/main/java/com/komprexo/app/compression/BitmapCodec.kt
- app/src/main/java/com/komprexo/app/compression/CompressionModels.kt
- app/src/main/java/com/komprexo/app/storage/ImageStorage.kt
- app/src/main/java/com/komprexo/app/ui/CompressionViewModel.kt
- app/src/main/java/com/komprexo/app/ui/KomprexoScreen.kt
- app/src/main/res/values-in/strings.xml
- app/src/main/res/xml/data_extraction_rules.xml
- app/src/main/res/xml/file_paths.xml
- app/src/sharedTest/java/com/komprexo/app/EngineChecks.kt
- app/src/test/assets/README.md
- app/src/test/assets/alpha.png
- app/src/test/assets/corrupt.jpg
- app/src/test/assets/large.png
- app/src/test/assets/noise.jpg
- app/src/test/assets/noise.webp
- app/src/test/assets/orientations.png
- app/src/test/assets/rotated.jpg
- app/src/test/assets/sha256.json
- app/src/test/assets/unsupported.gif
- app/src/test/java/com/komprexo/app/CompressionEngineTest.kt
- docs/COMPRESSION_ENGINE.md
- docs/PHASE0_VALIDATION.md

### Files modified (9)

- .github/workflows/android.yml
- README.md
- VALIDATION.md
- app/build.gradle
- app/src/main/AndroidManifest.xml
- app/src/main/java/com/komprexo/app/MainActivity.kt
- app/src/main/res/values/strings.xml
- app/src/test/java/com/komprexo/app/MainActivityTest.kt
- build.gradle

### Files retired/replaced (2)

- app/src/androidTest/java/com/komprexo/app/LaunchTest.java
- app/src/main/res/layout/activity_main.xml

The old XML launch layout and Java launch test were replaced by the required
Compose screen and Compose launch/recreation test; Phase 0 identity/build checks
remain covered. No repository, unrelated branch, release or deployment was made.

Next recommended work: owner review and physical-device/OEM usability QA before
approving a Phase 2 scope. No Phase 2 implementation is authorized or started.
