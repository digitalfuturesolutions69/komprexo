# FASE 4A.1 editorial and privacy review

Historical FASE 4A.1 review. FASE 4A.2 subsequently confirms Digital Future Solutions as the public developer, the verified Personal-account individual as operator and Indonesia as country. Indonesian law is proposed subject to approval. Current owner gates and Pages preparation: [FASE 4A.2](../PHASE4A2_VALIDATION.md). Historical statements below record what was unresolved at 4A.1, not the latest identity status.

FASE 4A.3 further confirms intended launch audience adults18andolder, with Indonesian jurisdiction proposed subject to mandatory consumer/privacy law. Audience is no longer awaiting confirmation; age enforcement/child-law compliance is not claimed, and effective date remains unset until final approval/publication. Current evidence and owner gates: [FASE 4A.3](../PHASE4A3_VALIDATION.md).

Prepared 9 October 2026 (Asia/Jakarta). **Complete drafts only; not approved for publication or a legal opinion.** Accepted implementation baseline: `9bab33b2d8586c4338d22f6ed597f6655e6097ae`, branch `work`, application ID `com.komprexo.app`.

## Sources and delivery surfaces

The canonical policies are `content/legal/{en,id,es,pt-BR,hi}.json`. Their generated offline assets, English Markdown documents and static website pages have matching substantive text. All five languages retain publication gates, including Support and About. Store listings use `content/store-listing.json`; Premium checkout disclosures use `content/i18n/`. Existing generators remain the source of generated copies.

Reviewed: Privacy Policy, Terms of Use, Premium Purchase Policy, Support Guide, About, Data Safety Audit, five Store listings, website Privacy/Terms/Premium/Support and corresponding offline content. No developer identity, address, effective date, audience or governing law was supplied or invented.

## Editorial changes and fact consistency

| Topic | All five policy versions and relevant surfaces |
|---|---|
| Selected photos | Explain user-selected files and reading information, local processing and temporary copies; no Komprexo photo server. Online source providers may need a connection. |
| Save / Share | Explain deliberate exports and recipient/cloud-provider policies; app deletion cannot retract exported copies. Originals remain unchanged. |
| Temporary storage | Explain cleanup events and the initialization-time check for files older than 24 hours. Do not promise deletion on an exact 24-hour timer. |
| Local records | Explain quotas, unfinished operations and preferences; clearing data/uninstalling does not erase exports or Google purchases. |
| Free | Three separate counters: Compression, Resize, Convert each 5 successful images/local day. Single and batch share their feature counter; batch maximum 2. |
| Charging | Failed and unused cancelled credits are not charged; completed successes count. Internal transformations charge the primary tool once. Save/Share use no credits. |
| Premium | Unlimited daily use of those three features, all Smart Presets, batch maximum 20 subject to existing safety limits. |
| Purchase | One-time non-consumable defined in plain language, no subscription; intended Indonesian price Rp49.000, actual checkout price supplied by Google Play. |
| Restore / offline | Restore supported with the owning Play account. Offline ownership verification lasts up to 24 hours; a renewed internet check may be required. Premium screen disclosure now says the same. |
| Refund / revocation | Google Play process and applicable consumer law, no automatic approval or fixed timing promise. Valid revocation/refund may remove Premium; completed image outputs are not deleted. |
| Privacy limits | Possible Google purchase/device/network/diagnostic processing expressly unresolved. No zero-collection claim and no unsupported claim about exact vendor fields. |
| Retention / security | Distinguish temporary cleanup, local entitlement validity, vendor records and support-mail retention. Avoid absolute security/encryption guarantees. |
| Support | Official email everywhere: komprexo.support@gmail.com. Voluntary message details and email-provider handling disclosed; avoid private images, payment details and tokens. |
| Rights | Access/correction/deletion inquiries under applicable law; mandatory consumer rights preserved. No invented jurisdiction or audience. |

Dense implementation terms were moved out of consumer-facing prose; dependency versions, permission constants, cryptographic details and SDK findings remain in technical audit/design documents. The prior support claim that automatic telemetry was absent was replaced with an explicit unresolved diagnostic disclosure. No discounts, scarcity, additional benefits or new behavior were introduced.

## Owner gates before publication

- Legal developer name, business address, governing law and dispute venue.
- Intended audience, children-related obligations and an approved effective date. Preparation date is not an effective date.
- Exact Google/Billing diagnostic fields, destinations, purposes, required/optional status, retention/deletion and collection/sharing classifications, supported by current vendor information and actual integration evidence.
- Support mailbox operator, access controls, retention/deletion rules and response process.
- Final Data Safety answers, including applicable user-directed/service-provider exceptions; library presence alone is not evidence of collection or its absence.
- Qualified native-speaker review of English, Indonesian, Spanish, Brazilian Portuguese and Hindi for equivalent legal meaning, plus qualified legal review. Automated parity checks are not linguistic or legal certification.
- Play product/price/region configuration, real purchase/restore/refund testing, legal approval, public privacy URL and separate publication/domain authorization.

