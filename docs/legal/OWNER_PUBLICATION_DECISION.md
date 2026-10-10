> FASE 4A.4D: publication remains BLOCKED. Hosting preparation now targets Rumahweb;
> prior GitHub-specific conclusions below are historical and do not establish Rumahweb
> practices. Provider evidence/configuration and live hosting tests are required.
> See [current procedure](../google-play/RUMAHWEB_STATIC_DEPLOYMENT.md).

# Owner publication decision — FASE 4A.4A

Prepared 10 October 2026 (Asia/Jakarta). Repository digitalfuturesolutions69/komprexo, branch work; expected baseline a09ad3c733f2f2211ec9cefc6b9ca2557fba2a57 verified locally and remotely before edits. Application ID com.komprexo.app unchanged. This evidence package closes inspectable technical/editorial gaps; it does not approve publication or certify law/SDK behavior.

## A. Verified and closed items

| Item | Evidence and exact scope |
|---|---|
| Baseline and inventory | Clean local baseline matched fetched remote. Fresh release resolution of 99exact artifact coordinates; per-artifact versions, parent edges, POM/artifact/class hashes, manifest permissions/components and13 requested assessment fields recorded. This verifies the inventory, not every runtime behavior. |
| App image/storage flows | Source reads only selected URI, processes locally into separate outputs, preserves provider original during processing, stages private/shared cache and exports only on user Save/Share. Temporary stale 24h cleanup occurs on storage initialization. No app photo server/backend/account/analytics/ads added. Existing fixture regressions support processing/output/storage behavior. |
| Local preferences/quotas | Three Free 5-success/day counters, single/batch share,2 Free / 20 Premium limits, primary-feature charging, cancellation/concurrency/persistence unchanged. Reservations/clock/day are local, not image history. These are client-side controls; data clearing can reset them. |
| Billing foreground initiation | MainActivity/BillingServices starts Play connection/query at launch/resume for Free users too. Corrected all 5 Privacy drafts/offline/web and Store listings to disclose checks can use internet without checkout. Image processing is offline, not all SDK functionality. |
| SDK bundled transport path | Resolved Billing 9.1.0 default logger -> TransportRuntime/CCT/Transport.send path established by bytecode, plus potential device/network metadata reads and SQLite queue. Default7-day local cleanup threshold verified in 3.1.8 bytecode; no guaranteed timed erasure or server retention inference. |
| No Firebase Analytics/Crashlytics integration in graph | Firebase encoder helpers are serialization dependencies. No corresponding Analytics/Crashlytics/app-init artifacts in actual release inventory. This does not mean no Billing diagnostics. |
| Manifest and startup | Four requested permissions; Billing/GMS/transport components documented per artifact. AndroidX startup metadata initializes lifecycle/profile/emoji; font-provider network behavior separately flagged. No new SDK or permission. |
| Premium disclosures | Product komprexo_premium_lifetime/INAPP, non-consumable/no subscription, actual Play ProductDetails checkout price, proposed Rp49.000, owning-account restore, pending/ack/cancel, current-query revocation and up to 24h encrypted offline cache align with code. Client-only verification limitation retained. Real payment proof remains outstanding. |
| GitHub Pages privacy/source | Official IP logging/storage disclosure preserved across5Privacy versions; site has no added scripts/forms/login/photo upload/backend/analytics/ads. Build is static 36 routes / 39 files with /komprexo/ paths. Runtime browser-origin checks detect unintended third-party requests. Hosting remains inactive. |
| Identity requirements clarified | Official Google sources confirm Personal legal-name/country/developer-email display and full-address display when monetized. Display name Digital Future Solutions doesn't replace verified individual. No actual personal name/address or credentials added; required disclosures are not erased to avoid privacy. |
| Public gates | All policies remain DRAFT; effective date unset. Inactive Pages template remains outside workflows. No Forms/Console/Gmail/vendor account changed, no publication/release/merge. |

“Closed” means the narrow fact/wording has supporting evidence. It does not settle opaque vendor/server or legal questions.

## B. Items awaiting vendor/runtime verification

