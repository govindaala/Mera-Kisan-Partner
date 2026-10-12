// app/src/main/java/in/merakisan/app/core/config/FeatureManager.kt
package `in`.merakisan.app.core.config

import android.content.Context
import android.content.SharedPreferences

object FeatureManager {
    private const val PREFS_NAME = "mera_kisan_features"

    private val DEFAULT_FLAGS: Map<String, Boolean> = mapOf(
        "payment" to false,
        "crop_boost" to false,
        "online_order" to true,
        "offers" to false,
        "mandi_price" to true,
        "buyer_request" to true,
        "admob" to false,
        "chat" to false,
        "banner_enabled" to true,
        "oil_marketplace" to false
    )

    fun isEnabled(context: Context, featureKey: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(featureKey, DEFAULT_FLAGS[featureKey] ?: false)
    }

    fun setFeature(context: Context, key: String, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(key, enabled).apply()
    }

    suspend fun syncRemoteConfig(context: Context): Boolean {
        return true
    }

    fun isMaintenanceMode(context: Context): Boolean = false

    fun getMaintenanceMessage(context: Context): String = "सिस्टम मेंटेनेंस जारी है।"
}
