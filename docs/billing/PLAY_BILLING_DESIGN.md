# Phase 4A — client-only Play Billing

Prepared 9 October 2026, Asia/Jakarta. No real payment or Console configuration verified.

Billing **9.1.0**, stable per [official release notes](https://developer.android.com/google/play/billing/release-notes), using Java API from Kotlin2.1.20; minSdk23. References: [integration](https://developer.android.com/google/play/billing/integrate), [one-time options](https://developer.android.com/google/play/billing/one-time-product-multi-purchase-options-offers).

## Product and separation

Only INAPP **komprexo_premium_lifetime**, one-time non-consumable. Proposed Indonesia IDR49,000; checkout price comes from Play ProductDetails. No subscriptions/consumption/custom checkout/credentials.

PlayBillingTransport wraps official BillingClient, automatic reconnection,12s callback timeouts and sanitized response categories. BillingController serializes refresh/decisions/ack/callbacks with Mutex. Connection/ack retry at most3 times,1s/2s backoff. Startup/foreground/reconnect/callback/Restore query current INAPP ownership. Callback alone never grants entitlement: SKU/package/quantity1/nonempty token/PURCHASED must match. PENDING never unlocks/acks. PURCHASED must already be acknowledged or successfully acknowledged before grant. Duplicate callbacks are idempotent; incomplete ack stays Free and is retried at reconciliation.

BillingServices exposes process entitlement; release EntitlementProviderFactory delegates to it. Debug source set alone offers labeled transient test override. Fakes are test-only. Existing DailyQuotaManager remains unchanged: three independent Free quotas of5 successful outputs/day, single/batch share primary feature, Free max2/Premium max20 safety bounded. Internal resize/encoding charges only primary workflow, never double. Save/Share unmetered; failed/cancelled unused slots released. Accepted Premium reservations finish under original policy after downgrade; subsequent operations use current plan. Originals/outputs never deleted on entitlement changes.

## Options/UI

Only eligible base BUY option: expected SKU/positive price/nonempty token; reject rental/preorder/discount offer ID/limited quantity/time window. Base options sort by purchase option ID consistently. Owner should activate one base lifetime BUY option. Buy re-queries fresh ProductDetails; fresh offer token passed to official flow. Changed formatted price/micros requires another click after display update. Unavailable product disables Buy; ITEM_ALREADY_OWNED restores. Opening Play dialog leaves Free unchanged, repeated launches blocked, processing bounded2min. Distinct localized loading/ready/processing/pending/purchased/active/cancel/failure/unavailable/restored/not-owned/ack-pending/offline/revoked states.

## Offline ownership lease

Successful current ownership+ack yields24h lease storing checked/expires timestamps and SHA256 token digest only. Raw tokens transient in memory, never logs. AES-GCM AndroidKeyStore nonexportable key, fixed AAD/randomIV/private AtomicFile; backup disabled. Expiry/detected clock rollback/tamper/key loss fail closed. Reevaluated about every15s while process runs; network error never extends lease. Expired record removed next check/start, not necessarily while closed. Cache write failure prevents durable recovery but doesn't revoke live verified purchase.

Successful current ownership query without eligible PURCHASED revokes and clears cache. Network failure alone doesn't imply refund. Reinstall clears storage/key; online Restore with eligible same Play account recovers ownership.

## Client-only security limitations

**No backend**, Developer API credentials, server verification or real-time developer notifications. Official Billing IPC/current ownership is implemented; independent Play-public-key cryptographic receipt verification is not. Local lease authentication is not purchase-token authenticity. Rooted/instrumented clients, app patching/clock manipulation cannot be made tamper-proof. Refund/revocation may await online query, up to offline lease validity and Play propagation delays. Storage deletion/write failure prevents absolute durable revocation guarantees. Never advertise server-verified ownership.

Future separately authorized server verification/Google Play Developer API+notifications would improve authenticity/revocation; credentials must never enter APK. Real Console/test-track/license-tester purchase/pending/refund/reinstall remain owner actions. Deterministic tests are **not payments**. Exact CI counts in [validation](../PHASE4A_VALIDATION.md).
