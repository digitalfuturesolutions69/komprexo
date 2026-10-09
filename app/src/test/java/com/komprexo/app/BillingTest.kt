package com.komprexo.app

import com.komprexo.app.access.*
import com.komprexo.app.billing.*
import com.komprexo.app.processing.Preset
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class BillingTest {
    private val transport=FakeBillingTransport()
    private val store=MemoryOwnershipStore()
    private var time=1_000_000L
    private val controller=BillingController(transport,store,{time},{})
    private fun run(block:suspend()->Unit)=runBlocking { block() }
    private fun free()=assertEquals(EntitlementState.Free,controller.state.value)
    private fun premium()=assertEquals(EntitlementState.Premium,controller.state.value)
    @Test fun loadsEligibleProductAndLocalizedPrice()=run { controller.refresh();assertEquals(BillingStatus.READY,controller.ui.value.status);assertEquals("IDR 49,000",controller.ui.value.price);assertTrue(controller.ui.value.canBuy);free() }
    @Test fun connectionRetriesAreBounded()=run { transport.connection=BillingCode.DISCONNECTED;controller.refresh();assertEquals(3,transport.connectCalls);free();assertFalse(controller.ui.value.canBuy) }
    @Test fun reconnectRecoversProduct()=run { transport.connection=BillingCode.NETWORK;controller.refresh();transport.connection=BillingCode.OK;controller.refresh();assertEquals(BillingStatus.READY,controller.ui.value.status) }
    @Test fun productMissingIsUnavailable()=run { transport.offers=emptyList();controller.refresh();assertEquals(BillingStatus.UNAVAILABLE,controller.ui.value.status);free() }
    @Test fun productQueryErrorDisablesPurchase()=run { transport.productCode=BillingCode.ERROR;controller.refresh();assertFalse(controller.ui.value.canBuy) }
    @Test fun ownershipStillRestoredIfProductUnavailable()=run { transport.offers=emptyList();transport.owned=listOf(purchasedFixture());controller.refresh(true);premium();assertEquals(BillingStatus.RESTORED,controller.ui.value.status) }
    @Test fun launchDoesNotGrantPremium()=run { controller.refresh();var launched=false;controller.buy { launched=true;BillingCode.OK };assertTrue(launched);free();assertEquals(BillingStatus.PROCESSING,controller.ui.value.status) }
    @Test fun doubleTapLaunchesOnlyOnce()=run { controller.refresh();var count=0;repeat(2) { controller.buy { count++;BillingCode.OK } };assertEquals(1,count);free() }
    @Test fun unavailableProductNeverLaunches()=run { transport.offers=emptyList();controller.refresh();controller.buy { fail("must not launch");BillingCode.OK };free() }
    @Test fun pendingDoesNotUnlockOrAcknowledge()=run { transport.owned=listOf(purchasedFixture(false).copy(payment=PaymentState.PENDING));controller.callback(BillingCode.OK);free();assertEquals(0,transport.acknowledgments);assertEquals(BillingStatus.PENDING,controller.ui.value.status);assertFalse(controller.ui.value.canBuy) }
    @Test fun cancellationDoesNotUnlock()=run { controller.refresh();controller.callback(BillingCode.CANCELLED);free();assertEquals(BillingStatus.CANCELLED,controller.ui.value.status) }
    @Test fun errorDoesNotUnlock()=run { controller.callback(BillingCode.ERROR);free();assertEquals(BillingStatus.FAILED,controller.ui.value.status) }
    @Test fun purchasedAcknowledgedRecordGrantsPremium()=run { transport.owned=listOf(purchasedFixture());controller.callback(BillingCode.OK);premium();assertEquals(BillingStatus.PURCHASED,controller.ui.value.status);assertFalse(controller.ui.value.canBuy) }
    @Test fun callbacksRequireOwnershipRequery()=run { controller.callback(BillingCode.OK);free();assertEquals(BillingStatus.READY,controller.ui.value.status) }
    @Test fun unacknowledgedPurchaseIsAcknowledgedFirst()=run { transport.owned=listOf(purchasedFixture(false));controller.refresh();assertEquals(1,transport.acknowledgments);premium() }
    @Test fun duplicateCallbacksDoNotRepeatAcknowledgment()=run { transport.owned=listOf(purchasedFixture(false));repeat(3) { controller.callback(BillingCode.OK) };assertEquals(1,transport.acknowledgments);premium() }
    @Test fun transientAcknowledgmentRetries()=run { transport.owned=listOf(purchasedFixture(false));transport.ackReplies=mutableListOf(BillingCode.NETWORK,BillingCode.OK);controller.refresh();assertEquals(2,transport.acknowledgments);premium() }
    @Test fun failedAcknowledgmentRemainsFree()=run { transport.owned=listOf(purchasedFixture(false));transport.ackReplies=mutableListOf(BillingCode.ERROR);controller.refresh();free();assertEquals(3,transport.acknowledgments);assertEquals(BillingStatus.ACKNOWLEDGMENT_PENDING,controller.ui.value.status) }
    @Test fun incompleteAcknowledgmentReconcilesLater()=run { transport.owned=listOf(purchasedFixture(false));transport.ackReplies=mutableListOf(BillingCode.NETWORK);controller.refresh();free();transport.ackReplies=mutableListOf(BillingCode.OK);controller.refresh();premium() }
    @Test fun restoreNoPurchaseShowsNotOwned()=run { controller.refresh(true);free();assertEquals(BillingStatus.NOT_OWNED,controller.ui.value.status) }
    @Test fun alreadyOwnedResponseRestores()=run { transport.owned=listOf(purchasedFixture());controller.callback(BillingCode.ALREADY_OWNED);premium() }
    @Test fun successfulEmptyQueryRevokesOwnership()=run { transport.owned=listOf(purchasedFixture());controller.refresh();premium();transport.owned=emptyList();controller.refresh();free();assertNull(store.cache);assertEquals(BillingStatus.REVOKED,controller.ui.value.status) }
    @Test fun queryFailureCannotBeTreatedAsRevocation()=run { transport.owned=listOf(purchasedFixture());controller.refresh();transport.purchaseCode=BillingCode.NETWORK;controller.refresh();premium();assertEquals(BillingStatus.OFFLINE_CACHED,controller.ui.value.status);assertFalse(controller.ui.value.canBuy) }
    @Test fun recentAuthenticatedCacheRestoresOffline()=run { transport.owned=listOf(purchasedFixture());controller.refresh();transport.connection=BillingCode.UNAVAILABLE;val restarted=BillingController(transport,store,{time},{});restarted.refresh();assertEquals(EntitlementState.Premium,restarted.state.value) }
    @Test fun expiredCacheFallsBackToFree()=run { transport.owned=listOf(purchasedFixture());controller.refresh();time+=CACHE_LIFETIME_MILLIS;transport.connection=BillingCode.UNAVAILABLE;controller.refresh();free();assertNull(store.cache) }
    @Test fun clockRollbackInvalidatesCachePermanently()=run { transport.owned=listOf(purchasedFixture());controller.refresh();time--;transport.connection=BillingCode.UNAVAILABLE;controller.refresh();free();assertNull(store.cache);time++;controller.refresh();free() }
    @Test fun cacheTamperOrBadLifetimeDoesNotGrant()=run { store.cache=OwnershipCache(time,time+CACHE_LIFETIME_MILLIS+1,"a".repeat(64));transport.connection=BillingCode.UNAVAILABLE;controller.refresh();free() }
    @Test fun cacheReadFailureFailsClosed()=run { store.failed=true;transport.connection=BillingCode.UNAVAILABLE;controller.refresh();free() }
    @Test fun cacheWriteFailureDoesNotDiscardVerifiedLivePurchase()=run { store.failed=true;transport.owned=listOf(purchasedFixture());controller.refresh();premium() }
    @Test fun wrongProductIsIgnored()=run { transport.owned=listOf(purchasedFixture().copy(productIds=listOf("other")));controller.refresh();free() }
    @Test fun wrongPackageIsIgnored()=run { transport.owned=listOf(purchasedFixture().copy(packageMatches=false));controller.refresh();free();assertEquals(0,transport.acknowledgments) }
    @Test fun unknownPaymentStateIsIgnored()=run { transport.owned=listOf(purchasedFixture().copy(payment=PaymentState.UNKNOWN));controller.refresh();free() }
    @Test fun blankPurchaseTokenIsIgnored()=run { transport.owned=listOf(purchasedFixture(token=""));controller.refresh();free() }
    @Test fun multiQuantityNonConsumableIsRejected()=run { transport.owned=listOf(purchasedFixture().copy(quantity=2));controller.refresh();free() }
    @Test fun purchaseDiagnosticsRedactTokens() { assertFalse(purchasedFixture(token="sensitive-fixture").toString().contains("sensitive-fixture"));assertFalse(transport.offers.first().toString().contains("offer-fixture")) }
    @Test fun rentalOffersAreRejected() { assertNull(selectLifetimeOffer(listOf(transport.offers.first().copy(rental=true)))) }
    @Test fun preorderOffersAreRejected() { assertNull(selectLifetimeOffer(listOf(transport.offers.first().copy(preorder=true)))) }
    @Test fun discountedOrLimitedOffersAreRejected() { assertNull(selectLifetimeOffer(listOf(transport.offers.first().copy(discount=true))));assertNull(selectLifetimeOffer(listOf(transport.offers.first().copy(limited=true)))) }
    @Test fun wrongProductOrFreePriceOfferIsRejected() { assertNull(selectLifetimeOffer(listOf(transport.offers.first().copy(product="other"))));assertNull(selectLifetimeOffer(listOf(transport.offers.first().copy(priceMicros=0)))) }
    @Test fun deterministicEligibleBuyOptionSelection() { val b=transport.offers.first().copy(option="b");val a=b.copy(option="a");assertEquals(a,selectLifetimeOffer(listOf(b,a))) }
    @Test fun freePremiumFreeTransitionsRetainAllThreeQuotaCounters()=run {
        val quotaStore=object:QuotaStore { var ledger=QuotaLedger();override suspend fun update(change:(QuotaLedger)->QuotaLedger):QuotaLedger { ledger=change(ledger);return ledger } }
        val quota=DailyQuotaManager(quotaStore,controller)
        for(op in Operation.entries) { val r=quota.reserve(op,1);quota.settle(r,0);quota.release(r) }
        transport.owned=listOf(purchasedFixture());controller.refresh()
        for(op in Operation.entries) { val r=quota.reserve(op,20,Preset.WEBSITE);quota.settle(r,0);quota.release(r);assertEquals(1,quotaStore.ledger.used(op)) }
        transport.owned=emptyList();controller.refresh();free()
        for(op in Operation.entries) { val r=quota.reserve(op,1);quota.settle(r,0);quota.release(r);assertEquals(2,quotaStore.ledger.used(op)) }
    }
    @Test fun refreshedPriceRequiresAnotherExplicitClick()=run { controller.refresh();transport.offers=listOf(transport.offers.first().copy(price="IDR 50,000",priceMicros=50_000_000_000));var count=0;controller.buy { count++;BillingCode.OK };assertEquals(0,count);assertEquals("IDR 50,000",controller.ui.value.price);controller.buy { count++;BillingCode.OK };assertEquals(1,count) }
    @Test fun disappearedOfferCannotLaunchFromStaleDetails()=run { controller.refresh();transport.offers=emptyList();controller.buy { fail("Stale offer must not launch");BillingCode.OK };free();assertFalse(controller.ui.value.canBuy) }
    @Test fun freshOfferTokenIsPassedToPurchase()=run { controller.refresh();transport.offers=listOf(transport.offers.first().copy(token="new-offer"));controller.buy { assertEquals("new-offer",it.token);BillingCode.OK };free() }
    @Test fun launchAlreadyOwnedReconcilesAutomatically()=run { controller.refresh();transport.owned=listOf(purchasedFixture());controller.buy { BillingCode.ALREADY_OWNED };premium();assertEquals(BillingStatus.RESTORED,controller.ui.value.status) }
    @Test fun unavailableConnectionBeforeLaunchFailsClosed()=run { controller.refresh();transport.connection=BillingCode.UNAVAILABLE;controller.buy { fail("Offline purchase must not launch");BillingCode.OK };free();assertFalse(controller.ui.value.canBuy) }

}
