#!/usr/bin/env bash
# Preserve failed/hung-test evidence before the enclosing 30-minute CI deadline.
# Only our sanitized test tag is exported. No unfiltered logcat or private paths.
set -uo pipefail
mkdir -p app/build/reports/androidTests/diagnostics
timeout --signal=TERM --kill-after=30s 15m ./gradlew --no-daemon connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.listener=com.komprexo.app.SafeTestProgressListener
komprexo_test_status=$?
timeout 15s adb logcat -d -v brief -s KomprexoTest:I '*:S' \
  > app/build/reports/androidTests/diagnostics/safe-test-progress.log || true
timeout 15s adb pull /data/local/tmp/komprexo-evidence app/build/reports/androidTests/screenshots || true
exit "$komprexo_test_status"
