// app/src/main/java/in/merakisan/app/core/config/FeatureManager.kt
package `in`.merakisan.app.core.config

import android.content.Context

object FeatureManager {

    suspend fun syncRemoteConfig(context: Context): Boolean {
        val repo = RemoteConfigRepository(context)
        val result = repo.fetchAndCacheConfig()
        return result.isSuccess
    }

    fun isFeatureEnabled(context: Context, featureKey: String, defaultValue: Boolean = false): Boolean {
        val repo = RemoteConfigRepository(context)
        return repo.isFeatureActive(featureKey, defaultValue)
    }

    fun isMaintenanceMode(context: Context): Boolean {
        val repo = RemoteConfigRepository(context)
        return repo.isMaintenanceMode()
    }
}
