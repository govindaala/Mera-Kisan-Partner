// app/src/main/java/in/merakisan/app/core/config/FeatureManager.kt
package `in`.merakisan.app.core.config

import android.content.Context

object FeatureManager {

    suspend fun syncRemoteConfig(context: Context): Boolean {
        return try {
            val repository = RemoteConfigRepository(context.applicationContext)
            val result = repository.fetchAndCacheConfig()
            result.isSuccess
        } catch (_: Exception) {
            false
        }
    }

    fun isFeatureEnabled(
        context: Context,
        featureKey: String,
        defaultValue: Boolean = false
    ): Boolean {
        return try {
            RemoteConfigRepository(context.applicationContext)
                .isFeatureActive(featureKey, defaultValue)
        } catch (_: Exception) {
            defaultValue
        }
    }

    // पुराने और नए दोनों तरह के कॉल सपोर्ट करने के लिए
    fun isEnabled(
        context: Context,
        featureKey: String,
        defaultValue: Boolean = false
    ): Boolean {
        return isFeatureEnabled(context, featureKey, defaultValue)
    }

    fun isMaintenanceMode(context: Context): Boolean {
        return try {
            RemoteConfigRepository(context.applicationContext)
                .isMaintenanceMode()
        } catch (_: Exception) {
            false
        }
    }

    fun getMaintenanceMessage(context: Context): String {
        return try {
            RemoteConfigRepository(context.applicationContext)
                .getBannerText()
                ?.takeIf { it.isNotBlank() }
                ?: "ऐप पर रखरखाव का कार्य चल रहा है। कृपया बाद में पुनः प्रयास करें।"
        } catch (_: Exception) {
            "ऐप पर रखरखाव का कार्य चल रहा है। कृपया बाद में पुनः प्रयास करें।"
        }
    }
}
