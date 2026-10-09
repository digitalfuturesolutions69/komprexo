# Komprexo FASE 4A.1 validation

Repository: digitalfuturesolutions69/komprexo. Branch: `work`. Accepted starting commit: `9bab33b2d8586c4338d22f6ed597f6655e6097ae`. Application ID remains `com.komprexo.app`.

## Scope and review

Editorial changes cover the five-language canonical legal policies, offline copies, website policies and draft Store listings, English legal/support documents, technical Data Safety audit and Premium screen disclosure. See [editorial review and owner gates](legal/EDITORIAL_REVIEW.md).

Privacy explains selected photos, on-device work, temporary files, provider-directed exports, local quotas/preferences, Google purchase handling, possible third-party diagnostics, retention/security limits, choices/rights and official contact. Terms and purchase policy preserve exact Free/Premium limits, successful-only charging, restoration, one-time pricing, 24-hour offline verification and lawful refund/revocation rules.

All documents remain complete **DRAFTS**. Identity, address, law, audience, effective date, vendor disclosures and support-mail practices remain **OWNER ACTION REQUIRED**. Preparation date is not an effective date. Qualified native-speaker/legal review is required for every language.

No Kotlin application logic, manifest, build configuration, SDK, runtime dependency inventory or CI workflow was changed. No quota, Billing or image-processing behavior changed. Existing tests are retained; new checks verify canonical/offline/website policy parity and publication gates. No deployment, release, merge, Console submission, real transaction or AdMob.

## Local results

| Check | Actual result |
|---|---|
| JVM unit tests | 171 passed; 0 failed, errors or skipped, parsed from 8 JUnit XML suites |
| Android lint | 0 errors, 12 warnings in local XML |
| Debug / release Kotlin | PASS |
| Dependency audit | PASS; unchanged release inventory of 99 artifacts |
| Debug and instrumentation APK assembly | PASS |
| Security / permission isolation | PASS; existing four audited merged permissions unchanged |
| Static page generation | PASS; 36 pages including English aliases |
| Python asset/localization/editorial checks | 10 passed (8 retained, 2 new) |
| Browser checks | 50 passed (45 retained, 5 new canonical policy parity checks) |
| Whitespace/diff check | PASS |
| Local emulator execution | NOT RUN: local KVM unavailable; API 23/26/28/36 runs remain mandatory in GitHub Actions |

Commands: Gradle `testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest compileReleaseKotlin auditRuntimeDependencies`; `python3 scripts/check-phase4-security.py`; `python3 scripts/build-static-site.py`; `python3 scripts/test-phase4-assets.py`; `CHROMIUM_PATH=/usr/bin/chromium npm test` in `website`.

The first local browser attempt failed before test execution because Playwright's default Chromium executable was absent. The existing configurable browser path was set to installed `/usr/bin/chromium`, then all 50 checks passed. No assertion, test or security check was disabled. Final generated content was checked again before committing.

Local logs are temporary environmental evidence in `/tmp/komprexo-phase4a1-*`; APKs and XML reports are in ignored `app/build/`. Generated static previews are in ignored `website/build/`; these were built locally, not deployed.

## Exact-commit CI delivery

The unchanged GitHub workflow validates the trusted wrapper, compiles Kotlin, runs JVM tests/lint/security/dependency checks, builds/uploads a debug APK, runs all 157 instrumentation scenarios on each API 23/26/28/36 and runs static/browser checks. Final commit, exact run URL, actual job/test results, artifact and local/remote HEAD equality are independently verified after push and recorded in the accompanying delivery report. Pending checks must not be interpreted as PASS from baseline evidence.

## Remaining limitations

No real Play payment/restore/refund or physical Samsung S9 retest. No manual TalkBack or native-speaker/legal certification. Possible vendor diagnostics and support-mail practices require owner confirmation. Policies and Store listings are drafts; website remains unpublished. Stop after FASE 4A.1.
