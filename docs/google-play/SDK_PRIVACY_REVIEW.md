> FASE 4A.4D hosting scope update: historical GitHub hosting evidence below applies
> only to the superseded Pages plan. Current planned host is Rumahweb shared hosting;
> provider-specific logging, recipients, retention/deletion and security are UNKNOWN.
> See [RUMAHWEB_STATIC_DEPLOYMENT.md](RUMAHWEB_STATIC_DEPLOYMENT.md). Original
> Android SDK evidence and unresolved Data Safety findings below remain preserved.

# Release SDK privacy inventory — FASE 4A.4A

10 October 2026 (Asia/Jakarta). **Static evidence, DRAFT; no vendor/runtime certification.** Fresh Gradle releaseRuntimeClasspath resolution contains 99 artifacts, identical coordinates to the previous license inventory. BOMs/platform constraints are visible in the resolved graph but are not executable artifacts. Desugar 2.1.5 is a separate core-library build dependency, not counted in 99 runtime artifacts. Test/Debug-only Robolectric/Espresso/Compose test dependencies are excluded from release inventory.

## Evidence method

1. Ran `:app:dependencies --configuration releaseRuntimeClasspath`, `:app:auditRuntimeDependencies`, `:app:processReleaseMainManifest`. Exported exact selected dependency edges with documentation-only `-I scripts/privacy-inventory.init.gradle :app:privacyResolvedGraph`. No dependency/manifest/app behavior changes.
2. `scripts/audit-privacy-inventory.py` reads actual resolved AAR/JARs, hashes bytes/classes.jar, extracts each artifact manifest permissions/components/initializer metadata, verifies published-POM hashes against existing license inventory, and records exact selected parent edges. It refuses coordinate/POM drift. No local paths, tokens or SDK API-key constants are copied into this document.
3. [Machine-readable 99-artifact inventory](SDK_PRIVACY_INVENTORY.json) contains every requested field: exact artifact/version, inclusion reason, initialization, permissions/components, data types, off-device/recipients/purposes/classification, encryption/deletion, evidence/confidence/unknowns. [Assessment profiles](SDK_ASSESSMENT_PROFILES.json) explain current app uses/capabilities, not an assertion that every library capability was executed. [Selected edges](RELEASE_DEPENDENCY_EDGES.json) include transitive and platform resolution. [POM/licenses](../legal/RUNTIME_DEPENDENCIES.json) are retained.
4. Reviewed MainActivity, BillingServices/Transport/Controller/Models/SealedOwnershipStore, ImageStorage, diagnostic source, quota/preferences and ViewModels. `javap -c -p` reviewed resolved Billing 9.1.0 and transport 3.1.8 classes. Bundled bytecode is implementation evidence, **not vendor privacy guidance or runtime observation**. No network capture or real purchase was performed.

## Verified implementation findings and remaining boundaries

