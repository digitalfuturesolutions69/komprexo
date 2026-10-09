# Komprexo FASE 4A execution report

Prepared 9 October 2026 Asia/Jakarta. Repository digitalfuturesolutions69/komprexo; work; accepted baseline 15266a239352cdf64ea37752a0f02fbf8d7de836. Local/remote matched baseline before edits; baseline existed, no previous Phase 4A implementation. Original evidence archived in PHASE3_VALIDATION.md.

## Actual local evidence

| Check | Result |
|---|---|
| Wrapper | Unchanged 8.11.1 SHA256 2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046 |
| Debug/release Kotlin | PASS |
| Dependency/license export | PASS 99 actual runtime artifacts |
| JVM | PASS 171/171, 0 failures/errors/skips; 117 baseline + 46 Billing + 8 locale |
| Android lint | PASS: 0 errors, 12 warnings (localeConfig API 33, legacy in/id alias, unused color, v26 monochrome detection, KTX). v33 monochrome exists |
| Debug APK | PASS: assembled locally (exact downloaded CI APK metadata below) app/build/outputs/apk/debug/app-debug.apk; signature PASS v1/v2, debug signing |
| Instrumentation APK | PASS assembled; local execution unavailable (no KVM) |
| Release security | PASS: 4 audited permissions/release isolation/secure FileProvider/99-library license graph |
| Python assets/locales/legal/site integrity | PASS 8/8; 160 keys × 5/placeholder parity/Hindi/SVG/PNG/512RGBA/safezone/36 routes/licenses/DRAFT/contact |
| Browser | PASS 45/45 final local run; initial 2 overflow failures corrected without dropping assertions |

First full run found 1 Compose locale-read lint error; fixed with LocalConfiguration and full rerun passed. Exact final-commit GitHub/emulator evidence is verified separately at delivery; implementation-run evidence appears below.

## Deliverables

Billing 9.1.0/lifetime INAPP komprexo_premium_lifetime; fresh base BUY option/Play price; pending/cancel/ack/retry/duplicates/restore/reconcile/revoke/authenticated 24-hour cache. Production fake unlock absent. Existing 3 daily quotas/processing/quality/safety/source preservation unchanged.

Geometric K/photo-frame identity #123B68/#52E0D1/#F5F8FC, outlined DejaVu wordmark; actual SVG/PNG/adaptive/monochrome/legacy/512 store/contact/mask sheets and licenses.

id/en/es/pt-BR/hi persistent System Default/per-app locale/API 23+AppCompat/API 33+OS. Settings offline legal/licenses/about/contact/version/restore. Static 36-page source, five languages, no deploy. Billing/security/brand/legal/DataSafety/licenses/fiveStore drafts/Console/publication docs.

## Pending owner/payment/publication acceptance

**Real Play purchase NOT TESTED**, Console product/base BUY/price/regions/signing/AAB/track/license testers unverified. Deterministic fake tests aren't payments.

**Legal DRAFT**: identity/address/law/audience/effective date/native-speaker/legal review pending; no invented company. Official support komprexo.support@gmail.com; Console contact not modified.

**Data Safety DRAFT assessment**: Billing transitively includes diagnostics transport/SDK-managed local SQLite/location libraries; no app location permission/API/photo upload/app DB. Vendor fields/retention/IDs/network behavior need confirmation before submission; no zero-Google-collection claim.

**Website NOT DEPLOYED**: proposed Pages URL not verified live; komprexo.click ownership unconfirmed/no CNAME/DNS. No live privacy URL to submit.

Client-only lease isn't tamper-proof/server verification; refund/revocation/clock/root/storage limitations documented. No physical Samsung S9/owner device, manual TalkBack, native-speaker or trademark clearance performed.

No AdMob/subscriptions/backend/app account/server DB/cloud processing/merge/release/publication. Stop after FASE 4A; new owner authorization required for any next phase.

## Verified implementation-run evidence

