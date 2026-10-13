// app/src/main/java/in/merakisan/app/core/config/FeatureManager.kt
package `in`.merakisan.app.core.config

import android.content.Context

object FeatureManager {

    suspend fun syncRemoteConfig(
        context: Context
    ): Boolean {
        val repository = RemoteConfigRepository(context)
        val result = repository.fetchAndCacheConfig()

        return result.isSuccess
    }

    fun isEnabled(
        context: Context,
        featureKey: String,
        defaultValue: Boolean = false
    ): Boolean {
        return isFeatureEnabled(
            context,
            featureKey,
            defaultValue
        )
    }

    fun isFeatureEnabled(
        context: Context,
        featureKey: String,
        defaultValue: Boolean = false
    ): Boolean {
        val repository = RemoteConfigRepository(context)

        return repository.isFeatureActive(
            featureKey,
            defaultValue
        )
    }

    fun isMaintenanceMode(
        context: Context
    ): Boolean {
        val repository = RemoteConfigRepository(context)

        return repository.isMaintenanceMode()
    }
}
