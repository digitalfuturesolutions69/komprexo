package com.komprexo.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.coroutines.resume

/** Official Play binder transport. No HTTP client, token logging or consumable API. */
class PlayBillingTransport(context: Context, private val changed: (BillingCode) -> Unit,
    private val disconnected: () -> Unit) : BillingTransport {
    private val packageName = context.applicationContext.packageName
    private val details = mutableMapOf<String, ProductDetails>()
    private val client = BillingClient.newBuilder(context.applicationContext)
        .setListener { result, _ -> changed(code(result)) }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    override suspend fun connect(): BillingCode {
        if (client.isReady) return BillingCode.OK
        return withTimeout(12_000) {
            suspendCancellableCoroutine { continuation ->
                client.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        if (continuation.isActive) continuation.resume(code(result))
                    }
                    override fun onBillingServiceDisconnected() { disconnected() }
                })
            }
        }
    }
    override suspend fun products(): BillingReply<List<PurchaseOffer>> = withTimeout(12_000) {
        suspendCancellableCoroutine { continuation ->
            val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(
                QueryProductDetailsParams.Product.newBuilder().setProductId(PREMIUM_PRODUCT)
                    .setProductType(BillingClient.ProductType.INAPP).build())).build()
            client.queryProductDetailsAsync(params) { result, queried ->
                val offers = mutableListOf<PurchaseOffer>()
                details.clear()
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queried.productDetailsList.filter { it.productId == PREMIUM_PRODUCT && it.productType == BillingClient.ProductType.INAPP }.forEach { product ->
                        product.oneTimePurchaseOfferDetailsList.orEmpty().forEach offerLoop@ { item ->
                            val token = item.offerToken?.takeIf { it.isNotBlank() } ?: return@offerLoop
                            details[token] = product
                            offers.add(PurchaseOffer(product.productId, item.purchaseOptionId, token,
                                item.formattedPrice, item.priceAmountMicros, item.rentalDetails != null,
                                item.preorderDetails != null, !item.offerId.isNullOrBlank() || item.discountDisplayInfo != null,
                                item.limitedQuantityInfo != null || item.validTimeWindow != null))
                        }
                    }
                }
                if (continuation.isActive) continuation.resume(BillingReply(code(result), offers))
            }
        }
    }
    override suspend fun purchases(): BillingReply<List<OwnedPurchase>> = withTimeout(12_000) {
        suspendCancellableCoroutine { continuation ->
            client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()) { result, owned ->
                val mapped = owned.map { purchase ->
                    val packageMatches = try { JSONObject(purchase.originalJson).optString("packageName") == packageName } catch (_: Exception) { false }
                    OwnedPurchase(purchase.products, purchase.purchaseToken, when (purchase.purchaseState) {
                        Purchase.PurchaseState.PURCHASED -> PaymentState.PURCHASED
                        Purchase.PurchaseState.PENDING -> PaymentState.PENDING
                        else -> PaymentState.UNKNOWN
                    }, purchase.isAcknowledged, packageMatches, purchase.quantity)
                }
                if (continuation.isActive) continuation.resume(BillingReply(code(result), mapped))
            }
        }
    }
    override suspend fun acknowledge(token: String): BillingCode = withTimeout(12_000) {
        suspendCancellableCoroutine { continuation ->
            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(token).build()) { result ->
                if (continuation.isActive) continuation.resume(code(result))
            }
        }
    }
    fun launch(activity: Activity, offer: PurchaseOffer): BillingCode {
        if (!client.isReady || activity.isFinishing || activity.isDestroyed) return BillingCode.UNAVAILABLE
        val product = details[offer.token] ?: return BillingCode.UNAVAILABLE
        val selected = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).setOfferToken(offer.token).build()
        return code(client.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(selected)).build()))
    }
    private fun code(result: BillingResult) = when (result.responseCode) {
        BillingClient.BillingResponseCode.OK -> BillingCode.OK
        BillingClient.BillingResponseCode.USER_CANCELED -> BillingCode.CANCELLED
        BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> BillingCode.ALREADY_OWNED
        BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> BillingCode.DISCONNECTED
        BillingClient.BillingResponseCode.NETWORK_ERROR, BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> BillingCode.NETWORK
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE, BillingClient.BillingResponseCode.ITEM_UNAVAILABLE, BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> BillingCode.UNAVAILABLE
        else -> BillingCode.ERROR
    }
}
