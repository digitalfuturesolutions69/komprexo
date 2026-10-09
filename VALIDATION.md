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

Latest completed local run: 117 JVM tests (69 retained + 48 new), zero failures/errors/skips. Android lint: zero errors, five UseKtx warnings. Debug and instrumentation APK assembly pass. Release Kotlin compilation and compiled-provider/merged-manifest security checks pass. The final three-counter local rerun passed; pending tests are remote instrumentation only.
Trusted wrapper JAR SHA256: `2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046`; wrapper distribution SHA256 remains pinned.
Local instrumentation lacks hardware acceleration; real GitHub Actions emulators execute API23/26/28/36.

## CI investigation history (superseded policy)

- [37871443601](https://github.com/digitalfuturesolutions69/komprexo/actions/runs/37871443601): build PASS, 98 JVM tests, lint zero errors/17 warnings. Each API ran 116 instrumentation cases: 114 passed and two NEW quota fixture compositions failed by trying to exhaust an already exhausted journal. Correct enforcement rejected the extra credits. Independent scenario journals retain every assertion; JVM tests now execute those exact compositions.
- [37872262666](https://github.com/digitalfuturesolutions69/komprexo/actions/runs/37872262666): build PASS, 100 JVM tests; API36 116/116 PASS. Older APIs exposed dialog recreation synchronization and API28 screenshot contrast failures after an attempted emulator command-line display adjustment. The profile still reported a 1080×1920 physical framebuffer. The supported pre-emulator-launch hook now sets actual AVD dimensions/density; recreation tests additionally verify the edited model/rendered state BEFORE lifecycle teardown and verify the retained ViewModel afterward. Assertions are preserved and strengthened, not suppressed.
- These runs precede the new THREE-counter owner policy and cannot establish final acceptance. Updated policy CI verification remains pending.

## Required final verification

All 69 previous JVM and 88 previous instrumentation cases must pass, alongside updated three-counter tests. CI must validate the wrapper, compile Kotlin, run units/lint, build/upload the debug APK, check release-provider isolation and pass API23/26/28/36 tests on the FINAL branch commit. Confirm the actual APK artifact and local/remote HEAD equality.

No final Phase3 PASS is claimed at this snapshot. Physical Samsung S9 and new owner UI review have not been performed by this task. No ads, Billing, payments, backend, database, website, login, analytics, telemetry, internet permission, merge, publication or Phase4 implementation are added.
