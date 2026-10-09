package com.komprexo.app.billing

/** Deterministic test double. Never connected to Google Play or real payments. */
class FakeBillingTransport : BillingTransport {
    var connection = BillingCode.OK
    var connectCalls = 0
    var productCode = BillingCode.OK
    var offers = listOf(PurchaseOffer(PREMIUM_PRODUCT,"buy","offer-fixture","IDR 49,000",49_000_000_000))
    var purchaseCode = BillingCode.OK
    var owned = emptyList<OwnedPurchase>()
    var acknowledgments = 0
    var ackReplies = mutableListOf(BillingCode.OK)
    override suspend fun connect(): BillingCode { connectCalls++; return connection }
    override suspend fun products() = BillingReply(productCode,offers)
    override suspend fun purchases() = BillingReply(purchaseCode,owned)
    override suspend fun acknowledge(token: String): BillingCode { acknowledgments++;return if(ackReplies.size>1) ackReplies.removeAt(0) else ackReplies.first() }
}
class MemoryOwnershipStore : OwnershipStore {
    var cache: OwnershipCache? = null
    var failed = false
    override suspend fun read(): OwnershipCache? { if(failed) error("Fixture storage failure");return cache }
    override suspend fun write(cache: OwnershipCache?) { if(failed) error("Fixture storage failure");this.cache=cache }
}
fun purchasedFixture(acknowledged: Boolean = true, token: String = "test-fixture") = OwnedPurchase(listOf(PREMIUM_PRODUCT),token,PaymentState.PURCHASED,acknowledged)
