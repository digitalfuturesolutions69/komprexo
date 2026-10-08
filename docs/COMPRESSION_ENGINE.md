# Phase 1.5 compression engine

## Scope and identity

Komprexo processes one selected image offline. Kotlin / Compose, coroutine IO and
Default dispatchers, lifecycle-aware StateFlow and a retained AndroidViewModel.
There are no network, storage or media permissions, backend, database, account,
ads, billing or telemetry. Application ID remains provisional `com.komprexo.app`.

The existing AGP 8.9.2 / Gradle Wrapper 8.11.1 / JDK 17 are retained. Compile and
target SDK are 36. Google's requirements, checked 2026-10-08, require API 36 for
new phone app submissions/updates from August 31, 2026:
https://support.google.com/googleplay/android-developer/answer/11926878
API 36 needs AGP 8.9.1 or newer; the existing plugin is sufficient:
https://developer.android.com/build/releases/about-agp
minSdk 23 is retained; the device CI matrix tests API 23 and 36. No publication
is authorized or performed.

## Flow and boundaries

- `CompressionModels`: typed requests, options, progress, success and failures.
- `BitmapCodec`: sniffing, structural/decode validation, bounds, sampling, EXIF.
- `AndroidCompressionEngine`: serialized bounded bitmap processing and size search.
- `ImageStorage`: capped URI import, SAF save, secure sharing, temporary cleanup.
- `CompressionViewModel`: lifecycle job/state management and background previews.
- `KomprexoScreen`: single-image Compose UI and system activity-result contracts.

PickVisualMedia uses Android Photo Picker where supported, system fallback where
available, then ACTION_OPEN_DOCUMENT on older devices. No broad storage access is
requested. Input URIs must use content://. Import copies at most 32 MiB to private
cache; file extensions/provider MIME are never trusted. JPEG, PNG and WebP magic,
decoder MIME, nonzero dimensions and terminal/container structure are checked.
JPEGs without an end marker and truncated containers are rejected. A sampled
preview decode verifies decodability. Unsupported formats get a typed error.
The selected original is never opened for writing.

## Deterministic resolution-first search

Targets are maximum byte counts. UI uses binary units: KB = 1024 bytes, MB =
1024 * 1024 bytes. Presets: 100/200/300/500 KB and 1/2 MB. Custom: integer
1–10240 KB. Engine accepts integer targets from 1 byte to 10 MiB; targets smaller
than encoding overhead can fail without producing an accepted image.

1. Reinspect input; normalize all eight EXIF transforms in bounded software ARGB.
   **Quality first** (UI default) removes the unconditional 2 MP / 2048-edge cap.
   Its budget is `min(16 MP, maxHeap/2/20, (availableHeap-32 MiB)/2/20)` pixels,
   clamped nonnegative. Twenty bytes/pixel reserve bitmap copies and encoder
   headroom. Powers-of-two sampling is used only when source exceeds this budget.
   Original resolution is preserved whenever the source fits it. This is not a
   guarantee that every camera image fits every device's memory.
   **Balanced** keeps the original 2 MP / 2048-edge bounds and is the model API
   default for backwards-compatible callers; the UI explicitly selects its mode.
2. Quality-first Auto evaluates JPEG and supported WebP for opaque images at each
   resolution. At each quality level JPEG is preferred if it fits, otherwise WebP
   is tried. Alpha Auto tries PNG losslessly first, then alpha-preserving WebP.
   Balanced Auto retains JPEG (opaque) / WebP (alpha). Explicit selections are
   honored. Only explicit JPEG composites alpha on white, with a visible warning.
   JPEG is the most broadly compatible photographic output; PNG supports lossless
   pixels/alpha and common editors; WebP is supported on Android 23–36 but some
   older external editors/viewers may need JPEG or PNG. Auto never removes alpha.
3. Quality-first tests 100,95,90,85,80,75,70. This bounded coarse search avoids
   dozens of high-resolution encodes for every dimension step. These are codec
   settings, not comparable perceptual scores across formats. Balanced searches
   every integer 100–35, retaining its highest-fitting-quality regression check.
   PNG is tried once at each resolution; its quality setting is ineffective.
4. Every trial uses the bounded disk stream, actual nonzero file length <= target,
   successful encoding and decoded output bounds. Overflow/partial files cannot
   be accepted. Native stream IO errors are captured and reported after encoding.
5. If no candidate fits, Quality-first reduces dimensions by 0.9; Balanced uses
   0.8. Each resize derives from normalized master pixels, never a previous lossy
   file. Never upscale. PNG size control uses dimensions, not a fake quality slider.
6. At 1x1 with no fit return UNREACHABLE_TARGET. Guarantees concern actual bytes
   and bounded allocations, not global perceptual optimality. Native codec costs
   and sizes vary by Android version. Both modes remain cancellable/serialized.

Output is freshly encoded, stripping source EXIF including GPS metadata. Small
sources may encode larger than their originals; reduction percentages are actual
and may be negative. Quality numbers are encoder settings, not a perceptual
score. Alpha WebP may use lossy RGB while preserving its alpha channel. PNG
preserves pixels at each chosen resolution; downsampling is still a visual change.
Animated WebP, if decoded by the device, exports the first decoded frame only;
wide-gamut/HDR/high-bit-depth inputs are normalized to software ARGB SDR pixels.

