# Phase 3 — Free and Premium foundation

Baseline: `f4f186363988867c868a6a36d4a30dae7332f5fc`, accepted Phase 2.5.
The owner's physical-device approval of Phase 2.5 is owner-reported, not a new physical test by this task.
Application ID remains `com.komprexo.app`; API 23–36.

## Authoritative access policy

| Feature | Free (Rp0) | Premium policy |
|---|---|---|
| Single and batch compression | Shared five successful images per local calendar day | Unlimited count |
| Compression, Resize, Convert batches | Two images per operation | Twenty, within existing byte/codec/memory limits |
| Single Resize and Convert | Unlimited; no compression credits | Unlimited |
| Modes and custom targets | All existing options and original quality | Same algorithms |
| Smart Presets | Document, Marketplace, Custom | All, additionally Website and Social |

Manual preset parameters remain editable. Restrictions preserve selections, settings, and accepted output files. Save and Share are never entitlement-gated. Transform batches execute sequentially through the existing transform engine, not a new processing algorithm. Compression quality, target verification, alpha consent and native memory bounds remain unchanged.

`FeatureAccessPolicy` is authoritative. ViewModels validate before processing or clearing existing results. Picker additions exceeding Free's batch limit leave the current selection intact. Preset restrictions apply both when selecting and when starting work, so a debug entitlement downgrade cannot bypass them. Both ViewModels use the same process-wide manager and native allocation gate.

## Entitlement verification boundary

Release source set has an immutable Free provider. There is no production purchase, entitlement flag, local unlock preference, SDK, or payment callback. The debug source set alone provides a clearly labeled, temporary testing switch. Restarting the process restores Free. Its implementation and localized labels are absent from release sources/resources. CI compiles release Kotlin and checks compiled provider classes for test-unlock/Premium-grant symbols; it does not create or publish a production release.

Premium's proposed price is Rp49.000, one time. The screen labels it provisional, lists future ad-free behavior conditionally, and shows the mandatory unavailable-purchase notice in Indonesian and English. Purchase remains disabled even in debug Premium test mode. A future separately authorized Billing provider must verify entitlement before replacing the release Free provider.

## Durable quota journal

Android Preferences DataStore contains only current quota day, clock high-water mark, aggregate committed count, reservation token, reserved count, and operation-local settled slot numbers. It stores no image bytes, image paths, source URI, EXIF, processing history, identity, or Premium unlock.

1. Reserve all requested compression slots atomically, checking both plan batch size and remaining credits. A process-wide mutex rejects simultaneous actions across screens and each ViewModel rejects double taps synchronously.
2. Process sequentially with the existing bounded engines.
3. After verified successful output (and preview for single compression), check cancellation, durably settle one slot, and publish/accept its output in a non-cancellable commit section.
4. Failures and unaccepted cancellations release remaining reservations without charging. Already committed successes retain their charge and batch output. Settlement of the same slot is idempotent.
5. Startup releases unfinished reservations but retains committed counts. A crash after durable success commit can leave a charge even if its screen was not rendered before termination; committed success is the accounting boundary. No persistent image history is introduced.
6. Reservations and dispatcher cancellation are coordinated so cancellation delivered after a DataStore edit cannot strand credits in the live process.
7. Storage corruption and invalid counters fail closed for compression; corrupt data is not replaced with a fresh allowance. Single Resize/Convert and completed Save/Share do not require the quota ledger. Failed cleanup unlocks the processing gate and retries journal recovery before future compression.

Activity recreation uses existing ViewModel ownership and saveable route/settings state; it does not reset DataStore or create a new reservation. Quota indicators observe settlements immediately and refresh local day when shown, then every 15 seconds. Each operation checks the date regardless of indicator refresh.

## Calendar and offline limitations

Local dates use the device's current timezone via `Clock.systemDefaultZone`, with java.time desugaring for API 23. A later local calendar date resets committed daily count only when wall-clock time has not moved behind the persisted high-water mark. Returning to an earlier date/timezone does not grant another reset. Reservations crossing midnight remain protected; a successful slot committed after midnight charges the new day. Duplicate slots remain idempotent across midnight within their active operation.

An offline local journal is not tamper-proof: clearing app data, reinstalling, root-level edits, or deliberately advancing the device clock/timezone can change allowances. Large forward clock errors followed by rollback may delay future resets until clock/day high-water marks are reached. There is no server time or remote enforcement. The live indicator may take up to 15 seconds to show midnight while no action occurs; processing checks immediately.

Existing debug APK signing uses ephemeral CI debug keys; artifacts from different runs can require uninstall/reinstall, which clears local quota. No stable release signing or production release is created here.

## Regression strategy

All 69 previous JVM tests and 88 instrumentation cases are retained. Old UI feature regressions explicitly activate the permitted debug test entitlement to continue testing their existing 10/20-image and all-preset scenarios; their assertions remain intact. New Free UI workflows exercise real content URIs/native engines with isolated journals, preventing test order from spending another test's allowance. Shared quota contract tests run on JVM and Android API 23/26/28/36; real DataStore persistence and corruption are tested on disk. Physical Samsung S9 and new Phase 3 owner review remain outstanding unless separately performed.

No ads, Billing, payment processing, analytics, internet permission, backend, database, accounts, website, cloud storage, image upload or telemetry are added.
