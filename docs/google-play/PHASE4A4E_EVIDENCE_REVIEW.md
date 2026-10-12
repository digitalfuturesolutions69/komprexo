# FASE 4A.4E — Evidence review, not a completed Data Safety declaration

Reviewed 12 October 2026 (Asia/Jakarta), baseline
490f78304bb4f88033945f56b855a79872a8cd1b. No production behavior changes.
Current app com.digitalfuturesolutions.komprexo; public developer Digital Future Solutions;
contact komprexo.support@gmail.com. Rumahweb is the planned host, not a tested live deployment.

## Evidence vocabulary

| Class | Meaning and limit |
| --- | --- |
| VERIFIED IMPLEMENTATION FACT | Current application source and scoped regression evidence support the described implementation. Not proof of opaque SDK execution, remote storage or all variants/markets. |
| SOURCE-SUPPORTED SDK CAPABILITY | Exact resolved artifact/bytecode supports a possible path or field. Does not prove execution, transmission, recipient role or classification. |
| VENDOR-CONFIRMED FACT | A primary vendor publication supports the statement within its stated scope. General policy is not version/account-specific assurance. |
| RUNTIME-VERIFIED FACT | An actual test/observation supports a narrowly scoped result. Automated device/UI tests are not real payments, packet evidence or hosted production logs. |
| UNKNOWN / OWNER ACTION REQUIRED | Evidence is insufficient; preserve HOLD until the named evidence is obtained. |

## Privacy statement traceability

| Topic | Current evidence | Class and remaining limit |
| --- | --- | --- |
| Selected images/permissions | ui/KomprexoScreen.kt and Phase2Screen.kt use Android photo-picker contracts; storage/ImageStorage.kt opens selected resolver URIs; manifest contains no broad photo/library permission | VERIFIED IMPLEMENTATION FACT. Remote picker providers may fetch input; provider behavior outside app unknown. |
| Images/metadata/originals | ImageStorage stages copies; compression decoder/metadata paths use format, dimensions/orientation and bounded decoding | VERIFIED IMPLEMENTATION FACT. Temporary source can contain original metadata; on-device processing is not a promise of metadata absence. |
| Temporary files | ImageStorage cache/images and cache/shared; cutoff older than24h checked in storage init; failure/replacement cleanup paths | VERIFIED IMPLEMENTATION FACT. No exact24h erase guarantee; initialization is a trigger, not a timed server deletion job. |
| Save/Share | ImageStorage and Phase2 export paths write user-directed copies/FileProvider URI grants | VERIFIED IMPLEMENTATION FACT. Receiving app/provider may upload; app cannot retract copies. Data Safety sharing exception remains conditional. |
| Local Free counts | FeatureAccessPolicy.DAILY_FREE=5; QuotaLedger separate COMPRESS/RESIZE/CONVERT used counts; LocalDate/Clock and conservative rollback checks; PreferencesQuotaStore DataStore | VERIFIED IMPLEMENTATION FACT. Three quotas per local calendar day, single/batch share; Free2/Premium20; local clearing can reset records. |
| Charging/acceptance | DailyQuotaManager durable settle/release; operation-specific workflow reservation, primary transformation charge; export code does not reserve credits | VERIFIED IMPLEMENTATION FACT. Failed/unused cancellation slots uncharged; accepted results remain counted; internal work not double charged. |
| Language/backup | Canonical catalogs/native locale preferences; manifest allowBackup/fullBackupContent false plus extraction rules | VERIFIED IMPLEMENTATION FACT. Clearing private data does not delete exported/provider records. |
| Checkout/ownership | PlayBillingTransport official client; BillingController requery/ack/restore; product komprexo_premium_lifetime INAPP/non-consumable | VERIFIED IMPLEMENTATION FACT. Real configured product, price/regions and transactions NOT runtime verified. Rp49,000 intended; actual checkout from Play. |
| Protected offline ownership | SealedOwnershipStore AES-GCM/Keystore AtomicFile digest/timestamps; BillingModels24h maximum; controller expiry/tamper/revoke paths | VERIFIED IMPLEMENTATION FACT. Digest not necessarily anonymous. No raw token/order/card persisted by app; local encryption does not prove all-transit encryption. |
| Startup/resume network | MainActivity onCreate initializes BillingServices; onResume refresh; BillingClient query paths include Free users | VERIFIED IMPLEMENTATION FACT. Optional checkout does not mean optional SDK initialization/diagnostics. Entire app is not promised offline. |
| App diagnostics | diagnostics/Diagnostics.kt emitter guarded by BuildConfig.DEBUG; sanitized API/model/stage/MIME/error/allocation fields | VERIFIED IMPLEMENTATION FACT. App logs are distinct from SDK/Play diagnostics; no fixed Android log-retention promise. |
| Billing/transitive telemetry | Historical exact9.1.0 default logger, TransportRuntime/CCT3.1.8 and queue bytecode, recorded99-artifact hashes | SOURCE-SUPPORTED SDK CAPABILITY. Actual payload, network events, fields, stable IDs, purposes/roles/retention remain UNKNOWN. |
| SDK queue cleanup | Recorded runtime bytecode default7-day threshold when cleanup invoked | SOURCE-SUPPORTED SDK CAPABILITY. No scheduled erase guarantee or Google server-retention inference. |
| Support | SettingsScreen ACTION_SENDTO official Gmail, no automatic image attachment or background send | VERIFIED IMPLEMENTATION FACT. Voluntary mail is external; real operator retention/deletion/access/authentication unresolved. |
| Rumahweb request flow | Static HTML/CSS/icon, local browser request tests; owner-selected Unlimited S/public_html/komprexo | VERIFIED IMPLEMENTATION FACT for static build; expected future HTTP/IP flow, not live provider observation. |
| Rumahweb practices | R1 primary provider customer policy reviewed below | VENDOR-CONFIRMED FACT for general customer-policy scope only; hosted visitor fields/logs/security/retention/recipients/account settings UNKNOWN. |
| User rights/deletion | Policies disclose contact, local clear/uninstall, export/provider limitations | VERIFIED IMPLEMENTATION FACT for available controls. Applicable rights, mailbox procedures and response handling require owner/legal review. |
| Actual tests | Local content/unit and exact-final-CI device/browser results reported with delivery | RUNTIME-VERIFIED FACT only for executed checks. No real purchases, SDK packet capture, mailbox delivery or public HTTPS observation inferred. |

