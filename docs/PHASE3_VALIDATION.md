# Komprexo Phase 3 validation — updated owner policy

Repository `digitalfuturesolutions69/komprexo`; authorized branch `work`.
Starting baseline `f4f186363988867c868a6a36d4a30dae7332f5fc` is accepted Phase2.5, including owner-reported physical-device approval.
Previous evidence: [docs/PHASE25_VALIDATION.md](docs/PHASE25_VALIDATION.md).
Application ID remains `com.komprexo.app`, minSdk23/targetSdk36.

## Current acceptance policy

The owner's later Phase3 policy update supersedes unlimited Free single Resize/Convert. Free now has THREE independent daily allowances:

- Compression: five accepted successes/day, shared between single and batch Compression.
- Resize: five accepted successes/day, shared between single and batch Resize.
- Convert: five accepted successes/day, shared between single and batch Convert.
- Each Free batch is limited to two images and its own feature's remaining credits.
- Premium policy: unlimited daily use of all three features; twenty images per batch within existing device/codec/memory/byte limits; Rp49.000 one time.
- Failed images and unused cancelled slots consume no credits; already accepted durable successes count once.

Combined-operation policy: charge exactly once to the primary workflow. Compression with internal resizing/encoding charges Compression; Resize with output-format encoding charges Resize; standalone Convert charges Convert. Classification comes from the processing workflow; Convert cannot perform target-size search/resizing and Resize cannot perform target-size search. No free processing or double charging occurs. Save/Share are never charged or gated.

See [docs/PHASE3_DESIGN.md](docs/PHASE3_DESIGN.md) for policy, atomic journal/recovery, backward-compatible Compression counter migration, debug/release isolation and offline limitations.

## Local evidence

Latest completed local run: 117 JVM tests (69 retained + 48 new), zero failures/errors/skips. Android lint: zero errors, five UseKtx warnings. Debug and instrumentation APK assembly pass. Release Kotlin compilation and compiled-provider/merged-manifest security checks pass. The final three-counter local rerun passed. The later unused-label cleanup also passed local lint, debug/instrumentation APK assembly and release Kotlin compilation.
Trusted wrapper JAR SHA256: `2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046`; wrapper distribution SHA256 remains pinned.
Local instrumentation lacks hardware acceleration; real GitHub Actions emulators execute API23/26/28/36.

## CI investigation history (superseded policy)

