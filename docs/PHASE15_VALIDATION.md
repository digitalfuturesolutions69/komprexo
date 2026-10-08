# Komprexo — FASE 1.5 validation

Date: 2026-10-08 UTC. Repository: digitalfuturesolutions69/komprexo.
Branch: work. Starting commit: f6206ba5cf8df2eb6301cc3820fbcb7607450cb5.
The existing implementation was extended; application ID remains com.komprexo.app.
Phase 0 and Phase 1 evidence is retained in docs/PHASE0_VALIDATION.md and
docs/PHASE1_VALIDATION.md. No main merge, release, publication or excluded feature.

## Root cause and implementation

The former decoder applied a blanket 2,000,000-pixel / 2048-edge limit before
searching output size. Power-of-two sampling could discard even more resolution:
a 1920x1600 source became 960x800 regardless of a generous target. Once these
pixels were lost, raising encoder quality could not restore fine detail.

- Quality-first (UI default) uses an adaptive heap budget, reserving 20 bytes per
  pixel for copies/encoding and at least 32 MiB free headroom. It retains original
  resolution when feasible, with a bounded 16 MP maximum. Balanced retains the
  original lower-memory behavior and all original engine regression scenarios.
- Quality-first uses seven quality levels (100–70 in steps of 5) and 10% dimension
  reductions. Balanced retains exhaustive integer quality 100–35 and 20% steps.
  Every resize derives from original decoded pixels. Native jobs remain serialized.
- Auto (recommended default) evaluates JPEG/WebP for opaque images. Alpha Auto
  tries lossless PNG then alpha-preserving WebP. Explicit selections remain honored;
  only explicit JPEG removes alpha, composites white and warns visibly.
- Actual byte length, overflow flag, encode success and output bounds are required
  before success. No target or memory safety check was disabled.
- Responsive presets/settings wrap; Custom field is conditional. Result summarizes
  original/output bytes, reduction, dimensions, format and achieved target.
  One Before/After preview replaces stacked images. Save/Share remain in an inset
  bottom bar, independent of scrolling. Adjust settings preserves all controls.
- System dark mode drives both Material colors and AndroidX SystemBarStyle.
  Light status icons are used on dark surfaces; dark icons on light surfaces.
  API23–25 navigation uses a dark scrim/light icons; API26+ uses theme-matched
  navigation icon appearance. Safe-drawing/navigation/IME insets remain applied.
  API35–36 edge-to-edge is retained. English and Indonesian labels are provided.

Algorithm, memory formula, format compatibility and limitations:
[COMPRESSION_ENGINE.md](docs/COMPRESSION_ENGINE.md).

## Actual before/after benchmark

Native Robolectric graphics, versioned seeded 1920x1600 detail.png. Balanced is
the previous algorithm's baseline. Output is decoded and bilinearly reconstructed
to source dimensions, with RGB PSNR sampled every two pixels; higher is better.
This deliberately detailed synthetic image is not the owner's camera image.
Neither encoder quality numbers nor these measurements guarantee global perceptual
optimality or the same output on every device.

| Maximum | Balanced bytes / dimensions / PSNR | Quality-first bytes / dimensions / PSNR |
|---|---|---|
| 2 MiB | 574022 / 960x800 / 15.54 dB | 1478038 / 1920x1600 / 35.96 dB |
| 500 KiB | 459587 / 960x800 / 15.54 dB | 500829 / 1920x1600 / 31.09 dB |
| 100 KiB | 102049 / 960x800 / 15.54 dB | 100978 / 1132x944 / 19.68 dB |

All six outputs meet their exact byte limits. At 2 MiB and 500 KiB the new mode
retains original resolution. At 100 KiB it selects WebP and still retains more
pixels/detail. JPEG is selected for the other five outputs. Full measurements:
[JSON](docs/QUALITY_COMPARISON.json); visible crop comparison:
[image](docs/QUALITY_COMPARISON.png). CI android-reports includes encoded outputs.
The benchmark test asserts higher fixture PSNR for Quality-first at each target.

Owner-provided physical-device observations are retained as observations only:
3.9 MB camera input; 2 MB→1.04 MB; 1 MB→624 KB; 500 KB→421 KB;
200 KB→184 KB; 100 KB→80 KB; custom 750 KB PNG→626.9 KB at614x816.
The original owner image/screenshots were not available for rerunning this case.

## Local execution

Existing JDK17 / Gradle8.11.1 / SDK36 setup, writable temporary caches and supported
proxy/network access; no environment credentials are stored in the application.

