# Phase 0 validation — 2026-10-08

## Actual results in the task workspace

- Android XML parsing: PASS, all 5 XML files well formed.
- Workflow YAML parsing: PASS, debug and device-test jobs present.
- Git whitespace check: PASS.
- `gradle --no-daemon testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`:
  BLOCKED before execution, `gradle: command not found`.
- `gradle --no-daemon connectedDebugAndroidTest`: BLOCKED before execution,
  `gradle: command not found`; Android SDK/emulator also unavailable.
- Gradle/SDK download attempts: FAILED, curl exit 7; inherited proxy at port
  8080 could not be reached. No network policy bypass was attempted.
- Remote Git access: FAILED, same proxy connection failure.
- GitHub CLI authentication check: FAILED, supplied GH_TOKEN reported invalid.

No Android test has passed or failed: none could execute. No APK has been built
and no GitHub Actions run has been verified. Workflow syntax checks do not prove
that a build succeeds. There is no domain unit test suite in Phase 0; the
available application test is the instrumentation launch/recreation smoke test.

## Expected CI output, not an existing artifact

The debug job is configured to upload `app-debug.apk` under
`komprexo-debug-<commit SHA>`, retained for 14 days. The job separately builds the
test APK and publishes reports. A second job runs the instrumentation test on an
API 35 emulator. Successful Actions execution and the APK download details must
be checked after the task branch can be pushed.

## Remaining issues

Restore workspace proxy connectivity and authorized Git push access, push the
`work` task branch, and verify both Actions jobs and the artifact. Final owner
approval of `com.komprexo.app` remains pending. No merge, release or deployment
has been performed. Scope stops at Phase 0.

## Recovery execution

The original commit `2c3af9e24f1aa4c9d37c90bcd250814a4909f6d6` was found
locally on `work` with a clean working tree. The GitHub connector initially
reported no remote branches. With the supported execution network grant, Git
transport successfully pushed the original commit to `origin/work`. The earlier
proxy errors were from restricted command execution; no network policy bypass or
replacement credential was used. `gh auth status` still reports its supplied
credential invalid; Git transport and the GitHub connector work independently.

The original Actions run 37752782518 failed in both jobs at SDK setup because
`setup-android` attempted to install the retired `tools` package. Setup now
explicitly requests `platform-tools`. Existing SDK, unit test, lint, APK and
instrumentation checks remain enabled, with `contents: read` permissions.

A Gradle 8.11.1 wrapper was obtained from the official Gradle v8.11.1 tag via the
GitHub connector. Its JAR SHA-256 matches the published release checksum:
`2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046`.
The distribution ZIP checksum is pinned. Wrapper shell syntax and JAR checksum
checks PASS. The wrapper downloaded and ran Gradle 8.11.1 successfully with the
supported network grant and writable `/tmp` cache. Android SDK 35 / build tools
35.0.0 installed successfully after redirecting the unwritable user cache.

The existing activity was converted to Kotlin to satisfy explicit Kotlin
compilation. A Robolectric JVM launch smoke test was added; the existing device
launch/recreation test is retained. No Phase 1 feature or package rename occurred.

Initial build attempts failed before tests: the default user cache is read-only,
and the supplied Java 21 runtime lacks a compiler. Writable cache locations and
a checksum-verified JDK 17 are being used for subsequent verification. Final
results will be appended after CI/build execution. No APK PASS is claimed here.

Recovery commit `a86ee153dee220075998c701cf3d3e08c54bca13` passed the CI debug
job (run 37753146098). Kotlin compilation, unit tests, lint and debug APK assembly
succeeded, and the APK artifact exists. Local validation also completed with
BUILD SUCCESSFUL using JDK 17, writable caches, and inherited proxy/CA trust
forwarded to Robolectric through a local-only Gradle init script. The unit test
report records 1 test, 0 failures, 0 errors, 0 skipped. Lint reports 0 errors and
2 warnings: DataExtractionRules and Overdraw. Local APK signature verification
passes and aapt confirms Komprexo / com.komprexo.app / min 23 / target 35.

The CI device job failed before executing tests because AndroidJUnitRunner was
not included in the test APK (ClassNotFoundException). An explicit
`androidx.test:runner:1.6.2` dependency repairs the verified defect; device checks
remain enabled and will be rerun. Acceptance remains pending until that run.
