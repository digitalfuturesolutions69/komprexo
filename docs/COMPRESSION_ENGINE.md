# Phase 1 compression engine

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

1. Reinspect input; sample by powers of two until width/height <= 2048 and pixels
   <= 2 million. Decode software ARGB_8888 and normalize all eight EXIF transforms.
2. Auto uses JPEG for opaque bitmaps, WebP for alpha-bearing bitmaps. PNG and WebP
   are explicit alternatives. Explicit JPEG composites alpha onto white; the UI
   warns before conversion and reports transparency removal after processing.
3. At the current resolution, try quality integers 100 down to 35. The first
   fitting candidate is the highest fitting quality at that resolution. An
   exhaustive descending search avoids assuming codec size is strictly monotonic.
   PNG is lossless and its quality parameter is ineffective, so try it once.
4. A bounded disk stream writes no more than the target, flags overflow and
   discards the remaining bytes of rejected candidates. Partial overflow files
   can never be accepted. Disk IO exceptions are captured at the Java stream
   boundary and rethrown after native encoding, so JNI cannot turn ENOSPC into
   an ambiguous encode failure. Measure actual file length and verify bounds.
5. If no quality fits, multiply the resolution factor by 0.8, derive dimensions
   from the normalized master aspect ratio (integer rounding, minimum 1x1), and
   retry. Never upscale and never decode an earlier lossy candidate.
6. At 1x1 with no fit, return UNREACHABLE_TARGET. JPEG/WebP retain quality >= 35;
   reducing resolution is preferred to very low quality. This defines the
   feasible search space, not a claim of a global perceptual optimum.

Output is freshly encoded, stripping source EXIF including GPS metadata. Small
sources may encode larger than their originals; reduction percentages are actual
and may be negative. Quality numbers are encoder settings, not a perceptual
score. Alpha WebP may use lossy RGB while preserving its alpha channel. PNG
preserves pixels at each chosen resolution; downsampling is still a visual change.
Animated WebP, if decoded by the device, exports the first decoded frame only;
wide-gamut/HDR/high-bit-depth inputs are normalized to software ARGB SDR pixels.

## Memory, concurrency and cancellation

Reject source >32 MiB, >128 million pixels, or >32768 pixels per dimension.
Decoded master is <=2 million pixels (~8 MB ARGB), further reduced when the
available heap budget is small (16 bytes/pixel headroom, at most one quarter of
maximum heap and half the current available heap). Budgets below 65536 pixels
return insufficient memory before decode. Each preview <=400k pixels
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
