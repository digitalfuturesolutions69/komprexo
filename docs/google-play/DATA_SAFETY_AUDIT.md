# Data Safety assessment — DRAFT

9 October 2026, Asia/Jakarta. FASE4A.1 editorial review. **DRAFT; OWNER ACTION REQUIRED; no declaration submitted.** Local processing and Google SDK network behavior are distinct.

## Publication decision and evidence boundaries

**NOT READY FOR SUBMISSION.** On-device photo processing does not require uploading photos to Komprexo. This does not mean the app and included Google software collect no data. The release graph has99artifacts; source confirms no app photo-upload endpoint, but cannot establish every vendor diagnostic field or retention rule. No new SDK, permission, behavior or runtime dependency is authorized by this editorial review.

The consumer policies now explain selected images/metadata, local work files, user-directed exports, quotas/preferences, purchases, possible Google diagnostics, retention limits, security and user choices in plain language. Implementation names, cryptography, permission constants and dependency versions remain in this audit and Billing design. English, Indonesian, Spanish, Brazilian Portuguese and Hindi use the same policy sections and generation sources. Native-speaker and legal approval are still required.

A24-hour offline entitlement validity window is **not** a Google/SDK/support-mail deletion deadline. Temporary images older than24hours are checked during ImageStorage initialization, not erased by a guaranteed24-hour timer. Revoking URI permission does not erase an imported copy. Exported copies remain outside app deletion control.

## Evidence

App com.komprexo.app/API 23–36. Main manifest BILLING; merged release adds INTERNET/ACCESS_NETWORK_STATE and AndroidX app-scoped DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION. No location/broad media/storage/camera/microphone/contacts/advertising-ID/account permission. Backup off; FileProvider nonexported/temporary URI grants/cache-shared only. Picker accesses user-selected content URI, never crawls library.

Actual release runtime **99 artifacts**: [POM/license/hash inventory](../legal/RUNTIME_DEPENDENCIES.json), [notices](../legal/OPEN_SOURCE_LICENSES.md). New direct Billing 9.1.0/AppCompat 1.8.0; existing AndroidX Compose/Lifecycle/DataStore/Exif/Core/Kotlin/coroutines/desugar retained.

**SDK finding:** Billing transitively brings Google datatransport api/runtime/backend-cct and GMS base/basement/tasks/location19.0.0/places-placereport17.0.0. No app location permission/API. Presence alone doesn't establish location collection. Transport may queue operational diagnostics in **SDK-managed local SQLite** and send to Google's transport backend. No application photo/account/backend database introduced. Do not claim absence of all SQLite or SDK diagnostics. Mandatory SDK dependencies retained rather than concealed.

## Data matrix

| Data | Access/purpose/required status | Off-device collection/sharing/recipient | Storage/retention/encryption/deletion | Evidence/questions |
|---|---|---|---|---|
| Photos/pixels | Optional user-selected local compress/resize/convert/preview | No app photo upload. User Save may target third-party/cloud document provider; Share deliberately grants chosen receiver access/copy; recipient policy applies | Private input/output/thumbnail cache; >24h files cleaned on ImageStorage initialization plus clear/replace/failure cleanup; crash delays cleanup. Originals preserved. Sandbox, no extra image encryption. Export copies user/provider-controlled | ImageStorage/BitmapCodec/engines/FileProvider. Review current Play user-directed-transfer exceptions |
| MIME/dimensions/filenames/EXIF | Selected metadata; dimensions/rotation needed; MIME optional; EXIF local | No app metadata transmission; exported/shared bytes receiver-visible | Temporary original input may include EXIF until cleanup; no personal EXIF/private-path logs; output unnecessary metadata stripped | BitmapCodec/DiagnosticTrace. Source cache not claimed EXIF-free |
| Quota/journal | Required Free enforcement: counters/reservations/feature/day/clock high-watermark | No transmission/sharing | Private DataStore until reset/replacement/clear/uninstall; backups off, sandbox/no custom encryption | DataStoreQuotaStore/DailyQuotaManager. No image IDs/history |
| Language | Optional locale/System Default | No app transmission | AppCompat private preference API 23–32/OS per-app locale33+; reset/default/clear/uninstall | SettingsScreen/metadata service/localeConfig; quota/entitlement unchanged |
| Product/state/token | Required only purchase/restore | Official Play query/ack sends transaction identifiers to Google; SDK may send diagnostic fields | Raw token/JSON transient memory, never logs. AES-GCM Keystore lease stores digest/timestamps valid24h; remove next check/revoke/clear/uninstall | Billing classes. No server/Developer API verification; digest purchase-related, not necessarily anonymous |
| Payment/card/account/order | App doesn't collect cards/create account/order history; Google payment UI processes | Google/processor handle payer data; provider handling doesn't excuse SDK assessment | No app card/order-ID store; Google retention/deletion applies | Billing APIs. Confirm actual SDK classifications/provider exceptions |
| Diagnostics/errors | Local sanitized API/manufacturer/model/stage/MIME/decoder/encoder/allocation/error category | No app telemetry endpoint. Billing transport may transmit service diagnostics to Google | Android local logs; SDK serialized event queue may use SQLite. Vendor retention/encryption not established from source alone | diagnostics/ and SDK POM/AAR. Need actual Billing9.1 fields/destinations/optionality/deletion/security disclosure |
| Device/network IDs/IP | App API/model/manufacturer diagnostics; Billing uses services/network | Google sees IP and may process SDK/device IDs. No advertising-ID permission/app tracking ID | No app stable-ID DB; SDK/Play terms govern logs/retention | Manifest/source. Do not infer no IDs collected from absent direct calls |
| Location | No app permission/request/API | Precise collection not evidenced; IP-derived approximate location by Google unresolved | No app location store | Transitive location libs not proof either way |
| Voluntary support email | User chooses to send contact information, device/app details, text or redacted screenshots to seek help | ACTION_SENDTO opens an email client; no automatic attachment/transmission. User sends to komprexo.support@gmail.com; mailbox owner and email providers handle it | Owner must define access, retention, deletion, security and applicable request handling; not established | OWNER ACTION REQUIRED. Support is not an in-app upload service; do not imply the developer never receives personal information |
| Contacts/mic/messages/health/passwords | Not used | No app collection/sharing | None | Manifest/source; no accounts/forms |

