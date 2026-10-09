package com.komprexo.app.billing

const val PREMIUM_PRODUCT = "komprexo_premium_lifetime"
const val CACHE_LIFETIME_MILLIS = 24 * 60 * 60 * 1000L

enum class BillingStatus { LOADING, READY, PROCESSING, PENDING, PURCHASED, ACTIVE, CANCELLED, FAILED, UNAVAILABLE, RESTORED, NOT_OWNED, ACKNOWLEDGMENT_PENDING, OFFLINE_CACHED, REVOKED }
data class BillingUiState(val status: BillingStatus = BillingStatus.LOADING, val price: String? = null,
    val premium: Boolean = false, val canBuy: Boolean = false, val busy: Boolean = false)
enum class BillingCode { OK, CANCELLED, ALREADY_OWNED, DISCONNECTED, UNAVAILABLE, NETWORK, ERROR }
data class BillingReply<T>(val code: BillingCode, val value: T? = null)
/** Only eligible non-discounted lifetime BUY options are supported. Never log tokens. */
data class PurchaseOffer(val product: String, val option: String?, val token: String, val price: String,
    val priceMicros: Long, val rental: Boolean = false, val preorder: Boolean = false,
    val discount: Boolean = false, val limited: Boolean = false) {
    override fun toString() = "PurchaseOffer(product=$product, price=$price)"
}
fun selectLifetimeOffer(offers: List<PurchaseOffer>): PurchaseOffer? = offers.filter {
    it.product == PREMIUM_PRODUCT && !it.rental && !it.preorder && !it.discount && !it.limited &&
        it.token.isNotBlank() && it.price.isNotBlank() && it.priceMicros > 0
}.sortedWith(compareBy<PurchaseOffer> { it.option ?: "" }.thenBy { it.token }).firstOrNull()
enum class PaymentState { PURCHASED, PENDING, UNKNOWN }
data class OwnedPurchase(val productIds: List<String>, val token: String, val payment: PaymentState,
    val acknowledged: Boolean, val packageMatches: Boolean = true, val quantity: Int = 1) {
    override fun toString() = "OwnedPurchase(payment=$payment, acknowledged=$acknowledged)"
}
interface BillingTransport {
    suspend fun connect(): BillingCode
    suspend fun products(): BillingReply<List<PurchaseOffer>>
    suspend fun purchases(): BillingReply<List<OwnedPurchase>>
    suspend fun acknowledge(token: String): BillingCode
}
/** Authenticated, non-Boolean cache. No raw purchase token or personal order data persisted. */
data class OwnershipCache(val checkedAt: Long, val expiresAt: Long, val tokenHash: String) {
    fun valid(now: Long) = tokenHash.matches(Regex("[a-f0-9]{64}")) && now >= checkedAt &&
        expiresAt - checkedAt == CACHE_LIFETIME_MILLIS && now < expiresAt
}
interface OwnershipStore {
    suspend fun read(): OwnershipCache?
    suspend fun write(cache: OwnershipCache?)
}
