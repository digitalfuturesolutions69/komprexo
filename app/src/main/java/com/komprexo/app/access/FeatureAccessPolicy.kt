package com.komprexo.app.access

import com.komprexo.app.processing.Preset
import kotlinx.coroutines.flow.StateFlow

sealed interface EntitlementState {
    data object Free : EntitlementState
    data object Premium : EntitlementState
}
interface EntitlementProvider { val state: StateFlow<EntitlementState> }
enum class Operation { COMPRESS, RESIZE, CONVERT }
enum class Restriction { DAILY_QUOTA, BATCH_LIMIT, PREMIUM_PRESET, BUSY, QUOTA_UNAVAILABLE }
class AccessDenied(val reason: Restriction) : Exception(reason.name)

/** Authoritative business limits; codec, byte and memory limits remain independent. */
object FeatureAccessPolicy {
    const val DAILY_FREE = 5
    val basicPresets = setOf(Preset.DOCUMENT, Preset.MARKETPLACE, Preset.CUSTOM)
    val premiumPresets = Preset.entries.toSet() - basicPresets
    fun batchLimit(entitlement: EntitlementState) = if (entitlement == EntitlementState.Premium) 20 else 2
    fun allowsPreset(entitlement: EntitlementState, preset: Preset) = entitlement == EntitlementState.Premium || preset in basicPresets
    fun check(entitlement: EntitlementState, count: Int, preset: Preset = Preset.CUSTOM) {
        if (count !in 1..batchLimit(entitlement)) throw AccessDenied(Restriction.BATCH_LIMIT)
        if (!allowsPreset(entitlement, preset)) throw AccessDenied(Restriction.PREMIUM_PRESET)
    }
}
