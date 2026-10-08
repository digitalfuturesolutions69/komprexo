# Komprexo — PHASE 1.6 validation

Repository: digitalfuturesolutions69/komprexo. Branch: work. Date: 2026-10-08 UTC.
Starting commit: 907b7279d9759f4e32c99f676c7883f6fa0fac47.
Application ID com.komprexo.app; minSdk 23 / targetSdk 36. Existing implementation
extended; no merge, release, telemetry, Internet permission or unrelated feature.
Previous validation is preserved in docs/PHASE15_VALIDATION.md.

## Investigation and verified defects

The owner's actual Samsung S9 failure stage and cause remain unknown. No physical
S9, original failing image or device diagnostic trace was available. Two native
baseline regression probes passed before fixes, demonstrating these real defects:

1. A valid JPEG with trailing payload decoded successfully with BitmapFactory but
   was rejected by the former last-two-bytes EOI check. Camera/motion-photo files
   can have trailers; the fixture tests generic trailers, not a Samsung photograph.
2. A valid 20000x40 PNG could not be encoded by the native WebP encoder (maximum
   dimension 16383). The engine incorrectly labeled this encoder failure as corrupt
   input. Neither defect is claimed as the proven cause of the owner's S9 issue.

JPEG validation now parses bounded buffered markers, requires a main scan and its
EOI, allows trailing data, and rejects truncated main images even when embedded
thumbnail bytes contain EOI. Explicit WebP dimensions are bounded to 16383 without
changing the chosen format. Auto considers original-resolution JPEG/PNG first.
API 23–29 uses legacy WebP; API 30+ uses explicit lossy WebP. BitmapFactory software
ARGB8888 decoding remains the common API 23–36 path; hardware ImageDecoder is not
introduced. Phase 1.5 adaptive budgets, alpha handling, original preservation,
quality search and actual-byte guarantees are retained.

Imports use ContentResolver streams, accept unknown lengths and descriptor
offsets, and do not require filesystem paths or optional provider metadata.
Declared MIME is diagnostic-only; actual signatures determine JPEG/PNG/WebP.
HEIC/HEIF is explicitly unsupported, including on APIs with platform support;
users receive export-to-JPEG/PNG guidance instead of a corruption accusation.
Errors distinguish invalid URI, revoked permission, unreadable provider, malformed
structure, unsupported format, decoder, encoder, output validation and memory.
Indonesian and English UI messages explain appropriate recovery.

## Safe diagnostics and physical retest

Debug builds emit local JSON tagged KomprexoDiagnostics: API, sanitized manufacturer
and model, processing stage, whitelisted declared MIME, decoder/encoder, exception
category and allocation category. No URI, file path, exception message/stacktrace,
image bytes, personal EXIF or document contents are logged. Release builds do not
emit these events. No remote transport is added.

On the S9, record Android version, whether failure follows selection or Compress,
input format and explicit output choice. Reproduce with this debug APK, then collect
only the structured tag using `adb logcat -d -s KomprexoDiagnostics:D '*:S'`.
Retest the original image with WebP and, separately, explicit JPEG/PNG; retain the
original. A real S9 retest remains required before claiming physical compatibility.

## Actual local verification

JDK17, Gradle8.11.1, SDK 36; supported temporary caches/proxy. Command:

```sh
./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

- PASS: 43 JVM tests, zero failures/errors/skips. Previous32 tests retained.
- PASS: production/JVM/instrumentation Kotlin compilation; both APK assemblies.
- PASS: Android lint, zero errors; two existing UseKtx warnings.
- PASS: trusted wrapper JAR SHA256
  2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046;
  pinned distribution checksum and CI wrapper validation retained.
- App APK exists: app/build/outputs/apk/debug/app-debug.apk; SHA256
  1296a06a5078eb7603ec9251327d412dc8c90183b7f52b2442304c7c19318cce.
  APK identity Komprexo / com.komprexo.app / min23 / target36 verified.
  Only AndroidX app-specific receiver permission; no Internet/storage permissions.
- Local device tests not executed: no /dev/kvm. CI matrix expanded to23,26,28,36.
- git diff --check passes. Git transport/connector access uses supported credentials.

Eleven shared compatibility checks cover Gallery-like test-provider streams,
offsets, missing/incorrect MIME, JPEG/PNG/WebP, unsupported HEIF signature,
revoked permission, invalid URI, trailer/truncation, WebP dimensions, decoder and
encoder failures, both modes under memory pressure, classification and log privacy.
The provider is a test double, not Samsung Gallery. Instrumentation uses actual
non-seekable pipes; JVM uses unknown-length file descriptors because Robolectric
pipe shadows raced and returned premature EOF. An initial compilation error and
one such JVM fixture failure were corrected; the complete 43-test run then passed.
The HEIF probe is a signature fixture, not a valid full HEIC photo.

## CI / acceptance

First CI run 37779550450 for c3a4b07e75c563c3f5f596599b6a178d95b52818
passed debug build/upload but failed device suites before completion. The test-only
provider ran in its own process; its Kotlin runtime references were absent from
the test APK (supplied only by the target APK during instrumentation). It is now
self-contained Java. The corrected local 43-test/lint/APK run passes. The failed/incomplete run is not counted as acceptance.

Verified corrected source: 65117e4e110c5f3784b2037aca3eed712eb3302f.
CI https://github.com/digitalfuturesolutions69/komprexo/actions/runs/37780513224
completed / success. All five jobs succeeded, including wrapper validation:

| Suite | Actual tests | Failures | Skips |
|---|---:|---:|---:|
| JVM | 43 | 0 | 0 |
| API 23 | 47 | 0 | 0 |
| API 26 | 47 | 0 | 0 |
| API 28 | 47 | 0 | 0 |
| API 36 | 47 | 0 | 0 |

Downloaded HTML reports confirm these counts. The previous 36 instrumentation
checks pass on every API; the 11 new shared compatibility checks pass too. Native
pipe reads, JPEG/PNG/WebP workflows, exceptions, permissions, memory pressure,
compact 320x569dp, 200% fonts, touch targets, Save/Share and theme/bar pixel checks
are exercised. These emulator results do not establish physical Samsung behavior.
CI lint: zero errors, 11 GradleDependency update warnings and 2 existing UseKtx
warnings; no checks disabled. Local lint has only the 2 UseKtx warnings.

Confirmed artifact name:
komprexo-debug-65117e4e110c5f3784b2037aca3eed712eb3302f
https://github.com/digitalfuturesolutions69/komprexo/actions/runs/37780513224/artifacts/11552580451
ZIP 9,253,039 bytes; downloaded app-debug.apk 9,680,413 bytes; APK SHA256
3929e080b1c5aa61ee02c1677d32c968cbda08192d09922467a58991ee50c899.
APK signature verification passes (existing META-INF metadata warnings remain).
Artifact retention 14 days; reports and screenshots also exist.

GitHub authentication and Git push operate with supported environment credentials.
Local/remote work matched the verified source. This documentation-only delivery
commit receives its own CI run and synchronization verification, reported in the
final execution response; this document does not invent its own commit/run ID.
Automated technical acceptance: PASS for the verified source. Physical S9
acceptance remains PENDING. No subsequent phase, merge or publication performed.
All prior UI, system-bar, large-font and compression tests remain enabled.
Physical S9 acceptance pending, regardless of emulator outcome. HEIF/HDR/animated
support is not added. Native codec memory accounting remains an estimate with
bounded pixels and typed OOM handling; device-native codecs can differ.
Stop after PHASE 1.6 and wait for owner approval.
