// app/src/main/java/in/merakisan/app/core/config/FeatureManager.kt
package `in`.merakisan.app.core.config

import android.content.Context
import android.content.SharedPreferences
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.network.model.AppConfigResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * MERA KISAN Remote Feature Control Center
 * Admin Panel से फीचर्स को बिना APK री-रिलीज़ के ON/OFF करने का केंद्रीय प्रबंधक
 */
object FeatureManager {

    private const val PREFS_NAME = "mera_kisan_features"

    // डिफ़ॉल्ट रूप से संवेदनशील या आगामी फीचर्स OFF रहेंगे
    private val DEFAULT_FLAGS = mapOf(
        "payment" to false,
        "crop_boost" to false,
        "online_order" to false,
        "farmer_rating" to false,
        "buyer_rating" to false,
        "chat" to false,
        "notifications" to true,
        "delivery" to false,
        "escrow" to false,
        "auction" to false,
        "offers" to false,
        "favorites" to true,
        "compare" to false,
        "verified_farmer" to true,
        "organic_verified" to false,
        "mandi_price" to false,
        "location_search" to true,
        "map_view" to false,
        "referral" to false,
        "wallet" to false,
        "subscription" to false,
        "premium_farmer" to false,
        "analytics" to true,
        "admob" to false,
        "banner_enabled" to true
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isEnabled(context: Context, featureKey: String): Boolean {
        val defaultVal = DEFAULT_FLAGS[featureKey] ?: false
        return getPrefs(context).getBoolean(featureKey, defaultVal)
    }

    fun setFeature(context: Context, featureKey: String, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(featureKey, enabled).apply()
    }

    /**
     * Vercel Backend से रिमोट कॉन्फ़िग लोड करके स्थानीय रूप से अपडेट करना
     */
    fun syncWithBackend(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiService = ApiClient.getApiService(context)
                val response = apiService.getAppConfig()
                if (response.isSuccessful && response.body()?.success == true) {
                    val config: AppConfigResponse? = response.body()?.data
                    config?.featureFlags?.forEach { (key, value) ->
                        if (value is Boolean) {
                            setFeature(context, key, value)
                        }
                    }
                }
            } catch (_: Exception) {
                // ऑफ़लाइन होने पर डिफ़ॉल्ट सुरक्षित स्थानीय फ्लैग्स पर काम जारी रखें
            }
        }
    }
}
