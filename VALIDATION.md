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

## CI and acceptance status at implementation commit

Existing CI retained with wrapper validation, JDK/SDK setup, Kotlin compilation,
unit tests, lint, APK upload and device tests. SDK platform now 36; emulator matrix
is API 23 and 36. No tests, lint or security validation have been weakened.

Local device tests are not claimed: /dev/kvm is unavailable. Device tests run in
GitHub Actions, including actual engine/storage checks and Compose selection →
compression → preview → save/share. System activity-result dialogs are stubbed;
engine, codecs and storage execute for real. The initial implementation source commit
`83f79cfc98f7a76d012fcd624cecce258af14d8d` passed run 37757242876:
https://github.com/digitalfuturesolutions69/komprexo/actions/runs/37757242876
The debug job and both device jobs succeeded. Each API emulator executed
28 tests, including the complete Compose selection/save/share workflow.
The confirmed APK artifact was `komprexo-debug-83f79cfc98f7a76d012fcd624cecce258af14d8d`,
artifact ID 11540094992. The follow-up native IO guard adds one meaningful test
per suite (27 JVM, expected 29 per device); final CI evidence will be recorded
after those jobs complete. Acceptance for the final delivery is pending until
that exact commit's workflow succeeds and its uploaded artifact is confirmed.

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
