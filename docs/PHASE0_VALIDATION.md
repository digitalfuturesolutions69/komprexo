# Komprexo — Phase 0 recovery and validation

Date: 2026-10-08 (Asia/Jakarta). Scope: Phase 0 only.

## Repository and recovery

- Repository: https://github.com/digitalfuturesolutions69/komprexo
- Existing project root: `/workspace/komprexo`; task branch: `work`.
- Original commit `2c3af9e24f1aa4c9d37c90bcd250814a4909f6d6` was found locally
  with a clean working tree. It was not recreated or inferred from missing data.
- Remote initially had no branches. Supported network permission enabled Git
  transport; the original branch was pushed, followed by ordinary fast-forward
  recovery commits. No force push, history overwrite, merge or release occurred.
- Verified application-source commit:
  `f1f247d80dd65d57f5300919b5cff969bcff53a1`.
- This report-only follow-up commit retains exactly that application and workflow
  source. Its hash is available with `git rev-parse HEAD`; synchronization is
  checked against `git ls-remote origin refs/heads/work` after pushing.

## Actual verification results

| Check | Result | Evidence |
| --- | --- | --- |
| Wrapper JAR validation | PASS | Official Gradle 8.11.1 SHA-256 matches; CI wrapper-validation passed in both jobs |
| Wrapper distribution | PASS | Gradle 8.11.1 downloaded with pinned ZIP checksum; wrapper ran successfully |
| Dependency resolution | PASS | Local debugRuntimeClasspath dependency task completed; CI resolved build/test dependencies |
| Kotlin compilation | PASS | compileDebugKotlin and compileDebugUnitTestKotlin succeeded locally and in CI |
| JVM unit test | PASS | 1 test, 0 failures, 0 errors, 0 skipped in local JUnit XML; CI testDebugUnitTest succeeded |
| Android lint | PASS with warnings | lintDebug completed locally and in CI; 0 errors, 2 warnings locally |
| Debug APK assembly | PASS | Local assembleDebug succeeded; CI debug job succeeded and APK artifact exists |
| Test APK assembly | PASS | assembleDebugAndroidTest succeeded; rebuilt test APK contains AndroidJUnitRunner |
| Device smoke test | PASS | API 35 CI emulator ran 1 test; connectedDebugAndroidTest and device job succeeded |
| APK signature / identity | PASS locally | apksigner verify exited 0; aapt confirms Komprexo, com.komprexo.app, min API 23, target API 35 |
| Static checks | PASS | 5 Android XML files parsed; workflow YAML parsed; wrapper shell syntax and Git whitespace checks passed |

Successful CI run: https://github.com/digitalfuturesolutions69/komprexo/actions/runs/37753693594

GitHub returned `status: completed`, `conclusion: success`, with head SHA
`f1f247d80dd65d57f5300919b5cff969bcff53a1`. Both `debug` and `device-test` jobs
succeeded. Evidence applies to this source commit, not a claim that a later
report-only commit has already completed its own workflow run.

## Confirmed APK artifact

- Name: `komprexo-debug-f1f247d80dd65d57f5300919b5cff969bcff53a1`
- Artifact ID: `11539341305`; ZIP size: 828171 bytes; not expired at verification.
- Location: successful workflow run above, Artifacts section.
- Download: https://github.com/digitalfuturesolutions69/komprexo/actions/runs/37753693594/artifacts/11539341305
- Contents configured for upload: `app/build/outputs/apk/debug/app-debug.apk`.
- Retention: 14 days; expiration: 2026-10-22 16:01:48 Asia/Jakarta.
- GitHub artifact ZIP digest:
  `sha256:75fc8db491592fa61a23397efeb0794a2fbcd081951dcde5f8fdff0eac483b36`.
- Local APK: `/workspace/komprexo/app/build/outputs/apk/debug/app-debug.apk`,
  836427 bytes; SHA-256:
  `a61cfc589e7388613a650f64bf21aa85d557c9622fabab53df7454355392b7ca`.
  Local and CI debug signing keys differ; the APK hashes need not match.
- Unit/lint and device test report artifacts are also present on that run.

This is a debug-signed APK, not a release or Play publication.

## Verified defects repaired

1. Added the official Gradle wrapper and distribution checksum; CI uses
   `./gradlew` and wrapper validation with only `contents: read` permissions.
   Wrapper JAR/scripts originate from `gradle/gradle` tag `v8.11.1`. Official
   checksum source: https://github.com/gradle/gradle-distributions/releases/tag/v8.11.1
   JAR checksum: `2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046`.
2. Converted the existing activity to Kotlin, enabled Kotlin 2.1.20, and added a
   JVM launch smoke test to meet explicit compilation/unit-test requirements.
3. SDK setup now requests `platform-tools`; original run 37752782518 failed
   because the action attempted to install the retired `tools` package.
4. Added explicit `androidx.test:runner:1.6.2`; run 37753146098 had a successful
   debug job but failed instrumentation before tests due to missing
   AndroidJUnitRunner. Corrected run 37753693594 executed the test successfully.

No tests, lint, wrapper validation or security checks were disabled to pass CI.

## Environment and authentication

- Initial shell Git/curl failed to reach the proxy; Java socket creation was
  denied under default execution permissions. Supported additional network
  permission resolved access; inherited proxy and CA trust were preserved.
- `gh auth status` reports the supplied GH_TOKEN invalid. No credential was
  printed, replaced or persisted. GitHub connector reads and authorized Git
  transport pushes succeeded independently. This remains a CLI-specific issue.
- Supplied Java 21 runtime lacked javac. Downloaded JDK 17 from official Adoptium
  release assets and verified its published SHA-256 before use.
- Android SDK 35/build tools 35.0.0 installed in `/tmp/komprexo-sdk`.
- Default Java/Android/Gradle/Robolectric caches are unwritable here. Local
  commands used `/tmp` caches; a local-only Gradle init script forwarded writable
  user.home and inherited proxy/CA settings to the test JVM. Initial Robolectric
  failure was an unwritable lock file; corrected execution passed.
- No local `/dev/kvm` exists. Device execution was verified on GitHub Actions,
  rather than claimed locally. Local ephemeral toolchain files are not committed.

## Remaining issues and acceptance

Technical Phase 0 acceptance: PASS for the verified source commit, including a
real uploaded APK and successful unit, lint and device checks.

- Application ID remains provisional `com.komprexo.app`, pending final owner
  approval. Namespace/package names were not changed.
- Lint warnings: DataExtractionRules (future backup configuration) and Overdraw
  (root and window backgrounds overlap). Deprecated system window inset API
  compiler warnings remain; these APIs support the configured minimum API 23.
- apksigner notes that Gradle's META-INF/app-metadata.properties entry is not
  protected by the JAR signature; signature verification itself succeeds.
- GitHub logs note deprecated Node 20 action runtimes forced onto Node 24; actual
  action execution passed. These are maintenance warnings, not build blockers.
- Smoke tests establish foundation launch behavior, not full product QA or
  compatibility coverage across all Android versions.
- GitHub CLI authentication remains unresolved; working Git transport and
  connector access permit the requested delivery.

No website, backend, database, authentication, AdMob, Billing or Phase 1 feature
was introduced. No main merge, Google Play publication, release or deployment.
Stop after Phase 0 validation; owner approval governs subsequent actions.