Implementation commit: 517fa0b1a0cf30d351c75c52ee2dfbb868093c95. GitHub authentication/authorized transport: fetch and push PASS; local and remote matched that implementation commit.

Run: https://github.com/digitalfuturesolutions69/komprexo/actions/runs/37897322072

| Job | Actual completed result at report preparation |
|---|---|
| debug | PASS; JVM171/171, release/debug compilation, dependency export, security, APK assembly and wrapper validation |
| static-preview | PASS; Python7/7 in implementation run, browser45/45; final source adds listing check and local Python8/8 passes |
| API23 | PASS157/157,0failed/0skipped |
| API26 | PASS157/157,0failed/0skipped |
| API28 | PASS157/157,0failed/0skipped |
| API36 | INCOMPLETE: 129/157 completed,0failed at last progress; job deadline cancelled the stalled run |

CI lint: **0 errors,25 warnings** (12 source/resource advisories plus13 available-dependency-update advisories). Local lint sees12 warnings; the difference reflects network metadata availability, not suppressed checks. Existing compatible foundation versions remain pinned; no broad upgrade undertaken to silence warnings.

Actual implementation APK artifact: komprexo-debug-517fa0b1a0cf30d351c75c52ee2dfbb868093c95, ZIP13,213,228bytes, uploaded/not expired: https://github.com/digitalfuturesolutions69/komprexo/actions/runs/37897322072/artifacts/11600554770 . Retention14days. Final delivery points to the artifact and CI run for the final commit, not this earlier implementation build.

API26 report HTML confirms157tests/0failures/0ignored; CI JVM HTML confirms171tests/0failures/0ignored. Actual downloaded screenshots reviewed: Hindi legal/settings text renders; Spanish landscape200%-font and Hindi compact200%-font controls are reachable; old system-bar pixel-contrast assertions/IME/workflow tests retained. Accessibility semantics/touch-target/layout tests are automated evidence, not a manual TalkBack certification.

The final report-bearing commit also includes a late-callback transport guard; after timeout/cancellation an old ProductDetails callback cannot replace fresh cached details. Its full local Gradle rerun passes171tests/lint/debug APK/instrumentation APK/release compilation/dependency export/security. Final CI is checked independently at delivery.

Baseline delta at delivery:126created,15modified,1removed files: native Billing/Settings/locale/icon/assets/tests, branding/content, static-source/tests, audits/legal/Play docs, build/workflow/security tools, README/validation. Removed only obsolete drawable launcher replaced by mipmap resources. Original engine/storage/quota-manager files remain unchanged. Exact file list is the final Git commit diff against the accepted baseline.

## CI investigation and corrections

The first API36 job completed129/157 tests with zero failures at its last progress update, then stalled and hit the30-minute deadline; no complete run or instrumentation report was claimed. The second candidate run37898905722 passed debug/static/API23/API26, but API28 had156passed/1failed (Portuguese compact200%-font support-control visibility); assertions remained unchanged.

