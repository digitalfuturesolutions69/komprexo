package com.komprexo.app

import androidx.test.platform.app.InstrumentationRegistry
import com.komprexo.app.access.*
import com.komprexo.app.billing.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Fake Play responses exercise Android compatibility; no real transactions. */
class BillingCompatibilityTest {
    private val transport=FakeBillingTransport()
    private val store=MemoryOwnershipStore()
    private var time=1_000_000L
    private val controller=BillingController(transport,store,{time},{})
    private fun run(block:suspend()->Unit)=runBlocking { block() }
    @Test fun connectionFailureAndRecovery()=run { transport.connection=BillingCode.NETWORK;controller.refresh();assertEquals(3,transport.connectCalls);transport.connection=BillingCode.OK;controller.refresh();assertTrue(controller.ui.value.canBuy) }
    @Test fun productEligibilityAndLocalizedPrice()=run { val valid=transport.offers.first();transport.offers=listOf(valid.copy(rental=true),valid);controller.refresh();assertEquals(valid.price,controller.ui.value.price);assertEquals(valid,selectLifetimeOffer(transport.offers)) }
    @Test fun openingPurchaseDialogDoesNotUnlock()=run { controller.refresh();controller.buy { BillingCode.OK };assertEquals(BillingStatus.PROCESSING,controller.ui.value.status);assertEquals(EntitlementState.Free,controller.state.value) }
    @Test fun pendingNeverAcknowledgedOrUnlocked()=run { transport.owned=listOf(purchasedFixture(false).copy(payment=PaymentState.PENDING));controller.callback(BillingCode.OK);assertEquals(0,transport.acknowledgments);assertEquals(EntitlementState.Free,controller.state.value) }
    @Test fun duplicateCallbacksSettleAcknowledgmentOnce()=run { transport.owned=listOf(purchasedFixture(false));repeat(3){controller.callback(BillingCode.OK)};assertEquals(1,transport.acknowledgments);assertEquals(EntitlementState.Premium,controller.state.value) }
    @Test fun transientAcknowledgmentRecovery()=run { transport.owned=listOf(purchasedFixture(false));transport.ackReplies=mutableListOf(BillingCode.NETWORK,BillingCode.OK);controller.refresh();assertEquals(2,transport.acknowledgments);assertEquals(EntitlementState.Premium,controller.state.value) }
    @Test fun failedAcknowledgmentAndCancellationRemainFree()=run { transport.owned=listOf(purchasedFixture(false));transport.ackReplies=mutableListOf(BillingCode.ERROR);controller.refresh();assertEquals(BillingStatus.ACKNOWLEDGMENT_PENDING,controller.ui.value.status);controller.callback(BillingCode.CANCELLED);assertEquals(EntitlementState.Free,controller.state.value) }
    @Test fun restoreAndLossOfOwnership()=run { transport.owned=listOf(purchasedFixture());controller.refresh(true);assertEquals(BillingStatus.RESTORED,controller.ui.value.status);transport.owned=emptyList();controller.refresh();assertEquals(BillingStatus.REVOKED,controller.ui.value.status);assertNull(store.cache) }
    @Test fun offlineExpiryAndRollbackDoNotGrant()=run { transport.owned=listOf(purchasedFixture());controller.refresh();transport.connection=BillingCode.UNAVAILABLE;time--;controller.refresh();assertEquals(EntitlementState.Free,controller.state.value);assertNull(store.cache) }
    @Test fun transientQueryErrorKeepsOnlyRecentOwnership()=run { transport.owned=listOf(purchasedFixture());controller.refresh();transport.purchaseCode=BillingCode.ERROR;controller.refresh();assertEquals(EntitlementState.Premium,controller.state.value);time+=CACHE_LIFETIME_MILLIS;controller.refresh();assertEquals(EntitlementState.Free,controller.state.value) }
    @Test fun wrongPackageAndUnsupportedPurchaseIgnored()=run { transport.owned=listOf(purchasedFixture(false).copy(packageMatches=false));controller.refresh();assertEquals(0,transport.acknowledgments);assertEquals(EntitlementState.Free,controller.state.value) }
    @Test fun realAndroidKeyStoreCacheSurvivesStoreRecreation()=run {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val name="billing-test-cache.bin";val file=File(context.filesDir,name)
        try {
            val cache=OwnershipCache(time,time+CACHE_LIFETIME_MILLIS,"a".repeat(64))
            SealedOwnershipStore(context,name).write(cache)
            assertEquals(cache,SealedOwnershipStore(context,name).read())
            assertFalse(String(file.readBytes(),Charsets.ISO_8859_1).contains("a".repeat(64)))
        } finally { file.delete() }
    }
    @Test fun authenticatedCacheTamperFailsClosed()=run {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val name="billing-test-tamper.bin";val file=File(context.filesDir,name)
        try {
            val cache=SealedOwnershipStore(context,name)
            cache.write(OwnershipCache(time,time+CACHE_LIFETIME_MILLIS,"b".repeat(64)))
            val bytes=file.readBytes();bytes[bytes.lastIndex]=(bytes.last().toInt() xor 1).toByte();file.writeBytes(bytes)
            assertNull(cache.read());assertFalse(file.exists())
        } finally { file.delete() }
    }
}
