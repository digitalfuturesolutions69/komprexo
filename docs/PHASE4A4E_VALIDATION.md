# FASE4A.4E — technical/editorial validation

Baseline490f78304bb4f88033945f56b855a79872a8cd1b verified locally/remotely before edits;
branch work, clean at preflight. Remote reconfirmed and ordinary git fetch succeeds
in the current session. No reset/newer work overwrite. Reviewed12October2026 Asia/Jakarta.

## Changes and preserved behavior

- Explicit independent5/5/5successful-image quotas per local calendar day, shared single/
  batch accounting,2Free/20Premium limits, failed/unused-cancelled reservations uncharged,
  accepted results counted, primary workflow once, Save/Share unmetered and local storage.
- Five canonical Privacy/Terms/Premium drafts and generated offline/website/Store copy
  synchronized. Android summary clarifies local calendar day; quota algorithms untouched.
- Source-generated Store footer now uses the planned Rumahweb privacy URL instead of
  a stale GitHub URL. Real Play checkout/refund/restore/24h disclosures unchanged.
- Evidence vocabulary/flow/category matrix and prioritized website/Play gates refined.
  Rumahweb general customer policy reviewed12October; no account-specific hosted-log,
  HTTPS/retention/deletion guarantee inferred. Original SDK evidence/inventory preserved.
- Owner account identity verification completed; Komprexo requirements distinct from
  AndroidPrint Closed Testing. Business future proposal only; no second product/100limit.
- Known-secret-pattern/public-ZIP verifier and actual-release99-artifact hash validation
  added to existing CI without changing SDKs/permissions, jobs or test matrix.

## Actual local results

Legal/locales/Store generation, static build/review ZIP, routes/links/parity PASS.
Python **23PASS**,0failures; Browser **152PASS**,0failed/skipped(1.8minutes).
New tests cover five-language quota/charging parity, preserved billing/email/host/draft
facts, no Business claims, known-secret-pattern detection without exposing values,
ZIP checksum rejection and same-version SDK byte-drift refusal.
Existing19Python/142browser and all Android regression sources retained.
Trusted Gradle8.11.1 wrapper SHA256
`2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046` unchanged.
Known-pattern/public allowlist/CRC/manifest/checksum/source audit PASS; not a certification
that arbitrary secrets can never occur. Current source/security checks pass against
retained local build reports; fresh final-CI compilation/identity/security results required.

Local Android invocation did NOT run tests: previous temporary JAVA_HOME was absent,
so Gradle stopped with invalid directory. Current environment has Java21 but no configured
Android SDK/sdkmanager/adb and lacks previous temporary Gradle/init/JDK files. Old local
unit/lint/APK reports are not claimed as current execution. Final CI retains Java17,
Android36SDK, unit/lint/debug/release/security and API23/26/28/36 device tests; actual
completed exact-commit reports/counts are recorded with delivery. No skipped/disabled checks.

## Resolved local validation failures

Initial Store wording exceeded4000characters before writes; compact equivalent Store
quota copy fixed the limit while keeping full canonical legal rules. Final Store lengths:
en3869/id3981/es3907/pt-BR3716/hi3767. Initial new numeric-summary test missed0inRp0 due
word-boundary regex; parsing all digit groups fixed the test without weakening policy
values. All23checks pass. No production defect or behavior change was used to fix tests.

## Evidence / release gates

[Evidence review](google-play/PHASE4A4E_EVIDENCE_REVIEW.md) and
[prioritized current decision](legal/PHASE4A4E_PUBLICATION_READINESS.md).
SDK_PRIVACY_INVENTORY.json99records reviewed/unchanged; no version-specific actual
telemetry schema/transmission/vendor retention evidence created. CI checks actual
release bytes against preserved hashes, not runtime privacy behavior.
DRAFT/effective dates/publication guard unchanged. Review ZIP only,38staticfiles,
root index.html, no source/APK/AAB/audit secrets/logs/backend/CNAME. New actual ZIP hash
and exact final SHA/CI/download evidence are reported with delivery, not copied from4D.
No live hosting/HTTPS/mail delivery, real Play payments/packets, Console inspection,
professional translation/legal approval, deployment/DNS/publication claimed.
No SDK/Billing/quota/processing/application-ID/package behavior changes, Business,
AdMob/backend/database/login, other-project mutations, merge or release.
Stop after4A.4E; passing CI is technical acceptance only, public readiness remains BLOCKED.

## First exact-commit CI failure and verified test-only correction

Implementation89e416b8ee737b34d952dc5d68219691acf20b11, run38205872601:
5/6jobsPASS; API36completed158tests with1failure in doubleTapConsumesOneCredit,
0skips/0stalls/0locale timeouts. API23/26/28each158PASS; JVM171PASS; lint0errors/
20warnings; Python23/browser152PASS; all99release artifact/POM/classes hashes match.

Failure is CalledFromWrongThreadException, expectedmain/callingDefaultDispatcher-worker-2.
Downloaded stack runs through Compose test ApplyingContinuationInterceptor and
TestMonotonicFrameClock after a legitimate background StateFlow progress publication,
not a duplicate-credit assertion. Exact1.8.0 official sources confirm default
UnconfinedTestDispatcher and Android MainTestClockImpl uses runOnUiThread. The
MonetizationWorkflowTest rule now uses StandardTestDispatcher(effectContext) so
recomposition queues onto the UI-driven test clock. All methods/assertions/timeouts,
real fixtures/engines and UI remain; no catches, skips, weakening or production edits.

Source evidence fetched12October2026 from official Maven:
https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-test-junit4-android/1.8.0/ui-test-junit4-android-1.8.0-sources.jar
https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-test-android/1.8.0/ui-test-android-1.8.0-sources.jar
Relevant files:AndroidComposeTestRule.android.kt,ComposeUiTest.android.kt,
MainTestClockImpl.android.kt. This test-only scheduler fix uses an existing transitive
test dependency; no SDK dependency/version change. New final SHA reruns all six jobs;
actual delivery records its outcome, not an unexplained rerun of the failed commit.