New locale/orientation test helpers now await the resumed replacement Activity, not merely updated resource configuration, and restore portrait explicitly before each compact case. Failure capture occurs before cleanup. AppCompat updated to current stable1.8.0 (official release notes: https://developer.android.com/jetpack/androidx/releases/appcompat), minSdk23, with its documented view-tree configuration-dispatch fix; image engines unchanged. These changes are validated rather than asserted to prove the old stall's root cause.

Instrumentation uses a test-only listener exporting public test names/API/exception category only; no message, URI, image bytes, private paths or token. A15-minute test-process deadline keeps nonzero failure status and collects sanitized progress/screenshots before the outer30-minute CI timeout. All157 tests and all4API jobs remain mandatory; no assertions, tests, lint or security checks removed. Final counts and any remaining stall evidence are reported after the new run.

Current compatibility correction local validation: AppCompat1.8.0 full Gradle run PASS171JVM/0failures/0skips,0lint errors/12warnings, debug+instrumentation assembly and release compilation. Resolved graph still99runtime artifacts; both AppCompat artifacts changed1.7.1→1.8.0; actual POM hashes and bundled upstream notices regenerated. Security guard and8Python tests PASS. APK is rebuilt after notice regeneration so the shipped inventory matches actual dependencies.

A test-only120-second per-case watchdog records the public main/runner class+method frames (no locals/messages/source paths/thread names), then fails the stalled instrumentation process. This covers blocked@Before/main-thread calls that JUnit interruption cannot finish. It never reports an incomplete suite as passing; all157cases must complete for acceptance. The guard is absent from production/debug app code and only registered by CI in the test APK.


Further diagnostic run37903929524: JVM171 passed; static8Python/45browser passed; API26/API28 completed157 each with no failures; API23 completed157 with1 Portuguese compact-font visibility failure. API36 completed142 with7 failures before the test watchdog intentionally failed the incomplete run. Reports show locale timeouts in System Default cleanup, not purchase/processing; the sanitized runner stack waits in Compose/Espresso for a Choreographer frame while the main looper is idle. The emulator also reports invalid color buffers. This is evidence of test/native-locale synchronization and emulator frame instability, not proof of a production billing failure.

Correction under verification: compare effective locale language and explicitly requested region, assert the persisted override independently, and require Activity replacement only on a real language change. Native same-language/default regional changes may retain the Activity. Use the officially supported software renderer with Vulkan disabled (https://developer.android.com/studio/run/emulator-troubleshooting). The Portuguese compact Settings toolbar uses the concise valid label Ajustes; the full Configurações heading remains in scrollable content. Explicitly await UI idle before visibility assertions; no visibility/touch assertions removed. All157 cases and API23/26/28/36 remain mandatory.


Run37905203816: debug/static/API26/API28 PASS; API23 finished157 with2 failures at Indonesian locale setup (legacy in/id canonical tag mismatch); Portuguese compact200% font visibility passed. API36 finished138 before watchdog failure,4 failed/incomplete; the remaining timed-out locale checks again occur during cleanup, and the runner hangs awaiting a frame after locale recreation. Software-renderer change alone did not fix the stall, so it is not claimed as a proven solution.

The follow-up normalizes stored tags using LocaleListCompat in both test expectations and the app selected-language indicator; render tests now explicitly assert the selected checkmark for every language. System Default checks effective system language rather than assuming an unsupported resource-region match. Test cleanup closes the scenario before resetting the platform locale, preventing cleanup-only Activity recreation/retiring-root frame waits; the actual System Default UI and persisted-locale recreation tests remain unchanged and mandatory. Sanitized progress is streamed during the suite because API23 logcat ring rotation discarded early diagnostic lines. No production workaround or processing change introduced.


## Verified completion — implementation 10a15e19517d3c89fc43361743fb384525116a29

**PASS: all six jobs**, run https://github.com/digitalfuturesolutions69/komprexo/actions/runs/37906486380 . API23/26/28/36 each completed157 tests,0failures/0skips; no watchdog fired. Downloaded API36 HTML confirms157/0; streamed log confirms157 START/157 FINISH/0FAIL/0STALLED/0LOCALE_TIMEOUT. Downloaded JVM HTML confirms171/0failed/0ignored. CI lint0errors/24warnings (12 source/resource advisories,12 available-dependency updates). No checks were disabled. The previous failed runs above remain recorded for transparency.

Actual API36 screenshots reviewed: Portuguese compact320x569dp/200% font toolbar and support action visible; Spanish landscape200% font support action visible; Hindi legal text renders; status/navigation icons have contrast. Automated system-bar, touch-target, compact/landscape/IME/navigation/save/share/quotas regressions all passed. This is not physical-device or manual TalkBack certification.

Actual downloaded CI debug APK: app-debug.apk,14,157,342bytes; SHA2568e2a499b2aa37e26df97e5f2c378f8e06c75acc595bcdc902865048f88cd7de6. Packagecom.komprexo.app; version0.4.0(2); min23/target36/compile36. apksigner exits0, v1/v2 signatures verify; debug certificate only, not a production artifact. Artifact komprexo-debug-10a15e19517d3c89fc43361743fb384525116a29, ZIP13,225,730bytes, https://github.com/digitalfuturesolutions69/komprexo/actions/runs/37906486380/artifacts/11604752976 ; uploaded/not expired at validation; retention14days.

The report-only delivery commit runs the unchanged full workflow again. Its exact final HEAD, run and APK artifact are supplied in the accompanying delivery message after completion, avoiding a self-referential commit hash in this file. Technical implementation is validated; real-payment/legal/publication acceptance remains separate.

## Owner delivery fields — GENERAL

| Field | Result |
|---|---|
| REPOSITORY | digitalfuturesolutions69/komprexo, existing project root |
| BRANCH | work |
| STARTING COMMIT | 15266a239352cdf64ea37752a0f02fbf8d7de836, confirmed accepted baseline |
| FINAL COMMIT | Delivery report commit: exact SHA supplied in accompanying response; validated implementation SHA above |
| LOCAL = REMOTE / WORKING TREE | Authorized push verified equal; clean before report update; delivery checked again after push |
| FILES CREATED / MODIFIED | 126created,15modified,1obsolete launcher removed; exact baseline file list below |

## Owner delivery fields — GOOGLE PLAY BILLING

| Field | Result |
|---|---|
| BILLING LIBRARY VERSION | 9.1.0 current stable; compatible Java API from Kotlin, min23 |
| PRODUCT ID / TYPE | komprexo_premium_lifetime; one-time INAPP, non-consumable, no subscription |
| PURCHASE FLOW | Official Play UI, fresh ProductDetails, localized Play price; unavailable product disables Buy |
| PURCHASE OPTION HANDLING | Eligible base lifetime BUY; reject rental/preorder/discount/time-window/quantity limits; fresh offer token |
| PURCHASE ACKNOWLEDGMENT | Only PURCHASED, correct SKU/package/quantity/token; grant after confirmed acknowledgment; bounded retries/idempotency |
| RESTORE PURCHASES | Query current ownership; startup/foreground/reconnect/callback/explicit Restore |
| PENDING PURCHASES | Distinct localized status; no acknowledgment/unlock/consumption |
| REFUND/REVOCATION HANDLING | Successful current query without ownership revokes/clears cache; network failure alone does not imply refund |
| PREMIUM ENTITLEMENT | Unlimited daily3features/max20batch, existing technical limits and presets preserved |
| FREE ENTITLEMENT | 5successful/day separately for Compress/Resize/Convert, max2batch |
| QUOTA REGRESSION | Existing manager/engines unchanged; shared single/batch primary-feature counters, no internal double-charge, unused failures/cancellations refunded, Save/Share unmetered; tests PASS |
| CLIENT-ONLY SECURITY LIMITATIONS | No backend/independent cryptographic receipt verification/RTDN;24h authenticated local lease is not tamper-proof, delayed offline revocation possible |
| PLAY CONSOLE CONFIGURATION STATUS | OWNER ACTION REQUIRED; product/base option/IDR49,000/regions/signing/track/testers unverified |
| REAL GOOGLE PLAY PURCHASE TEST | NOT TESTED; deterministic fake tests are not transactions |

## Owner delivery fields — BRANDING

| Field | Result |
|---|---|
| LOGO CONCEPT | Original geometric K/photo-frame field; owner aesthetic/trademark review pending |
| PRIMARY COLORS | #123B68 deep blue, #52E0D1 teal, #F5F8FC off-white; dark UI accent#7CDCE8 |
| WORDMARK | Komprexo, outlined DejaVu Sans with bundled license |
| SVG SOURCE | artwork/komprexo-master.svg,icon.svg,logo-primary/horizontal/light/dark.svg |
| PNG EXPORTS | Actual primary/horizontal/light/dark variants and mask/contact sheets in artwork |
| ANDROID LAUNCHER ICON | Legacy square/round mdpi–xxxhdpi48–192px |
| ADAPTIVE ICON | API26 layers108dp, foreground inside66dp safe zone |
| MONOCHROME ICON | API33 same geometry/system tint |
| 512X512 STORE ICON | artwork/play-store-icon-512.png; RGBA32-bit, opaque square |
| ASSET VALIDATION | PASS CRC/decompression/dimensions/vector self-containment/references/safe zone |
| BRANDING PREVIEW | artwork/brand-contact-sheet.png and launcher-mask-preview.png, actual exports reviewed |

## Owner delivery fields — MULTILINGUAL

| Field | Result |
|---|---|
| SUPPORTED LANGUAGES | Indonesian(id),English(en),Spanish(es),Brazilian Portuguese(pt-BR),Hindi(hi) |
| LANGUAGE SETTINGS / SYSTEM DEFAULT | Native Settings picker, selected indicator; OS fallback without override |
| LOCALE PERSISTENCE | AppCompat API23–32 private locale persistence; native per-app LocaleManager33+; recreation tests PASS |
| TRANSLATION COVERAGE |160 main keys ×5, placeholder parity, debug labels/offline documents/static source/store drafts |
| BILLING LOCALIZATION |14statuses ×5, actual price from Play; no invented production price |
| ACCESSIBILITY | Compact320x569dp/landscape/200%font/48dp touch targets/semantics/system bars/IME regressions PASS4APIs; manual TalkBack/native-speaker/physical review NOT TESTED |

## Owner delivery fields — LEGAL AND CONTACT

| Field | Result |
|---|---|
| SUPPORT EMAIL / CONTACT SUPPORT | komprexo.support@gmail.com only; ACTION_SENDTO opens mail client, no automatic photos/tokens/attachments; no-client fallback localized |
| PRIVACY POLICY / TERMS OF USE | DRAFT,5languages offline+static, truthful local/cache/Google SDK/transfer/network limitations |
| PREMIUM PURCHASE POLICY | DRAFT, one-time proposed price/Play fulfillment/restore/refund guidance, no invented refund deadline |
| DATA SAFETY AUDIT | DRAFT actual99library graph/merged permissions; Google diagnostic transport/SDK SQLite/location library presence disclosed; vendor-data details unresolved |
| OPEN-SOURCE LICENSES | Actual POM hashes/upstream binary notices/Apache/BSD/DejaVu/Google terms, bundled offline5languages |
| OWNER LEGAL APPROVAL REQUIRED | Identity/address/effective date/jurisdiction/audience/children law/consumer rights/native-speaker review pending; no owner identity invented |

## Owner delivery fields — WEBSITE

| Field | Result |
|---|---|
| STATIC WEBSITE TECHNOLOGY | Python stdlib-generated HTML/CSS, no runtime JS/forms/tracking/backend/login/checkout |
| GITHUB PAGES SOURCE | website/build generated output + .nojekyll; source/generator committed, no deployment workflow/CNAME |
| STATIC BUILD TEST | PASS8asset checks/45browser cases |
| HOME / PRIVACY / TERMS / SUPPORT / PREMIUM / ABOUT PAGE | All6pages ×5languages,6English root aliases =36actual HTML routes |
| FIVE-LANGUAGE COVERAGE | id/en/es/pt-BR/hi; internal links/switcher/320px/landscape200%/dark/keyboard tests PASS |
| PUBLIC DEPLOYMENT STATUS | NOT DEPLOYED; proposed Pages URL not verified public/live |
| FUTURE DOMAIN PREPARATION | komprexo.click ownership unconfirmed; owner checklist only, no DNS/CNAME/certificate changes |

## Owner delivery fields — GOOGLE PLAY READINESS

| Field | Result |
|---|---|
| STORE LISTING |5DRAFT localized listings,30/80/4000limits validated; actual512icon, screenshot/feature-graphic specification; Console not changed |
| CONTENT RATING PREPARATION / TARGET AUDIENCE | Truthful owner questionnaire/checklist prepared; ages/rating/children declarations not invented |
| ADS DECLARATION | No AdMob or ads in this phase; owner must make truthful current Console entry |
| PRIVACY POLICY URL STATUS | No public approved URL; DRAFT/proposed Pages only |
| PLAY CONSOLE CHECKLIST | docs/google-play/PLAY_CONSOLE_CHECKLIST.md and PLAY_CONSOLE_SETUP.md |
| REMAINING OWNER ACTIONS | Review branding/translations/legal/DataSafety; configure product/base option/pricing/regions/signing/license testers; separately authorize real-payment testing and public policy hosting before submission |

## Owner delivery fields — QUALITY

| Field | Result |
|---|---|
| UNIT TESTS | PASS171/171,0failures/errors/skips;117baseline+46Billing+8locale |
| INSTRUMENTATION API23 / API26 / API28 / API36 | PASS157/157 each,0failures/skips;126baseline+13Billingcompatibility+18Settings/locale |
| ANDROID LINT | PASS0blocking errors;24CI warnings/12local warnings, no suppression to obtain PASS |
| SECURITY CHECKS | PASS wrapper/release-source isolation/exact4permissions/backup off/secure FileProvider/no token logs/runtime99license match |
| WEBSITE TESTS | PASS45browser/8Python; no public deployment |
| LOCALIZATION TESTS | PASS8JVM locale mapping+18native UI/storage cases on4APIs+five-language integrity/browser checks |
| DEBUG APK / APK ARTIFACT URL | Actual downloaded app-debug.apk metadata and implementation artifact URL above; exact delivery-commit artifact supplied in response |
| GITHUB ACTIONS RUN URL | All6implementation jobs PASS at37906486380; report-only delivery run separately verified in response |
| KNOWN LIMITATIONS | Real purchases/Console settings/public URL unverified; legal/DataSafety/translation/brand review pending; client-only/root/clock/offline/storage limits; no manual TalkBack/physical Samsung S9/payment test |
| FASE4A TECHNICAL ACCEPTANCE | PASS implementation automated checks; delivery commit verified separately; payment/legal/publication approval not implied |
| NEXT RECOMMENDED PHASE | Owner review and separately authorized Console/license-tester/payment/policy-publication work; no FASE4B/AdMob started |

## Exact files changed from accepted Phase3 baseline

```text
M	.github/workflows/android.yml
M	.gitignore
M	README.md
M	VALIDATION.md
M	app/build.gradle
A	app/src/androidTest/java/com/komprexo/app/BillingCompatibilityTest.kt
A	app/src/androidTest/java/com/komprexo/app/SafeTestProgressListener.kt
A	app/src/androidTest/java/com/komprexo/app/SettingsLocaleTest.kt
M	app/src/debug/java/com/komprexo/app/access/EntitlementProviderFactory.kt
A	app/src/debug/res/values-es/strings.xml
A	app/src/debug/res/values-hi/strings.xml
A	app/src/debug/res/values-pt-rBR/strings.xml
M	app/src/main/AndroidManifest.xml
A	app/src/main/assets/legal/en/about.txt
A	app/src/main/assets/legal/en/licenses.txt
A	app/src/main/assets/legal/en/premium.txt
A	app/src/main/assets/legal/en/privacy.txt
A	app/src/main/assets/legal/en/support.txt
A	app/src/main/assets/legal/en/terms.txt
A	app/src/main/assets/legal/es/about.txt
A	app/src/main/assets/legal/es/licenses.txt
A	app/src/main/assets/legal/es/premium.txt
A	app/src/main/assets/legal/es/privacy.txt
A	app/src/main/assets/legal/es/support.txt
A	app/src/main/assets/legal/es/terms.txt
A	app/src/main/assets/legal/hi/about.txt
A	app/src/main/assets/legal/hi/licenses.txt
A	app/src/main/assets/legal/hi/premium.txt
A	app/src/main/assets/legal/hi/privacy.txt
A	app/src/main/assets/legal/hi/support.txt
A	app/src/main/assets/legal/hi/terms.txt
A	app/src/main/assets/legal/id/about.txt
A	app/src/main/assets/legal/id/licenses.txt
A	app/src/main/assets/legal/id/premium.txt
A	app/src/main/assets/legal/id/privacy.txt
A	app/src/main/assets/legal/id/support.txt
A	app/src/main/assets/legal/id/terms.txt
A	app/src/main/assets/legal/pt-BR/about.txt
A	app/src/main/assets/legal/pt-BR/licenses.txt
A	app/src/main/assets/legal/pt-BR/premium.txt
A	app/src/main/assets/legal/pt-BR/privacy.txt
A	app/src/main/assets/legal/pt-BR/support.txt
A	app/src/main/assets/legal/pt-BR/terms.txt
M	app/src/main/java/com/komprexo/app/MainActivity.kt
A	app/src/main/java/com/komprexo/app/billing/BillingController.kt
A	app/src/main/java/com/komprexo/app/billing/BillingModels.kt
A	app/src/main/java/com/komprexo/app/billing/BillingServices.kt
A	app/src/main/java/com/komprexo/app/billing/PlayBillingTransport.kt
A	app/src/main/java/com/komprexo/app/billing/SealedOwnershipStore.kt
M	app/src/main/java/com/komprexo/app/ui/KomprexoApp.kt
M	app/src/main/java/com/komprexo/app/ui/PremiumScreen.kt
A	app/src/main/java/com/komprexo/app/ui/SettingsScreen.kt
D	app/src/main/res/drawable/ic_launcher.xml
A	app/src/main/res/drawable/ic_launcher_foreground.xml
A	app/src/main/res/drawable/ic_launcher_monochrome.xml
A	app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
A	app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml
A	app/src/main/res/mipmap-anydpi-v33/ic_launcher.xml
A	app/src/main/res/mipmap-anydpi-v33/ic_launcher_round.xml
A	app/src/main/res/mipmap-hdpi/ic_launcher.png
A	app/src/main/res/mipmap-hdpi/ic_launcher_round.png
A	app/src/main/res/mipmap-mdpi/ic_launcher.png
A	app/src/main/res/mipmap-mdpi/ic_launcher_round.png
A	app/src/main/res/mipmap-xhdpi/ic_launcher.png
A	app/src/main/res/mipmap-xhdpi/ic_launcher_round.png
A	app/src/main/res/mipmap-xxhdpi/ic_launcher.png
A	app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png
A	app/src/main/res/mipmap-xxxhdpi/ic_launcher.png
A	app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png
A	app/src/main/res/values-es/strings.xml
A	app/src/main/res/values-hi/strings.xml
M	app/src/main/res/values-in/strings.xml
A	app/src/main/res/values-pt-rBR/strings.xml
A	app/src/main/res/values/brand_colors.xml
M	app/src/main/res/values/strings.xml
M	app/src/main/res/values/styles.xml
A	app/src/main/res/xml/locales_config.xml
M	app/src/release/java/com/komprexo/app/access/EntitlementProviderFactory.kt
A	app/src/sharedTest/java/com/komprexo/app/billing/BillingFixtures.kt
A	app/src/test/java/com/komprexo/app/BillingTest.kt
A	app/src/test/java/com/komprexo/app/LocaleMappingTest.kt
A	artwork/DEJAVU_LICENSE.txt
A	artwork/brand-contact-sheet.png
A	artwork/icon.png
A	artwork/icon.svg
A	artwork/komprexo-master.svg
A	artwork/launcher-mask-preview.png
A	artwork/logo-dark.png
A	artwork/logo-dark.svg
A	artwork/logo-horizontal.png
A	artwork/logo-horizontal.svg
A	artwork/logo-light.png
A	artwork/logo-light.svg
A	artwork/logo-primary.png
A	artwork/logo-primary.svg
A	artwork/play-store-icon-512.png
A	content/i18n/en.json
A	content/i18n/es.json
A	content/i18n/hi.json
A	content/i18n/id.json
A	content/i18n/pt-BR.json
A	content/legal/en.json
A	content/legal/es.json
A	content/legal/hi.json
A	content/legal/id.json
A	content/legal/pt-BR.json
A	content/store-listing.json
A	docs/PHASE3_VALIDATION.md
A	docs/PHASE4A_VALIDATION.md
A	docs/billing/PLAY_BILLING_DESIGN.md
A	docs/billing/PLAY_CONSOLE_SETUP.md
A	docs/branding/BRAND_GUIDELINES.md
A	docs/google-play/DATA_SAFETY_AUDIT.md
A	docs/google-play/PLAY_CONSOLE_CHECKLIST.md
A	docs/google-play/PLAY_CONSOLE_SETUP.md
A	docs/google-play/STATIC_POLICY_PUBLISHING.md
A	docs/google-play/STORE_LISTING.md
A	docs/legal/ABOUT.md
A	docs/legal/OPEN_SOURCE_LICENSES.md
A	docs/legal/PREMIUM_PURCHASE_POLICY.md
A	docs/legal/PRIVACY_POLICY.md
A	docs/legal/RUNTIME_DEPENDENCIES.json
A	docs/legal/TERMS_OF_USE.md
A	docs/legal/licenses/APACHE-2.0.txt
A	docs/legal/licenses/BINARY_NOTICES.txt
A	docs/legal/licenses/PROTOBUF-BSD-3.txt
A	docs/support/SUPPORT_GUIDE.md
A	scripts/build-static-site.py
M	scripts/check-phase3-security.py
A	scripts/check-phase4-security.py
A	scripts/generate-branding.py
A	scripts/generate-legal-assets.py
A	scripts/generate-license-notices.py
A	scripts/generate-locales.py
A	scripts/generate-store-listing.py
A	scripts/run-device-tests.sh
A	scripts/test-phase4-assets.py
A	website/package-lock.json
A	website/package.json
A	website/playwright.config.cjs
A	website/style.css
A	website/tests/static-site.spec.cjs
```


## Final-delivery verification follow-up

Report commit1e3aa17462a95e553ca5132850b5434a1cf07deb, run37907515307, passed debug/static/API23/API26/API28. API36 first attempt failed132/incomplete with5errors: actual screenshots show the emulator's Quickstep launcher ANR dialog over Komprexo, causing dimmed system-bar screenshots and lost window focus. A targeted same-commit rerun removed that dialog but reached141tests/0failed before another Compose frame-idling stall; watchdog correctly failed142/incomplete. Neither attempt is claimed as passed.

Dependency inspection found the test classpath still on Espresso3.6.1/Runner1.6.2/Core1.6.1/JUnit1.2.1. Official stable AndroidX Test release notes document Espresso3.7.0's TestLooperManagerCompat/new platform API synchronization; Android's MessageQueue guidance specifically recommends3.7+ for the new APIs introduced in Android16. Test-only dependencies are aligned to stable Espresso3.7.0/Runner1.7.0/Core1.7.0/JUnit1.3.0 (min21, compatible with app min23), retaining Compose/Billing/app runtime pins and every assertion. This is a verified missing compatibility path; subsequent CI determines whether it resolves the observed stall. References: https://developer.android.com/jetpack/androidx/releases/test and https://developer.android.com/about/versions/17/changes/messagequeue . No app-owned backend, runtime database, telemetry, subscriptions or ads added.

Test-only dependency correction local Gradle tasks PASS: unchanged unit inputs/results171passed; lint0errors/12warnings; debug/instrumentation APKs assembled, release Kotlin compiled, runtime audit99unchanged and release security guard PASS. New test dependencies resolved and compiled successfully. Full CI is required on the correction commit before delivery.