The release inventory remains 99 artifacts. Google Billing transitively includes diagnostic transport, SDK-local SQLite and location-related libraries. No application location permission/API was added; no claim is made that this establishes all vendor behavior. See [Data Safety Audit](../google-play/DATA_SAFETY_AUDIT.md).

## Official references reviewed

- [Google Play refund guidance](https://support.google.com/googleplay/answer/2479637)
- [Google Play Data Safety requirements](https://support.google.com/googleplay/android-developer/answer/10787469)
- [Android declaration of data use](https://developer.android.com/privacy-and-security/declare-data-use)

Reviewed 9 October 2026. These sources inform wording and unresolved questions; no Console declaration, vendor disclosure verification, real purchase or publication was performed.

## FASE 4A.4 final review and Pages readiness

10 October 2026, Asia/Jakarta. Reviewed all five versions of Privacy, Terms, Premium Purchase Policy, Support and About, offline Android copies, Store listings, Premium disclosures, Data Safety audit, audience declaration and Pages template. Existing identity/adult/jurisdiction/quotas/one-time price/refund/restore/up-to-24h offline facts remain accurate to source. No public developer legal name/address was invented, no consumer rights waived and no effective date set.

Verified editorial gap: planned Pages hosting logs/stores visitors' IP addresses for security according to GitHub, including signed-out visitors. Added equivalent consumer disclosure and GitHub privacy reference to en/id/es/pt-BR/hi Privacy, generated offline copies and static pages. Site-specific retention/deletion remain unverified. No image upload or app telemetry implementation was added. Native-speaker and qualified legal review remain required; automated equality does not certify legal translation.

Free project hosting at https://digitalfuturesolutions69.github.io/komprexo/ supersedes custom-domain planning as a prerequisite. Read-only metadata confirms public repository/default work/Pages not active. Removed unnecessary configure-pages from the inactive template and disabled checkout credential persistence; future deployment alone has narrowly scoped Pages/OIDC write. All active workflows remain unchanged and read-only. Current DRAFT guard still blocks publication.

[Final checklist](FINAL_LEGAL_APPROVAL_CHECKLIST.md) lists unresolved owner/legal/SDK/mailbox/translation/publication decisions. [Pages procedure](../google-play/STATIC_POLICY_PUBLISHING.md) defines activation/exact-commit review, deployment and approval-gated rollback through normal source commits without history rewriting. No activation, deployment, domain configuration, Play submission, release or AdMob occurred.


## FASE 4A.4A evidence-based gap closure

10 October 2026, Asia/Jakarta. Fresh resolution and per-artifact analysis cover 99 release runtime artifacts. Rechecked app launch/resume, image/cache/export, quota/localization, ownership cache and debug diagnostics against source; reviewed resolved Billing 9.1.0 and transport bytecode. The default Billing logger has a bundled CCT send/queue path. This is source evidence, not a claim that actual fields, server recipients, retention or deletion have been observed. Firebase encoder dependencies are not Firebase Analytics or Crashlytics.

Corrected a substantive disclosure gap: Google Play connections and ownership/product checks start at app launch/resume for Free users too. Added equivalent plain-language wording to all five Privacy drafts, generated offline policies and Store listings. Preserved local photo processing, no automatic photo upload, statutory rights, one-time checkout/restore/refund/up-to-24h limits, developer/operator distinction and unresolved effective dates. No Billing, quotas, image processing, SDK or permissions changed.

Added a field-by-field [SDK inventory](../google-play/SDK_PRIVACY_REVIEW.md), [official-source register](../google-play/OFFICIAL_PRIVACY_EVIDENCE.md), [data-flow matrix](../google-play/DATA_FLOW_MATRIX.md), [proposed Data Safety worksheet](../google-play/DATA_SAFETY_FINAL_REVIEW.md) and [mailbox operations proposal](../support/SUPPORT_MAILBOX_OPERATIONS.md). Narrow payment-provider and user-directed sharing exceptions are conditional; neither exempts all SDK or purchase data. New browser checks inspect actual requests from all static pages under root and /komprexo/ paths.

Official Personal-account guidance requires review of public verified legal-name/country/contact and monetization full-address disclosure. No personal details were invented or added. Mailbox retention of 90 days for closed text and 30 days for attachments is an unadopted proposal, not current practice. Native-speaker/legal review and actual Console/vendor/runtime facts remain required. [Owner decision](OWNER_PUBLICATION_DECISION.md): **BLOCKED — SPECIFIC EVIDENCE REQUIRED.** All public drafts remain unpublished.