- [37871443601](https://github.com/digitalfuturesolutions69/komprexo/actions/runs/37871443601): build PASS, 98 JVM tests, lint zero errors/17 warnings. Each API ran 116 instrumentation cases: 114 passed and two NEW quota fixture compositions failed by trying to exhaust an already exhausted journal. Correct enforcement rejected the extra credits. Independent scenario journals retain every assertion; JVM tests now execute those exact compositions.
- [37872262666](https://github.com/digitalfuturesolutions69/komprexo/actions/runs/37872262666): build PASS, 100 JVM tests; API36 116/116 PASS. Older APIs exposed dialog recreation synchronization and API28 screenshot contrast failures after an attempted emulator command-line display adjustment. The profile still reported a 1080×1920 physical framebuffer. The supported pre-emulator-launch hook now sets actual AVD dimensions/density; recreation tests additionally verify the edited model/rendered state BEFORE lifecycle teardown and verify the retained ViewModel afterward. Assertions are preserved and strengthened, not suppressed.
- These runs precede the new THREE-counter owner policy and cannot establish final acceptance.
- [37873769368](https://github.com/digitalfuturesolutions69/komprexo/actions/runs/37873769368), updated policy commit `f8ff3b66730014e06d818ac4d14b8baa050e118d`: build PASS; API23 and API36 PASS; API28 executed all 126 cases with three screenshot-helper failures. Screenshots establish API28 navigation icons RGB142 on RGB245, measured WCAG contrast 3.005:1. The old arbitrary `<130` pixel cutoff incorrectly rejected them. The helper now requires actual minimum 3:1 contrast and correct icon polarity for BOTH system bars, retaining background and pixel-count assertions. Its instrumentation APK compiles successfully. No production system-bar change or skipped test is needed. API26 was still running when this snapshot was written. The corrected run below supersedes these helper failures.

## Verified three-counter implementation

[GitHub Actions run 37874589467](https://github.com/digitalfuturesolutions69/komprexo/actions/runs/37874589467) completed SUCCESS for implementation commit `60eae16c898dc4002e8aeba76bcbd8847a2a12c3`:

| Check | Actual result |
|---|---|
| Trusted wrapper validation / Kotlin compilation | PASS |
| JVM tests | 117 passed; zero failures/ignored (69 previous + 48 new) |
| API23 instrumentation | 126 passed; zero failures |
| API26 instrumentation | 126 passed; zero failures |
| API28 instrumentation | 126 passed; zero failures |
| API36 instrumentation | 126 passed; zero failures |
| Instrumentation total | 504 executions (126 distinct cases: 88 previous + 38 new) |
| Android lint | Zero errors; 18 nonblocking warnings in this CI snapshot |
| Debug APK / instrumentation APK assembly | PASS |
| Release Kotlin / entitlement-isolation checks | PASS; no release APK created |

The earlier build-report artifact records 117 units, zero failures/ignored, and 18 lint warnings: 12 GradleDependency, five UseKtx and one newly unused compression-only quota label. That obsolete label was removed in both locales; local lint after removal reports five UseKtx warnings. Dependency-version checks add warnings in networked CI; the final handoff reports the final run's actual count. No lint warning or check was suppressed.

The 320 × 569 dp emulator profile is physically 720×1280 at 360 dpi, configured before emulator launch. All four APIs execute the previous compact/landscape/200% font/IME/dialog/navigation/export regressions plus Premium disabled-purchase, bilingual notice, quota indicators and light/dark contrast scenarios. Screenshot evidence is included in each API report artifact. Automatic accessibility checks cover semantics, accessible controls, minimum touch targets, nonoverlap and persistent actions; this is not a physical TalkBack or Samsung S9 retest.

Verified implementation artifact: `komprexo-debug-60eae16c898dc4002e8aeba76bcbd8847a2a12c3`, containing `app-debug.apk` (11,878,878 bytes), retained 14 days:
[APK artifact 11591498703](https://github.com/digitalfuturesolutions69/komprexo/actions/runs/37874589467/artifacts/11591498703).
Downloaded bytes SHA256: `01461a2aa65251293daa0c1b7485a42ee9ef3307934d39731c3e36544aac547f`.
`apksigner verify` PASS; `aapt` confirms Komprexo, `com.komprexo.app`, minSdk23 and targetSdk36.

This document records a completed implementation run rather than inventing the identifier of the later documentation/unused-resource commit. The final execution report supplies that exact commit, its complete CI result, APK artifact, and local/remote synchronization. Final handoff requires all five jobs to pass on that commit, not merely this earlier run.

## Acceptance and limitations

Updated three-counter implementation acceptance: PASS on the verified run above. All three independent five-image allowances, single/batch sharing, accepted-success charging, reservation/recovery/cancellation, persistence and date/reset behavior are covered. Premium daily allowances are unlimited and its twenty-image batch limit remains subordinate to native technical limits. Production entitlement remains Free. The disabled purchase screen is a foundation, not a real purchase.

- Physical Samsung S9, owner review of the new screen and manual TalkBack testing remain unperformed by this task.
- Offline quota bookkeeping cannot prevent data clearing/reinstall, root-level tampering or deliberate forward clock/timezone manipulation. Clock rollback never grants another reset; a large erroneous forward clock can delay future resets. Idle indicators can take up to 15 seconds to show midnight; processing checks immediately.
- Activity recreation is tested; after process termination selected URIs/results are not persisted as image history. Recovery retains charged successes and releases unfinished reservations. A durable accepted success can remain charged if the process terminates before its UI renders.
- Debug Premium is explicit, temporary and testing-only; release provider cannot grant it. Debug signing keys can differ between CI runs, requiring uninstall/reinstall and clearing local quotas.
- No new physical quality claim: existing bounded image engines, byte-target guarantees and original-file protections are retained and their regressions pass.
- No ads, Billing, real payments, backend, database, website, login, analytics, telemetry, internet permission, merge, publication or Phase4 implementation are added. Any future Billing/AdMob integration requires new owner approval.

## Files changed from accepted Phase 2.5 baseline

### Created

- `app/src/androidTest/java/com/komprexo/app/MonetizationLifecycleTest.kt`
- `app/src/androidTest/java/com/komprexo/app/MonetizationWorkflowTest.kt`
- `app/src/androidTest/java/com/komprexo/app/QuotaDeviceTest.kt`
- `app/src/debug/java/com/komprexo/app/access/EntitlementProviderFactory.kt`
- `app/src/debug/res/values-in/strings.xml`
- `app/src/debug/res/values/strings.xml`
- `app/src/main/java/com/komprexo/app/access/AccessServices.kt`
- `app/src/main/java/com/komprexo/app/access/DailyQuotaManager.kt`
- `app/src/main/java/com/komprexo/app/access/FeatureAccessPolicy.kt`
- `app/src/main/java/com/komprexo/app/access/PreferencesQuotaStore.kt`
- `app/src/main/java/com/komprexo/app/ui/PremiumScreen.kt`
- `app/src/release/java/com/komprexo/app/access/EntitlementProviderFactory.kt`
- `app/src/sharedTest/java/com/komprexo/app/access/QuotaChecks.kt`
- `app/src/test/java/com/komprexo/app/QuotaTest.kt`
- `docs/PHASE25_VALIDATION.md`
- `docs/PHASE3_DESIGN.md`
- `scripts/check-phase3-security.py`
- `scripts/configure-compact-emulator.py`

### Modified

- `.github/workflows/android.yml`
- `VALIDATION.md`
- `app/build.gradle`
- `app/src/androidTest/java/com/komprexo/app/AccessibilityTest.kt`
- `app/src/androidTest/java/com/komprexo/app/LaunchTest.kt`
- `app/src/androidTest/java/com/komprexo/app/Phase2WorkflowTest.kt`
- `app/src/androidTest/java/com/komprexo/app/Screenshots.kt`
- `app/src/androidTest/java/com/komprexo/app/UiStabilizationTest.kt`
- `app/src/androidTest/java/com/komprexo/app/WorkflowTest.kt`
- `app/src/main/java/com/komprexo/app/ui/CompressionViewModel.kt`
- `app/src/main/java/com/komprexo/app/ui/KomprexoApp.kt`
- `app/src/main/java/com/komprexo/app/ui/KomprexoScreen.kt`
- `app/src/main/java/com/komprexo/app/ui/Phase2Controls.kt`
- `app/src/main/java/com/komprexo/app/ui/Phase2Screen.kt`
- `app/src/main/java/com/komprexo/app/ui/Phase2ViewModel.kt`
- `app/src/main/res/values-in/strings.xml`
- `app/src/main/res/values/strings.xml`
- `README.md`

