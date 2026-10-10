# FASE 4A.4B — owner-approved application identity migration

10 October 2026, Asia/Jakarta. Repository digitalfuturesolutions69/komprexo, authorized branch work. Expected baseline `30afb40aafa08bf2e9d712029ed77bd205984d19` matched clean local HEAD and freshly fetched remote work before edits.

## Scope

Approved application ID: **com.digitalfuturesolutions.komprexo**. Replaced only Gradle applicationId and its approval comment; Kotlin namespace and source packages remain com.komprexo.app. MainActivity continues resolving through that namespace. Runtime FileProvider authorities already use applicationId/context.packageName and now resolve to com.digitalfuturesolutions.komprexo.files. AndroidX's app-scoped signature permission now has the approved ID prefix. Security checks verify the merged release package, provider and exact permission set. Added one instrumentation regression for packaged identity and private FileProvider/signature protection; all previous tests retained.

README/current Console setup/checklist identify the approved ID. FASE 4A.4A audit artifacts, official evidence, legal content, Data Safety findings and publication decision are preserved. Previous documents' baseline-specific ID references are historical evidence, not current Gradle configuration. This identity-only migration does not resolve vendor, runtime, Console, mailbox, legal or native-language gaps: **BLOCKED — SPECIFIC EVIDENCE REQUIRED** remains the publication decision.

No Billing behavior/product ID, quota policy, image engine, SDK/version, offline legal content, active workflow, minSdk/targetSdk or application name change. The only permission difference is AndroidX's existing signature permission prefix following the new identity; no new permission capability or SDK.

## Installation and Play implications

Android treats this as a different application from com.komprexo.app. It does not update the old installation or automatically transfer its private preferences, quota journals, temporary files, Keystore ownership cache or grants. Each package has separate app storage. No cross-app migration or quota algorithm change was implemented. Previous saved user/provider files remain outside app-private storage.

Google Play purchases/configuration are package-specific. The unchanged komprexo_premium_lifetime product string does not establish that old-package ownership transfers to a new package. Owner must separately inspect/configure actual product/package records and perform authorized Play purchase/restore/refund tests before release. No Console access, configuration or submission occurred.

## Verification

Local test/build results are recorded below after completion. Final delivery separately records exact final SHA, remote equality, six-job first-run CI result, downloaded report counts and verified packaged APK metadata. A planned or previous-baseline run is not claimed as final-commit evidence. Local KVM is unavailable; actual instrumentation execution relies on unchanged API 23/26/28/36 CI jobs.

Website remains static/unpublished. No Pages activation/deployment, effective date, domain, Play submission, release, merge, force push, AdMob or next phase.


First local attempt: MainActivityTest's existing identity assertion still expected the former ID and failed; corrected its explicit expected ID without removing any test. The concurrent Android/browser run also lost its Gradle daemon; cgroup reported an OOM kill. No application memory algorithm changed. After browser completion, rerun Android validation sequentially with local-only bounded Gradle workers/heap. This failure is retained, not called PASS or hidden by retrying CI.


Final local results: Gradle BUILD SUCCESSFUL in 1m 52s (100 tasks; 19 executed/81 up-to-date), 171 unit tests/0 failures/errors/skips, lint 0 errors/12 warnings, debug and instrumentation APK assembly, release Kotlin and runtime audit PASS. Local aapt confirms com.digitalfuturesolutions.komprexo and test package com.digitalfuturesolutions.komprexo.test, minSdk 23/targetSdk 36. Security/package/provider/permissions PASS; actual 99 runtime coordinates match preserved audit. Website build/preparation PASS, 17 Python checks and 140 browser tests PASS. Multiple Kotlin daemon-session warning remains non-blocking. No local emulator execution claimed.


First CI implementation commit 81b482cbda62c78ea156bbd465431db82ed065c3: [run 38023789993](https://github.com/digitalfuturesolutions69/komprexo/actions/runs/38023789993) failed API 28 with 2 of 158 tests failing: SettingsLocaleTest.legalNavigationReturnsWithoutResettingWork and englishCompactFont200. The new packaged-identity regression passed. The in-test font screenshot shows Settings after asynchronous Billing status expanded its content, leaving the requested contact control below the viewport. The navigation test mixed Compose click with immediate Espresso Back without awaiting document composition/BackHandler effects. Post-test watcher screenshots show launcher after ActivityScenario cleanup and are not evidence of an app crash.

Corrected test synchronization only: await actual non-loading/non-busy Billing state and focused activity before navigating/scrolling Settings; synchronize Compose after navigation, assert legal draft content rendered before Espresso Back, then synchronize after Back. All visibility/48dp/navigation/state-retention assertions and every test remain enabled. No fixed sleep, test retry, mocked production behavior or app-code change was introduced. A new commit runs all six jobs; the failed first implementation run remains reported.

After synchronization changes, local instrumentation APK compilation PASS: BUILD SUCCESSFUL in 17s, 48 tasks (4 executed/44 up-to-date). Previous local JVM/lint/app build results remain recorded; final exact-commit CI repeats the complete suite.