- Google/Billing 9.1.0 logger schema: actual diagnostic/ownership fields, stable identifiers, triggers, field values, optional controls, purposes and destinations/regions/redirects/onward recipients.
- Actual Free-startup/resume/reconnect traffic, correctly installed Play test purchase/pending/cancel/ack/restore/refund/revoke flows, device/provider differences and relevant API 23/26/28/36 network/queue behavior. Existing tests are not real Play transactions or packet capture.
- Device/SIM/network/IP-based location or IDs used by SDK/Play services. Transitive location artifact and missing app permission alone answer neither collection nor noncollection.
- Server retention/deletion/request procedures, recipient/service-provider roles and sharing exceptions, all-transfer encryption. Static queue default/HTTPS cannot establish them.
- Emoji/font provider processing and OS/profile behavior on actual devices. No provider diagnosis fabricated.
- GitHub site-specific visitor-log retention/deletion not established; source confirms general host IP logging only.

Evidence plan and potential Play categories: [DATA_SAFETY_FINAL_REVIEW](../google-play/DATA_SAFETY_FINAL_REVIEW.md). No vendor message, packet capture or payment was performed; obtain separate task authorization where needed.

## C. Items awaiting owner confirmation

- Privately inspect verified Personal account/legal operator, monetization/full-address display, actual public contact fields and region-specific requirements. Do not falsely promise that Play can hide the verified legal name/address. No required information removed or invented.
- Actual Play product/price/regions/tax/merchant/testing configuration, audience declaration/rating/Restrict Minor Access, SDK guidance and Data Safety form version. No Console inspection/settings/submission occurred.
- Actual support-mailbox access/authentication/recovery/handling/retention/deletion. Proposed90days after closed text / 30 days attachments need approval and operational adoption; NOT current practice. See [mailbox checklist](../support/SUPPORT_MAILBOX_OPERATIONS.md).
- Actual launch countries. en/id/es/pt-BR/hi language support is not evidence of country availability or all market legal applicability.
- Required public operator disclosures and final legal/translation text. After factual gaps resolved, approve exact public commit/site activation/deployment; coordinate effective date with actual publication only, not draft preparation.
- Separate future website/application release/Console approval, protected environment/settings/version review and rollback authority. Custom domain not required. No automatic follow-on phase authorized.

## D. Items requiring qualified review

Legal advisers: Indonesian proposed jurisdiction plus mandatory consumer/privacy/child/cross-border rights in actual markets; required developer disclosures; Data Safety collection/sharing/payment/user-directed exceptions; startup diagnostics and any legally required prominent in-app notice/affirmative consent; retention/request operation. If an implementation change is necessary, obtain separate authorization; do not claim an editorial policy fixes missing consent behavior.

Qualified native speakers/legal translators: equivalent meaning in Indonesian, English, Spanish, Brazilian Portuguese and Hindi, including uncertainty, no all-offline promise, one-time/checkout/refund/restore/up to 24h limits and statutory rights. Automated equality/character counts aren't certified translation.

## E. Publication readiness decision

**BLOCKED — SPECIFIC EVIDENCE REQUIRED.** Material SDK field/recipient/purpose/retention/deletion/optionality/classification evidence, actual Console/market/identity and mailbox facts, qualified legal/native review and final public approval remain outstanding. Do not choose READY FOR OWNER LEGAL APPROVAL solely because CI succeeds. This package is ready to review as an evidence inventory, not ready to publish.

[Official sources](../google-play/OFFICIAL_PRIVACY_EVIDENCE.md) · [SDK inventory](../google-play/SDK_PRIVACY_REVIEW.md) · [data flow](../google-play/DATA_FLOW_MATRIX.md) · [proposed Console answers](../google-play/DATA_SAFETY_FINAL_REVIEW.md) · [remaining final checklist](FINAL_LEGAL_APPROVAL_CHECKLIST.md).

The final exact-commit CI/test evidence accompanies delivery. Publication remains NOT AUTHORIZED/NOT PERFORMED; no Pages workflow activation, effective date, Android release, Console submission, Billing/quota/processing change, AdMob/backend, merge or force push.
