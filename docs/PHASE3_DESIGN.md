# Phase 3 — Free and Premium foundation

Baseline: `f4f186363988867c868a6a36d4a30dae7332f5fc`, accepted Phase 2.5.
The owner's physical-device approval of Phase 2.5 is owner-reported, not a new physical test by this task.
Application ID remains `com.komprexo.app`; API23–36.

The owner's Phase 3 policy update supersedes the original request for unlimited Free single-image Resize/Convert. The current policy below applies to ALL single and batch operations.

## Authoritative access policy

| Feature | Free (Rp0) | Premium policy |
|---|---|---|
| Compression | Five accepted successes per local calendar day; single and batch share this counter | Unlimited daily |
| Resize | Five accepted successes per local calendar day; single and batch share this separate counter | Unlimited daily |
| Convert | Five accepted successes per local calendar day; single and batch share this separate counter | Unlimited daily |
| All batch operations | Two images per operation, within that feature's remaining credits | Twenty, within existing byte/codec/memory limits |
| Modes and custom targets | All existing options and original quality | Same algorithms |
| Smart Presets | Document, Marketplace, Custom | All, additionally Website and Social |

Manual preset parameters remain editable. Restrictions preserve selections, settings, and accepted output files. Save and Share are never entitlement-gated. Transform batches execute sequentially through the existing transform engine, not a new processing algorithm. Compression quality, target verification, alpha consent and native memory bounds remain unchanged.

`FeatureAccessPolicy` is authoritative. ViewModels validate before processing or clearing existing results. Picker additions exceeding Free's batch limit leave the current selection intact. Preset restrictions apply both when selecting and when starting work, so a debug entitlement downgrade cannot bypass them. Both ViewModels use the same process-wide manager and native allocation gate.

## Combined-transform charging policy

Charge one credit per accepted output to the operation's PRIMARY workflow:

- Compress (single or batch), including target-size search, resizing and output-format encoding, charges Compression exactly once. It does not charge Resize or Convert.
- Resize (single or batch), including its necessary output-format encoding, charges Resize exactly once. It does not charge Compression or Convert.
- Convert (single or batch), using original dimensions and the existing fixed-quality transform encoder, charges Convert exactly once. It does not charge Compression or Resize.

The processing workflow determines this category, not an editable user setting or picker label. Convert cannot invoke target-size search or resizing; Resize cannot invoke target-size search. A user cannot relabel a compression request to spend another counter. Ordinary file-size changes caused by transform encoding remain governed by the selected transform's own five-image allowance. No operation produces a free successful image, and internal scaling/encoding does not double charge. There is no arbitrary multi-tool pipeline in this phase.

Changing tools gives access to a separate, intentionally independent quota, never a reset of the exhausted counter. Saving/sharing an accepted output or previewing it does not process a new image or charge again.

## Entitlement verification boundary

Release source set has an immutable Free provider. There is no production purchase, entitlement flag, local unlock preference, SDK, or payment callback. The debug source set alone provides a clearly labeled temporary testing switch. Restarting the process restores Free. Its implementation and localized labels are absent from release sources/resources. CI compiles release Kotlin and checks compiled provider classes for test-unlock/Premium-grant symbols; it does not create or publish a production release.

Premium's one-time price is Rp49.000 (Rp49,000); localized Billing price requires a later separately authorized integration. The screen lists future ad-free behavior conditionally and shows the mandatory unavailable-purchase notice in Indonesian and English. Purchase remains disabled even in debug Premium testing. A future Billing provider must verify entitlement before replacing the release Free provider.

## Durable three-counter journal

Android Preferences DataStore contains only quota day, clock high-water mark, three aggregate committed counters, current reservation token/feature, reserved count and operation-local settled slot numbers. It stores no image bytes, image paths, source URI, EXIF, processing history, identity, or Premium unlock. The earlier Compression key `used` is retained; absent Resize/Convert counters migrate as zero without resetting prior Compression use. Missing old reservation feature defaults to Compression. Malformed counters or feature identifiers fail closed.

1. Atomically reserve requested slots in the selected feature, checking both batch size and remaining credits. A process-wide mutex rejects simultaneous actions across all screens and each ViewModel rejects double taps synchronously.
2. Process sequentially with the existing bounded engines.
3. After verified successful output (and preview for single compression), check cancellation, durably settle one slot in its feature, and publish/accept the output in a non-cancellable commit section.
4. Failures and unaccepted cancellations release remaining reservations without charging. Already committed successes retain their charge and batch output. Duplicate settlement is idempotent for Free and Premium.
5. Startup releases unfinished reservations but retains all three committed counters. A crash after durable success commit can leave a charge even if its screen was not rendered before termination; committed success is the accounting boundary. No persistent image history is introduced.
6. Reservations and dispatcher cancellation are coordinated so cancellation delivered after an edit cannot strand credits in the live process.
7. Storage corruption and invalid counters fail closed for ALL Free processing. Corrupt data is not replaced with fresh allowances. Completed Save/Share does not require the quota ledger. Failed cleanup unlocks the processing gate and retries journal recovery before future processing.

Activity recreation uses existing ViewModel ownership and saveable route/settings state; it does not reset DataStore or create a new reservation. The Home indicator displays all three allowances; each tool displays its own feature. Quota indicators observe settlement and refresh when visible/foreground, then every 15 seconds while started. Each operation checks the date regardless of indicator refresh. Premium indicators show unlimited daily use for all three features.

## Calendar and offline limitations

Local dates use the device's current timezone via `Clock.systemDefaultZone`, with java.time desugaring for API23. A later local calendar date atomically resets all three committed counts only when wall-clock time has not moved behind the persisted high-water mark. Returning to an earlier date/timezone does not grant another reset. A reservation crossing midnight remains protected in its own counter; success committed after midnight charges the new day. Duplicate slots remain idempotent across midnight within the active operation.

An offline local journal is not tamper-proof: clearing app data, reinstalling, root-level edits, or deliberately advancing the device clock/timezone can change allowances. Large forward clock errors followed by rollback may delay resets until clock/day high-water marks are reached. There is no server time or remote enforcement. The idle live indicator may take up to 15 seconds to show midnight; processing checks immediately.

Existing debug APK signing uses ephemeral CI debug keys; artifacts from different runs can require uninstall/reinstall, which clears all local quotas. No stable release signing or production release is created here.

## Regression strategy

All 69 previous JVM tests and 88 instrumentation cases are retained. Old UI feature regressions explicitly activate the permitted debug test entitlement to continue testing their existing 10/20-image and all-preset scenarios; their assertions remain intact. New Free UI workflows use real content URIs/native engines with isolated journals so test order cannot spend another test's allowances. Shared quota contracts run on JVM and Android API23/26/28/36. Each counter is tested for single/batch sharing, independence, persistence, concurrent attempts, partial success, failure/cancellation cleanup, restart, local date/timezone and rollback. Real DataStore persistence and corruption use actual disk I/O. Legacy counter migration is verified separately.

A physical Samsung S9 and new Phase3 owner review remain outstanding unless separately performed. No ads, Billing, payment processing, analytics, internet permission, backend, database, accounts, website, cloud storage, image upload or telemetry are added.