Paths above resolve under app/src/main/java/com/komprexo/app unless stated otherwise.
Critical numeric/charging copy is now synchronized across five Privacy/Terms/Premium,
Android legal assets and Store drafts. UI quota indicators retain the same semantics;
only local-calendar-day wording is clarified. No quota algorithm or price behavior changed.

## Data Safety categories — current assessment and closure evidence

| Category/question | Supported scope | Outstanding evidence and responsible party | Status |
| --- | --- | --- | --- |
| Personal information | No app account/profile form; voluntary email and Google account/checkout separate | Google/vendor actual SDK fields/roles; owner mailbox handling; actual Console question scope | UNKNOWN / HOLD |
| Purchase history/financial information | App accesses ownership/token/state; no card/order-history store. Provider-direct card exception is narrow | Vendor payload/purposes/retention; real Play tests; qualified exception assessment, not a blanket Billing exemption | UNKNOWN / HOLD |
| User identifiers | Protected token digest local; raw token passed to Play acknowledgment | Vendor persistent IDs, linkability, recipient roles; authorized runtime evidence | UNKNOWN / HOLD |
| App activity | Local counts are not image history; Billing service events have supported paths | Version-specific event schema, triggers/optional controls and purposes | UNKNOWN / HOLD |
| App info/performance/diagnostics | Debug app emitter distinct from bundled SDK transport | Vendor diagnostics/timing schema + scoped runtime observations across Free/startup/purchases | UNKNOWN / HOLD |
| Crash-related data | No standalone Crashlytics/analytics integration in release inventory | SDK/Play error/crash fields and purposes; no absence inference from library names | UNKNOWN / HOLD |
| Device identifiers | Potential build/device/network fields; Build.FINGERPRINT is not proof of unique hardware ID | Stable-ID/IP/SDK-ID exact handling and classification | UNKNOWN / HOLD |
| Approximate/inferred location | No app location permission/client. Network/SIM/country/IP capability distinct from geolocation collection | Vendor inference/usage/schema and runtime destinations; do not answer No from permission absence | UNKNOWN / HOLD |
| Photos/videos | User-selected input processed locally; user-directed Save/Share to chosen destination | Verify any additional SDK access/transmission; qualified sharing exception/recipient review | VERIFIED scoped app-local flow; whole declaration HOLD |
| Collection versus sharing | On-device processing differs from off-device SDK traffic and app-to-app export | Actual fields/destinations, provider/onward roles and conditional exceptions | UNKNOWN / HOLD |
| Required versus optional | Purchase is optional; startup checks occur for Free too | All-user/region opt-in/out controls for each actual data type, vendor confirmation | UNKNOWN / HOLD |
| Encryption in transit | Local ownership encrypted at rest; static CCT HTTPS default supported | All relevant actual transfers/TLS/providers/redirects and vendor assurances; no blanket Yes | UNKNOWN / HOLD |
| Deletion/retention | Scoped local clear/cleanup supported; exports/provider records remain | Server/SDK queue deletion, support operation and vendor request paths; actual Console wording | UNKNOWN / HOLD |
| Account-deletion flow | No app account creation; Google account external | Owner review of actual app-specific Console form; do not invent a login/deletion backend | NOT APPLICABLE to nonexistent app account; contact/data rights remain |

