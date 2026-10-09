# Komprexo Phase 3 validation

Repository: `digitalfuturesolutions69/komprexo`; authorized branch: `work`.
Starting baseline: `f4f186363988867c868a6a36d4a30dae7332f5fc` (accepted Phase 2.5, including owner-reported physical approval).
Previous evidence is preserved in [docs/PHASE25_VALIDATION.md](docs/PHASE25_VALIDATION.md).

Scope: Free/Premium access policies, shared daily quota, DataStore journal, local limits and Premium UI only.
Application ID `com.komprexo.app`, minSdk23/targetSdk36. No ads, Billing or real purchase integration.
See [docs/PHASE3_DESIGN.md](docs/PHASE3_DESIGN.md) for policies, durable commit semantics, debug/release separation, and offline limitations.

## Execution evidence

Local validation: 100 unit tests (69 retained + 31 new), zero failures/errors/skips. Android lint: zero errors, five UseKtx warnings. Debug APK and instrumentation APK assembly pass. Release Kotlin compilation and compiled-provider isolation check pass. Trusted wrapper JAR matches SHA256 `2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046`; distribution SHA256 remains pinned.

Initial CI [37871443601](https://github.com/digitalfuturesolutions69/komprexo/actions/runs/37871443601): build/unit/lint/APK/security job PASS, 98 JVM tests, lint zero errors/17 warnings (12 dependency update advisories, five UseKtx). API23/26/28/36 each ran 116 cases with two failures in newly composed quota test fixtures: an already-exhausted journal was passed to a helper that first exhausts a fresh journal. The manager correctly rejected the extra attempts. Independent fixtures now retain all assertions; two JVM tests verify the same device-scenario compositions. No tests, lint, or checks are disabled. Screenshot review additionally found API23 landscape captures letterboxed by the emulator's physical framebuffer/`wm` override mismatch, including old Phase2.5 scenarios. The emulator now starts with the intended 720×1280 framebuffer and 360 dpi instead of relying only on runtime overrides. Final emulator verification remains pending.
Local instrumentation requires hardware acceleration unavailable in this workspace; real emulators in GitHub Actions cover API23/26/28/36.
A physical Samsung S9 and owner review of the new Phase 3 UI have not been performed by this task.

Final delivery must identify the exact implementation commit, real test counts, all CI jobs and APK artifact. Publication, merging and Phase4 remain unauthorized.
