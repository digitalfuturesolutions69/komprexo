package com.komprexo.app.access

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*

class PreferencesQuotaStore(private val dataStore: DataStore<Preferences>) : QuotaStore {
    private val day = longPreferencesKey("day")
    private val wall = longPreferencesKey("clock_high_water")
    private val used = intPreferencesKey("used")
    private val id = stringPreferencesKey("reservation")
    private val reserved = intPreferencesKey("reserved")
    private val settled = stringSetPreferencesKey("settled_slots")
    override suspend fun update(change: (QuotaLedger) -> QuotaLedger): QuotaLedger {
        var result: QuotaLedger? = null
        dataStore.edit { p ->
            val next = change(QuotaLedger(p[day] ?: Long.MIN_VALUE, p[wall] ?: Long.MIN_VALUE,
                p[used] ?: 0, p[id] ?: "", p[reserved] ?: 0, p[settled] ?: emptySet()))
            p[day] = next.day; p[wall] = next.highWaterMillis; p[used] = next.used
            p[id] = next.reservation; p[reserved] = next.reserved; p[settled] = next.settled
            result = next
        }
        return checkNotNull(result)
    }
}