## Memory, concurrency and cancellation

Reject source >32 MiB, >128 million pixels, or >32768 pixels per dimension.
Balanced decoded master is <=2 million pixels (~8 MB ARGB), further reduced by
available memory (16 bytes/pixel, maximum-heap/4 and available-heap/2 limits).
Quality-first uses the adaptive 20-byte/pixel budget above, with a hard 16 MP
ceiling instead. Budgets below 65536 pixels fail before decode. Actual decoded
pixel count and allocationByteCount are checked before orientation processing.
Each preview <=400k pixels
(~1.6 MB). Orientation/white compositing and scaling temporarily need additional
bounded bitmaps. Only one engine bitmap job at a time is allowed by a process-wide
mutex. Master + normalized or scaled bitmap allocations are bounded; Android
codec-native buffers and application/framework heap are not a fixed total-RSS
promise. OOM is mapped to a friendly error rather than claiming success.

Candidate bytes live on disk, not unbounded ByteArrayOutputStreams. Each request
has a UUID result directory; requests do not delete each other's files. File
import/copy loops use 16 KiB buffers. Sampling math, actual bitmap allocation,
large synthetic input, byte limits and concurrency are automated checks.

Cancellation is checked before decode, between encodes/scales and on stream
writes. Native decoder/encoder calls and provider reads may finish before they
observe cancellation; there is no unsafe thread interruption. Failed/cancelled
jobs remove their own result directories; superseded selections/results are
removed without touching originals or externally saved copies.

## Save, share and privacy

Save uses ACTION_CREATE_DOCUMENT with the actual MIME and extension and writes a
copy through ContentResolver. A user-selected existing destination may be
replaced only through the system's own confirmation; the app never automatically
overwrites the selected source. Full storage, revoked access, cancellation and
IO errors are surfaced. A failed/interrupted provider write can leave a partial
document in the chosen destination; the app does not delete externally owned
files. A success message appears only after stream completion/close.

Share copies into cache/shared only, then grants read access through an
unexported FileProvider with content URI + ClipData. Provider paths expose only
shared/, never the full cache or originals. Shared copies are retained for
receivers and purged on a later app launch after 24 hours. Share-cache allocation
is capped at 128 MiB; a full cache reports storage full. Other orphaned temporary
files are pruned after 24 hours at launch. System cache eviction can remove a
pending shared copy; external saved copies are independent.

English and Indonesian strings cover all typed error cases. Configuration
changes retain work/results; target/format controls use rememberSaveable. A killed
process cannot resume a native encoding job: the user must reselect/recompress.
The engine has no network code; a user-selected system document provider may
itself fetch a remote document outside Komprexo's image processing.

## Tests and known coverage boundaries

Versioned, seeded, non-personal fixtures are in src/test/assets with SHA-256
manifest. Shared EngineChecks run against Robolectric native graphics and real
API 23/36 device encoders. Checks use actual decoded/encoded bytes. Compose
workflow tests exercise system activity-result boundaries with stubbed dialog
responses, not a mocked engine. Dialog UX on OEM devices, arbitrary codec inputs,
HDR fidelity and device-specific memory pressure require broader product QA.
Actual execution results belong in VALIDATION.md, not in this design document.

## Phase 1.5 UI and system bars

Responsive FlowRows replace fixed rows; custom input exists only when selected.
Quality-first and Auto are default/recommended; settings remain accessible through
Adjust settings after processing. Results summarize original/output sizes,
reduction, original normalized/output dimensions, actual format and byte status.
Before/After switches one bounded preview. Save/Share stay outside the scrollable
content in the Scaffold bottom bar. Controls wrap at large fonts and use at least
48 dp touch heights. Safe drawing insets, bottom navigation and IME insets keep
controls clear of system UI; Scaffold padding is consumed to avoid doubled insets.

System dark mode now drives both Material colors and AndroidX edge-to-edge
SystemBarStyle. Light backgrounds use dark status icons (API23+) and dark nav
icons (API26+). API23–25 navigation uses a dark scrim with supported light icons.
Dark backgrounds use light icons. Transparent status bars / modern gesture bars
are backed by the matching theme surface. API35–36 edge-to-edge is retained.

## Phase 1.6 compatibility

JPEG structure validation permits trailing payload after the main EOI while still
rejecting missing main EOI. Provider data is copied through bounded ContentResolver
streams, including unknown-length pipes and offset descriptors; optional MIME
metadata does not control decoding. Software BitmapFactory is used on API23–36.
HEIC/HEIF receives an explicit unsupported-format error rather than being assumed
corrupt. Explicit WebP uses a16383-pixel edge limit and preserves format/alpha;
API23–29 uses legacy WEBP, API30+ WEBP_LOSSY. Auto skips infeasible WebP dimensions
while considering original-resolution JPEG/PNG. Byte and adaptive memory limits
remain unchanged. Debug-only structured local diagnostics and S9 retest directions
are documented in VALIDATION.md; no physical Samsung verification is claimed.
