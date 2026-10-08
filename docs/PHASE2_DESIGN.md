# Phase 2 processing contracts

Komprexo remains native Android, offline and single-device. There is no Internet
permission, backend, database, account, remote image processing, advertising or
billing. Application ID com.komprexo.app and API23–36 support are retained.

## Modules and ownership

- Existing AndroidCompressionEngine is reused for single/batch size targets.
  CompressionOptions adds an optional resize specification, default Original;
  all accepted default search, byte checks and quality/memory behavior remain.
- BatchOrchestrator accepts at most20 items, rejects overlapping calls on its
  instance, processes sequentially and isolates individual failures. Its progress
  is (successful+failed)/total, not an estimate of codec progress. Current image
  is1-based. Cancellation propagates, marks unprocessed items cancelled and
  preserves completed results delivered through callbacks. Caller owns successes.
- ImageTransformEngine converts/resizes independently of compression targets.
  Typed TransformRequest/ProcessingResult/ImageOutput prevent presenting a
  conversion or resize as achieving an unselected file-size target. It reinspects
  actual signatures, normalizes EXIF orientation visually, encodes once, verifies
  dimensions/MIME and performs a real sampled output decode. JPEG/WebP quality95;
  PNG lossless encoding. A32MiB output safety cap is not a user compression target.
- Transform, compression and Phase2 thumbnail bitmap work share a process-wide
  allocation mutex. Pixel work runs on Dispatchers.Default; provider/cache/export
  IO runs on Dispatchers.IO. UI updates use immutable StateFlow snapshots.
- Phase2ViewModel accepts one job at a time. Selection is bounded to20 batch items
  or one converter/resize item,32MiB each and256MiB aggregate imported data.
  Excess selection is rejected visibly; failed imports remain numbered entries.
  Thumbnails are at most160x160 ARGB pixels each (about2MiB total for20, excluding
  object overhead). Full decoded bitmaps are never accumulated across selections.
  Phase2 imports validate pixels while generating their thumbnail, avoiding the
  separate validation-preview decode used by the accepted single-image flow.
- Imported source copies and processor-created output directories are app-owned.
  Remove, clear, replacement and ViewModel teardown delete only these files.
  Native bitmaps are recycled after each processing operation. Display thumbnails
  are released by dropping references, avoiding recycling while Compose draws them.
  Dispatch-handoff cancellation cleans unaccepted output/share directories too.
- Process death cannot resume processing. Saved-state interruption markers explain
  this; old app-owned cache is reclaimed by the accepted24-hour cleanup policy.

## Resize and formats

Original dimensions, integer1–100 percent, width-only, height-only, and custom
width/height are supported.25/50/75/100 percent controls are provided. Aspect ratio
is locked by default; with two locked dimensions, the result fits inside their
box. Explicit unlocking permits stretching; no upscaling is implemented. Fit
boxes never crop, stretch or upscale. All dimensions are1..32768; encoder and
adaptive pixel limits still apply. Exact converter/resize dimensions either
succeed or return a device-limit error if the safely decoded bitmap is too small.
Compression with resizing can reduce further to satisfy its actual byte target.

JPEG/JPG, PNG and supported WebP are the only formats. Actual bytes determine
source format; optional MIME metadata cannot override it. Converter selects an
explicit format; Resize Auto preserves the detected source format. Compression
Auto retains accepted alpha-safe/photo rules. Alpha-to-JPEG requires visible
confirmation in Phase2, composites white and records transparency removal.
Changing format/preset or adding/replacing sources resets this confirmation.
API23–29 uses legacy WebP; API30+ WEBP_LOSSY.16383 is the WebP edge limit.
HEIC/HEIF, animation, HDR/wide-gamut and metadata-preserving exports are not added.
Other apps/editors may have limited WebP support; JPEG/PNG are compatibility choices.

## Editable presets

These are engineering defaults, not certified requirements of any platform.
All values can be edited before processing. Document/Marketplace use Auto, which
prefers JPEG for opaque photographs and preserves alpha on other images.

| Preset | Maximum bytes | Format | Mode | Dimension policy |
|---|---:|---|---|---|
| Document upload |204800 (200KiB)|Auto|Quality-first|Original decoded resolution|
| Marketplace |512000 (500KiB)|Auto|Quality-first|Fit1600x1600|
| Website |512000 (500KiB)|WebP|Quality-first|Fit width1280, preserve ratio|
| Social media |1048576 (1MiB)|Auto|Quality-first|Fit1080x1080|
| Custom |204800 (200KiB)|Auto|Quality-first|Original decoded resolution|

Marketplace also offers1200/1600/2000 square boxes. Social offers1:1(1080x1080),
4:5(1080x1350) and16:9(1920x1080) bounding boxes. These do not force the source
aspect ratio or claim to meet upload rules; no content is cropped. Resize applies
only preset dimensions/format and explicitly has no size target; Document is
available in Compress/Batch. Tiny targets can lose text/detail or be unreachable;
users must inspect the output for their intended document/platform.

## Export and security

Individual copies use ACTION_CREATE_DOCUMENT. Multiple copies use a user-selected
ACTION_OPEN_DOCUMENT_TREE with DocumentsContract and an API23-compatible tree
helper. No broad storage permission or persistent tree grant is requested. Each
new document gets a random UUID filename and the actual output MIME/extension.
Sequential export reports each success/failure, keeps successful external files,
and attempts to delete only the current newly-created partial document on failure.
A provider that revokes permission can prevent that cleanup; partial documents can
then remain. Providers may rename requested files. Process death loses session
state; it does not delete completed exports. Originals are never overwritten.

One/multiple outputs use Android Sharesheet ACTION_SEND/ACTION_SEND_MULTIPLE,
read grants and ClipData for every URI. FileProvider exposes only app-owned share
copies, not source/result directories or raw private filesystem paths. UUID names
avoid collisions. Combined share cache remains bounded to128MiB, with the accepted
24-hour recipient-grant retention; a capacity error leaves originals/results intact.
No ZIP is needed: individual documents and multiple content URIs satisfy export.

Structured debug diagnostics remain local and sanitized. No paths, image bytes,
exception messages/stacktraces, personal EXIF or document content are logged. Test
providers exist only in the instrumentation APK; their Java implementations work
in a separate process without relying on the target APK's Kotlin runtime.

## UI and verification

Home provides Compress/Batch/Convert/Resize. The accepted Compress UI remains,
with an added Home action and optional preset/resize dialog. New tools show an
editable settings summary, numbered selection thumbnails and source sizes,
separate result groups, exact progress and persistent action bars. Settings dialogs
keep lists compact. English/Indonesian, system themes, edge-to-edge/safe/IME insets,
48dp targets and large font handling remain. Tests exercise real codecs and byte
copies; gallery/document doubles do not certify OEM or real SAF provider behavior.