```sh
./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

- Unit/native-graphics regression: 32 tests, 0 failures/errors/skips. All previous
  27 tests are retained and passing. Four shared checks add both modes, actual
  bytes/formats, alpha preservation and heap budgets; one adds measured quality.
- Production, JVM and instrumentation Kotlin compilation pass.
- Android lint: 0 errors, 2 existing UseKtx warnings, 0 informational findings.
  Unused UI strings were removed and size-progress wording was shortened; no
  lint check was suppressed.
- Debug app and instrumentation APK assembly pass. Local app APK:
  app/build/outputs/apk/debug/app-debug.apk. SHA256:
  1cc2cfe9adc7d7e45738d4e8e3aa42a8326d50b0fd576e96f13df0a24bb8e4ee.
- Wrapper JAR checksum matches trusted Gradle8.11.1:
  2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046.
  Existing pinned distribution checksum and CI wrapper validation are retained.
- APK identity verified: Komprexo / com.komprexo.app, min23 / target36.
  Only AndroidX's app-specific signature receiver permission; no Internet/media
  or broad storage permission. APK signature verification passes (existing
  META-INF/JAR metadata warnings remain).
- Local instrumentation is not claimed: /dev/kvm is unavailable.
- Fixture hashes and git diff --check pass.

Initial new lossless-PNG resolution test incorrectly used a 2 MiB target for a
larger lossless source; it was corrected to 10 MiB to isolate memory/resolution
behavior. A build started during test-file edits had an incomplete source list;
the complete build was rerun. These failed attempts are not reported as passes.

## CI and delivery evidence

First implementation run:
https://github.com/digitalfuturesolutions69/komprexo/actions/runs/37767077806
for 57593e1d33edd55c518f56d13e34f2e70f22f81c.
Debug job passed. Both API23 and API36 executed 36 tests and reported BUILD
SUCCESSFUL, but their jobs then failed collecting screenshots after AGP had
uninstalled the app and removed its external-files directory. This is a real
workflow failure, not a passing CI run. Screenshot evidence now copies through
the instrumentation shell into /data/local/tmp before cleanup; the final workflow
must verify this artifact collection too. No test/lint/security check was disabled.
A result-mode Cancel action was also retained while Save/Share is busy.
Corrected run https://github.com/digitalfuturesolutions69/komprexo/actions/runs/37767892551
for 51e3bf01f3531e3f9a43d96a0b90cf0ddb001ea3 passed all three jobs and collected
screenshots. Downloaded device reports confirm 36 tests, zero failures/skips on
each API. Screenshot review then found poor icon contrast in the initial API23 dark-theme
capture despite correct flags. The test now waits for that
separate process and checks visible foreground pixels in both captured bars,
in addition to flags. Those stronger checks now pass on both APIs; the delayed
API23 dark capture confirms visible light icons. This was screenshot timing during
a transition, not persistent incorrect icon styling in the updated application.

Verified final implementation/test source: 03e6bd574110a13da3932cb0417d32e7be7e639d.
https://github.com/digitalfuturesolutions69/komprexo/actions/runs/37768885611
GitHub returned completed / success. All three jobs succeeded:

- debug: wrapper validation, Kotlin, 32 unit tests, lint, app/test APK build/upload.
- device-test (23): 36 tests, 0 failures, 0 skipped; screenshot collection succeeds.
- device-test (36): 36 tests, 0 failures, 0 skipped; screenshot collection succeeds.

Downloaded report HTML confirms 32 JVM tests / zero failures or ignored, and
36 device tests / zero failures or skipped on each API. Three added UI tests cover
conditional Custom, compact/200% font controls and both theme/bar contrasts.
The existing picker/save/share workflow additionally checks Before/After and
persistent Save/Share at 200% font. Minimum 48 dp touch heights are asserted.

System-bar screenshot pixel checks pass after a 350 ms SystemUI settling period
(test-only; no production delay). Representative solid glyph/background contrast,
calculated from the captured sRGB pixels, is 5.64:1 light status / 18.59:1 dark
status on both APIs; API23 navigation 14.06:1 in both themes; API36 navigation
5.71:1 light / 17.83:1 dark. These are sampled glyph colors, not a certification of
every antialiased pixel or OEM. [Bar crops](docs/SYSTEM_BAR_EVIDENCE.png) and
[color measurements](docs/SYSTEM_BAR_EVIDENCE.json) are retained. Full screenshots
are in each device report artifact, including the result with accessible actions.

Confirmed APK artifact:
komprexo-debug-03e6bd574110a13da3932cb0417d32e7be7e639d
https://github.com/digitalfuturesolutions69/komprexo/actions/runs/37768885611/artifacts/11546687458
Artifact ID11546687458; ZIP9236013 bytes; not expired at verification.
ZIP SHA256: 1f5b2b2226a5d88e48799cbd9a2850a42de2c78b516d652d2fe59c7efec3dff7.
APK upload path: app/build/outputs/apk/debug/app-debug.apk.
Retention14 days; expires2026-10-22 11:17:29 UTC. Report artifacts also exist.

Git transport and GitHub connector authentication are operational with supported
environment access; no token was exposed, stored or replaced. Local/remote work
were synchronized at the verified source commit. This delivery follow-up changes
only documentation/evidence. Its SHA/synchronization and its own CI run are
verified separately before the final execution report; this file does not invent
a self commit hash or not-yet-existing run ID.

CI retains Java17, SDK36, wrapper validation, unit tests, lint, app/test APK build,
APK upload and contents:read permissions. Device matrix remains API23/API36,
now explicitly configured to 720x1280 at density360 (320x569 dp). Three new UI
checks cover conditional Custom, 200% font scale/touch targets and light/dark
system-bar appearance. Existing end-to-end picker/encode/preview/save/share
scenario now also exercises Before/After and persistent actions at 200% font.
System dialogs are stubbed; codecs, storage, UI and output bytes are real.
Screenshots are collected only by instrumentation, not by the production app.

## Remaining limitations / acceptance

- Owner camera image and physical/OEM screenshots require owner retest. Automated
  bar appearance checks/emulator screenshots cannot certify every OEM contrast.
- High-resolution sources can still need sampling under heap pressure. The budget
  bounds pixel allocations, not exact total native RSS; OOM remains a typed error.
- Tiny targets necessarily lose detail or can be unreachable. Coarse quality and
  resolution search favors safety/runtime; it is not an exhaustive global optimum.
- JPEG/WebP are lossy; PNG resizing also loses detail. WebP compatibility varies
  with external editors. HDR/wide-gamut/animated output is not promised.
- Native codecs/provider IO may delay cooperative cancellation. Process death
  cannot resume encoding; cache eviction can remove unsaved files. A failed SAF
  save may leave a partial external document. Existing share cleanup rules remain.
- Application ID is preserved provisionally pending final owner approval.

Stop after FASE 1.5. Technical implementation acceptance: PASS for the verified source/tests. Owner
physical-device review and final approval remain pending. No subsequent phase or
publication is authorized.