No UNKNOWN/HOLD becomes NO. Actual categories/purposes/optionality/form answers remain
DRAFT, not selected or submitted. A passing CI run resolves regression questions,
not SDK data classification, remote deletion or legal approval.

## Current primary-source recheck — accessed12 October2026 (Asia/Jakarta)

- G4: https://support.google.com/googleplay/android-developer/answer/10787469 . Includes third-party SDK handling in app declarations; defines off-device collection and conditional sharing exceptions. Optionality requires user choice across users/regions. These general rules do not establish Komprexo's SDK payload/roles/retention.
- G2: https://developer.android.com/google/play/billing/security . Recommends backend purchase verification/acknowledgment for additional protection. Current client-only limitation remains disclosed; no backend authorized or added.
- R1: https://www.rumahweb.com/kebijakan-privasi/ . Describes automatic customer-related recording including IP/cookies and provider access to hosted customer data for technical/legal handling. Scope is general customer policy, not Komprexo visitor-log schema. OAuth customer-data deletion instructions do not establish universal access-log deletion. No Unlimited S account settings, server retention, subcontractor list or live HTTPS verified.

Original OFFICIAL_PRIVACY_EVIDENCE and bytecode/artifact records are preserved. R1
must not be relabeled as proof that this static site sets cookies or runs provider
SSO: no such scripts/forms are present. Host automatic server logging is a separate
question from browser code. No vendor correspondence, cPanel/Console access or packets collected.

## SDK inventory decision

SDK_PRIVACY_INVENTORY.json contains99 exact runtime artifacts including Billing9.1.0,
with versions/hash/source/profile/confidence/unresolved fields. Inventory is reviewed
and preserved without speculative edits; no new version-specific vendor schema has
been obtained. Release graph/manifests/hashes are revalidated by scoped checks; fresh
local/CI resolution results are reported with delivery. No SDK upgrade or dependency change.

## Account and future-product distinctions

Owner confirms Google Play account exists and identity verification is completed:
**VERIFIED owner statement**, not an independent Console inspection. Do not require
repeating account verification. Privately review monetized Personal public operator
fields against existing verified records; do not invent/copy residential/private details.
AndroidPrint Closed Testing is separate; it establishes no Komprexo track/tester/
production eligibility. Komprexo app-specific Console/track/product/Data Safety gates remain.

Business (Rp99,000, proposed100-image batches, ZIP export/folder-based processing) is
**future proposal only**, not implemented/marketed/purchasable/validated here. Existing
folder export does not establish proposed folder-based input processing or a Business
entitlement. Only current lifetime Premium product, no daily quota, batch20 with safety
limits is assessed. No Business product/engine/limit added.