- App launch constructs PlayBillingTransport for **all users**, including Free. onResume refreshes ownership/product queries; callbacks/reconnection also refresh. MainActivity.onCreate/onResume and BillingServices.initialize/refresh establish this. Optional checkout does not make all SDK activity opt-in.
- BillingClientImpl default constructor selects `zzdr`; its constructor creates `zzdt`. `zzdt(Context)` calls TransportRuntime.initialize and creates `PLAY_BILLING_LIBRARY` proto transport using CCTDestination.INSTANCE; `zza(zzkw)` calls Transport.send. Failures may suppress logger initialization/events. This closes the old uncertainty about a bundled logger/transport **code path**, not which packets were actually transmitted.
- CctTransportBackend.decorate contains metadata reads for API, model, hardware, device/product, OS build, manufacturer/fingerprint, timezone, locale/country, SIM operator and application build; request generation adds network type/subtype. These are potential actual-data categories supported by bytecode. A field's presence does not establish values, whether events leave on every device, or whether it is a stable user identifier. No IMEI/advertising-ID collection conclusion is made.
- CCT backend has HTTP POST/output-stream/gzip and a default HTTPS destination path. Default endpoint is assembled from SDK string parts; no API-key constants are reproduced. Server recipients/redirects/regions/purposes/retention/deletion and full in-transit encryption remain UNKNOWN without vendor/runtime evidence. Never equate default HTTPS with a blanket encryption assertion for all services.
- Transport runtime includes SDK-local SQLite payload/metadata/retry storage and jobs/alarm scheduling. EventStoreModule uses EventStoreConfig.DEFAULT with cleanup age 604800000 ms (7 days); SQLiteEventStore.cleanUp deletes qualifying old events when invoked. This is a **static default cleanup threshold**, not a guaranteed 7-day erase timer or Google server retention period. The queue is not given the app ownership lease's AES-GCM encryption. Device queue existence, execution and timing require runtime checks.
- GMS location/places/base/tasks are real transitive dependencies from Billing. App source requests no location permission or location API. Their names do not prove location collection; actual invoked SDK/service APIs and IP inference remain UNKNOWN.
- Firebase artifacts here are encoder helpers; there is no resolved Firebase Analytics, Crashlytics or Firebase app-init integration. Encoders alone do not perform networking; caller transport can send their output. This is graph/manifest evidence, not a zero-diagnostics claim.
- Merged manifest has four requested permissions: BILLING, INTERNET, ACCESS_NETWORK_STATE and app-scoped signature receiver permission. Billing declares ProxyBillingActivity/V2 and service visibility queries; GMS base declares GoogleApiActivity. Transport declares backend-discovery/job services and alarm receiver. SDK components are nonexported except the platform-protected profileinstaller diagnostics receiver documented in artifact/merged evidence. Review actual manifest attributes rather than claiming all components unexported.
- AndroidX Startup auto-initializes ProcessLifecycle, ProfileInstaller and EmojiCompat via metadata. Emoji default font loading may query a system/downloadable-font provider; provider-side network/diagnostics remain device-dependent UNKNOWN. ProfileInstaller deals with local compilation profiles, not an independently configured app analytics endpoint. Kotlin/coroutine/UI utility presence is not by itself a collection finding.

## Component index

Use the JSON record for per-component 13-field details, exact declared permissions/components, hashes, selected parent and confidence. HIGH applies to exact bytes/versions/declarations; current app uses are SOURCE-CODE SUPPORTED; actual off-device fields/vendor roles and deletion are UNKNOWN/REQUIRES RUNTIME VERIFICATION unless specifically evidenced.