## Safeguards, limits and owner questions

No app backend/photo upload/login/analytics SDK/AdMob/cloud processing/server DB. INTERNET/NETWORK_STATE serve authorized Play and SDK operations; image processing remains offline, payments don't. ACTION_SENDTO opens user-selected mail client; voluntary support message never auto-attaches photos/tokens. Static website has no scripts/forms/tracking; future Pages hosting can keep its own access logs.

Keystore encrypts lease, not all images. Revoked URI permission doesn't erase already copied cache. Clear app data deletes local copies/counters; exported copies cannot be retracted. Client-only data clearing can reset quotas.

Do **not** submit unqualified “no collection” while Billing/transport behavior unresolved. Obtain current vendor disclosure, inspect merged production graph/manifest and owner-authorized Play-test network behavior without sensitive logs, establish recipient roles/fields/retention/encryption/deletion and form's current collection/sharing/service-provider/user-directed exceptions. No packet capture/legal certification claimed.

Owner confirms identity/address/effective date/target ages/children law/consumer jurisdiction before policy publication. **Re-audit before separately authorized FASE 4B AdMob.**

References: [Data Safety](https://support.google.com/googleplay/android-developer/answer/10787469), [SDK data use](https://developer.android.com/privacy-and-security/declare-data-use), [Billing](https://developer.android.com/google/play/billing/release-notes).

## Unresolved items — OWNER ACTION REQUIRED

| Gap | Required evidence/action before publication |
|---|---|
| Controller/developer identity | Legal name, business address, governing law/dispute jurisdiction, audience/children obligations and effective date. Preparation date must not become effective date automatically. |
| Billing/transport data fields | Confirm current vendor disclosure for resolved Billing9.1.0 and its transport chain. Determine exact account/purchase/device/network/diagnostic identifiers and destinations. Library names alone do not prove a data type is collected. |
| Vendor retention/deletion/security | Confirm queue/network/server retention, encryption in transit, deletion/request handling, recipient roles and optionality. Do not infer these from app cache encryption or absence of permissions. |
| Location/network inference | No app location permission/API; transitive location libraries are not proof of collection or noncollection. Vendor IP-derived information remains unresolved. |
| Support mailbox | Confirm operator, access controls, retention, deletion and responses to lawful requests for voluntary email data. No response deadline or automatic erasure promise is made. |
| Play form classifications | Apply current SDK/service-provider/user-directed-transfer rules to actual behavior; do not prefill “no collection” or guess exemption eligibility. |
| Final legal and language approval | Review all five drafts with qualified native speakers and appropriate legal advisers; approve public text and host policy only after separate authorization. |

Official guidance reviewed9October2026: Google Play Data Safety requires assessment of third-party SDK data, not only developer endpoints; refund eligibility depends on purchase/payment/location and applicable law. References above and [refund guidance](https://support.google.com/googleplay/answer/2479637). This is compliance preparation, not a legal certification, vendor disclosure confirmation, payment test or Console submission.
