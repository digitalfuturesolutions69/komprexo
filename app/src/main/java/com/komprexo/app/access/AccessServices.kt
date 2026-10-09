package com.komprexo.app.access

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

object AccessServices {
    private var manager: DailyQuotaManager? = null
    @Synchronized fun quota(context: Context): DailyQuotaManager {
        return manager ?: DailyQuotaManager(PreferencesQuotaStore(PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            produceFile = { File(context.applicationContext.filesDir, "quota.preferences_pb") }
        )), EntitlementProviderFactory.provider).also { manager = it }
    }
}