| Exact release artifact | Assessment | Declared permissions | Declared components |
|---|---|---|---|
| androidx.activity:activity-compose:1.10.1 | utility | none | none |
| androidx.activity:activity-ktx:1.10.1 | utility | none | none |
| androidx.activity:activity:1.10.1 | utility | none | none |
| androidx.annotation:annotation-experimental:1.4.1 | utility | none | none |
| androidx.annotation:annotation-jvm:1.9.1 | utility | none | none |
| androidx.appcompat:appcompat-resources:1.8.0 | utility | none | none |
| androidx.appcompat:appcompat:1.8.0 | utility | none | none |
| androidx.arch.core:core-common:2.2.0 | utility | none | none |
| androidx.arch.core:core-runtime:2.2.0 | utility | none | none |
| androidx.autofill:autofill:1.0.0 | utility | none | none |
| androidx.collection:collection-jvm:1.5.0 | utility | none | none |
| androidx.collection:collection-ktx:1.5.0 | utility | none | none |
| androidx.compose.animation:animation-android:1.8.0 | utility | none | none |
| androidx.compose.animation:animation-core-android:1.8.0 | utility | none | none |
| androidx.compose.foundation:foundation-android:1.8.0 | utility | none | none |
| androidx.compose.foundation:foundation-layout-android:1.8.0 | utility | none | none |
| androidx.compose.material3:material3-android:1.3.2 | utility | none | none |
| androidx.compose.material:material-icons-core-android:1.7.8 | utility | none | none |
| androidx.compose.material:material-ripple-android:1.8.0 | utility | none | none |
| androidx.compose.runtime:runtime-android:1.8.0 | utility | none | none |
| androidx.compose.runtime:runtime-saveable-android:1.8.0 | utility | none | none |
| androidx.compose.ui:ui-android:1.8.0 | utility | none | none |
| androidx.compose.ui:ui-geometry-android:1.8.0 | utility | none | none |
| androidx.compose.ui:ui-graphics-android:1.8.0 | utility | none | none |
| androidx.compose.ui:ui-text-android:1.8.0 | utility | none | none |
| androidx.compose.ui:ui-tooling-preview-android:1.8.0 | utility | none | none |
| androidx.compose.ui:ui-unit-android:1.8.0 | utility | none | none |
| androidx.compose.ui:ui-util-android:1.8.0 | utility | none | none |
| androidx.concurrent:concurrent-futures:1.1.0 | utility | none | none |
| androidx.core:core-ktx:1.16.0 | utility | none | none |
| androidx.core:core-viewtree:1.0.0 | utility | none | none |
| androidx.core:core:1.16.0 | utility | ${applicationId}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION | none |
| androidx.cursoradapter:cursoradapter:1.0.0 | utility | none | none |
| androidx.customview:customview-poolingcontainer:1.0.0 | utility | none | none |
| androidx.customview:customview:1.0.0 | utility | none | none |
| androidx.datastore:datastore-android:1.1.7 | datastore | none | none |
| androidx.datastore:datastore-core-android:1.1.7 | datastore | none | none |
| androidx.datastore:datastore-core-okio-jvm:1.1.7 | datastore | none | none |
| androidx.datastore:datastore-preferences-android:1.1.7 | datastore | none | none |
| androidx.datastore:datastore-preferences-core-android:1.1.7 | datastore | none | none |
| androidx.datastore:datastore-preferences-external-protobuf:1.1.7 | datastore | none | none |
| androidx.datastore:datastore-preferences-proto:1.1.7 | datastore | none | none |
| androidx.drawerlayout:drawerlayout:1.0.0 | utility | none | none |
| androidx.emoji2:emoji2-views-helper:1.4.0 | emoji | none | none |
| androidx.emoji2:emoji2:1.4.0 | emoji | none | provider:androidx.startup.InitializationProvider |
| androidx.exifinterface:exifinterface:1.4.1 | exif | none | none |
| androidx.fragment:fragment:1.5.4 | utility | none | none |
| androidx.graphics:graphics-path:1.0.1 | utility | none | none |
| androidx.interpolator:interpolator:1.0.0 | utility | none | none |
| androidx.lifecycle:lifecycle-common-java8:2.9.0 | utility | none | none |
| androidx.lifecycle:lifecycle-common-jvm:2.9.0 | utility | none | none |
| androidx.lifecycle:lifecycle-livedata-core-ktx:2.9.0 | utility | none | none |
| androidx.lifecycle:lifecycle-livedata-core:2.9.0 | utility | none | none |
| androidx.lifecycle:lifecycle-livedata:2.9.0 | utility | none | none |
| androidx.lifecycle:lifecycle-process:2.9.0 | utility | none | provider:androidx.startup.InitializationProvider |
| androidx.lifecycle:lifecycle-runtime-android:2.9.0 | utility | none | none |
| androidx.lifecycle:lifecycle-runtime-compose-android:2.9.0 | utility | none | none |
| androidx.lifecycle:lifecycle-runtime-ktx-android:2.9.0 | utility | none | none |
| androidx.lifecycle:lifecycle-viewmodel-android:2.9.0 | utility | none | none |
| androidx.lifecycle:lifecycle-viewmodel-compose-android:2.9.0 | utility | none | none |
| androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.0 | utility | none | none |
| androidx.lifecycle:lifecycle-viewmodel-savedstate-android:2.9.0 | utility | none | none |
| androidx.lifecycle:lifecycle-viewmodel:2.9.0 | utility | none | none |
| androidx.loader:loader:1.0.0 | utility | none | none |
| androidx.profileinstaller:profileinstaller:1.4.0 | startup | none | provider:androidx.startup.InitializationProvider, receiver:androidx.profileinstaller.ProfileInstallReceiver |
| androidx.resourceinspection:resourceinspection-annotation:1.0.1 | utility | none | none |
| androidx.savedstate:savedstate-android:1.3.0 | utility | none | none |
| androidx.savedstate:savedstate-ktx:1.3.0 | utility | none | none |
| androidx.startup:startup-runtime:1.1.1 | startup | none | provider:androidx.startup.InitializationProvider |
| androidx.tracing:tracing:1.2.0 | utility | none | none |
| androidx.vectordrawable:vectordrawable-animated:1.1.0 | utility | none | none |
| androidx.vectordrawable:vectordrawable:1.1.0 | utility | none | none |
| androidx.versionedparcelable:versionedparcelable:1.1.1 | utility | none | none |
| androidx.viewpager:viewpager:1.0.0 | utility | none | none |
| com.android.billingclient:billing:9.1.0 | billing | com.android.vending.BILLING | activity:com.android.billingclient.api.ProxyBillingActivity, activity:com.android.billingclient.api.ProxyBillingActivityV2 |
| com.google.android.datatransport:transport-api:3.0.0 | transport | none | none |
| com.google.android.datatransport:transport-backend-cct:3.1.8 | transport | android.permission.ACCESS_NETWORK_STATE, android.permission.INTERNET | service:com.google.android.datatransport.runtime.backends.TransportBackendDiscovery |
| com.google.android.datatransport:transport-runtime:3.1.8 | transport | android.permission.ACCESS_NETWORK_STATE | service:com.google.android.datatransport.runtime.scheduling.jobscheduling.JobInfoSchedulerService, receiver:com.google.android.datatransport.runtime.scheduling.jobscheduling.AlarmManagerSchedulerBroadcastReceiver, service:com.google.android.datatransport.runtime.backends.TransportBackendDiscovery |
| com.google.android.gms:play-services-base:18.5.0 | gms | none | activity:com.google.android.gms.common.api.GoogleApiActivity |
| com.google.android.gms:play-services-basement:18.9.0 | gms | none | none |
| com.google.android.gms:play-services-location:19.0.0 | gms | none | none |
| com.google.android.gms:play-services-places-placereport:17.0.0 | gms | none | none |
| com.google.android.gms:play-services-tasks:18.2.0 | gms | none | none |
| com.google.firebase:firebase-encoders-json:18.0.0 | encoders | none | none |
| com.google.firebase:firebase-encoders-proto:16.0.0 | encoders | none | none |
| com.google.firebase:firebase-encoders:17.0.0 | encoders | none | none |
| com.google.guava:listenablefuture:1.0 | utility | none | none |
| com.squareup.okio:okio-jvm:3.4.0 | utility | none | none |
| javax.inject:javax.inject:1 | utility | none | none |
| org.jetbrains.kotlin:kotlin-android-extensions-runtime:1.9.22 | utility | none | none |
| org.jetbrains.kotlin:kotlin-parcelize-runtime:1.9.22 | utility | none | none |
| org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.8.0 | utility | none | none |
| org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.0 | utility | none | none |
| org.jetbrains.kotlin:kotlin-stdlib:2.1.20 | utility | none | none |
| org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2 | utility | none | none |
| org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.2 | utility | none | none |
| org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.7.3 | utility | none | none |
| org.jetbrains:annotations:23.0.0 | utility | none | none |
| org.jspecify:jspecify:1.0.0 | utility | none | none |

## Reproduce bytecode evidence

Obtain exact AARs from inventory Maven POM URLs (same artifact basename, `.aar`), verify artifact/classes.jar SHA256, extract classes.jar locally, then run JDK17 javap for BillingClientImpl, zzdr, zzdt; CctTransportBackend/CCTDestination; EventStoreConfig/EventStoreModule/SQLiteEventStore. Do not copy payloads, private paths, API keys or actual purchase tokens into reports. API metadata capabilities are findings from code, not packet capture.

No Play SDK Index/version-specific vendor privacy statement establishing exact Billing 9.1.0 diagnostic schema, server retention/deletion, optionality or sharing exemptions was established from the official pages reviewed. See [official evidence register](OFFICIAL_PRIVACY_EVIDENCE.md) and [required external verification](DATA_SAFETY_FINAL_REVIEW.md).
