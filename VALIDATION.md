# Komprexo — FASE 2.5 validation

Repository: digitalfuturesolutions69/komprexo. Branch: work.
Starting local and remote HEAD: 2432224b464cad0b47069dbe9089d23b25ff56ed.
Clean baseline inspected before changes; existing authorized Git transport verified.
Previous Phase 2 evidence is archived in docs/PHASE2_VALIDATION.md.

## UI changes

One reusable Compose settings dialog replaces the compression Smart Presets and
Batch/Convert/Resize settings dialogs. Its maximum height is 90% of the usable
window after safe drawing, IME and margin insets; width is capped at 560 dp.
Settings and title scroll together. Done remains outside the scrolling viewport.
The column wraps short content instead of reserving an empty fixed-height area.
Keyboard resizing and focus clearing on Done are explicit. Scrolling/dismissal
keeps editable values; normal activity recreation keeps the open dialog and values.
Dialog system-bar icon contrast follows light/dark theme; API 23 retains a dark
navigation background for its light navigation icons.

Chip groups use both horizontal and vertical spacing, wrapping labels, minimum
48 dp targets and explicit selected checkmarks. Marketplace/social dimension
choices now show selection state. Numeric dimensions remain full-width fields.
A reusable Material 3 Surface app bar separates the wrapping page title from Home.
It consumes top/horizontal safe drawing insets. Redundant in-content navigation
is removed; Home has one title, Compress retains Komprexo and a Compress subtitle.
Image selection is full-width; all four workflows show selected counts.
Active progress is in the fixed action area. Existing Save/Share actions remain.
When Phase 2 results exist, rerun Start is beside settings in scrollable content;
only Save/Share remain pinned, preserving room on short, large-font screens.

A saveable screen-state holder preserves compression UI settings across Home
navigation; a dedicated saver retains compression resize settings on recreation.
The existing Phase 2 view model owns one tool's selection/results/settings. Changing
that tool now requires explicit confirmation before clearing work. Cancel keeps it;
returning to the same tool keeps it. Original external images are never deleted.
This is not a multi-tool session/history feature.

No compression/decoder/encoder/storage/batch/preset processing implementation was
changed. The application ID remains com.komprexo.app (minSdk 23 / targetSdk 36).
No new processing feature, dependency, backend, account, website, billing or ads.

## Verification

All 69 existing unit tests and 79 existing instrumentation scenarios are retained.
Nine new UI scenarios cover Resize scrolling/dismissal, compact batch dialog at
200% font scale, actual landscape converter/resize at 200%, compression choice
geometry and retained navigation state, normal-root dialog recreation, explicit
cross-tool discard/cancel, light/dark dialog bar contrast and actual on-screen IME.
Font scaling uses Compose's LocalDensity at 2.0; landscape uses actual Android
requested orientation. The IME case enables hardware-keyboard IME display on the
emulator and restores the previous setting afterward. Dialog choice bounds are
checked for non-overlap and full layout height; Done has a 48 dp target assertion.
Previous processing/UI regression coverage still runs on API 23/26/28/36.

Local JDK 17 / SDK 36 / Gradle 8.11.1; trusted Wrapper unchanged. Proxy and trust
configuration remain outside the repository. Local instrumentation cannot run
because /dev/kvm is unavailable; emulator results must come from actual CI.

Initial local verification passed all 69 unit tests (zero failures/errors/skips),
lint (zero errors, six warnings), and app/test APK assemblies. Follow-up verification
is pending. The initial source CI run 37862102496 passed debug but failed new UI
checks for clipping and dialog status-icon contrast; it is not claimed PASS.
The first clipping helper incorrectly included covered background/partially scrolled
text. It now checks fully visible text in the active scroll viewport. Controls use
content-height selectable surfaces with radio semantics, wrapping labels and padding.
Dialog status/navigation inset areas now have explicit contrasting backgrounds,
including transparent edge-to-edge bars on API 35+. The CI evidence-copy step now
runs on shell EXIT so failed tests retain screenshots; Gradle failures still fail CI.
No test assertions, lint failures or build/security checks are disabled.

## Limits and acceptance

Screenshots referenced by the owner are not attached to this message. Source
layout defects and automated screenshots are used for verification; an owner
physical-device review of these UI changes remains required. IME heights, keyboards
and OEM layouts vary. Very short windows require scrolling to reach settings;
confirmation remains outside the scroll area. Process-death restoration of image
sessions remains outside existing scope. Existing Phase 2 safety limits apply.

Stop after FASE 2.5. No merge, release or publication.
