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
 * Admin Panel से फीचर्स व मेंटेनेंस को दूरस्थ रूप से नियंत्रित करने का केंद्रीय प्रबंधक
 */
object FeatureManager {

    private const val PREFS_NAME = "mera_kisan_features"
    private const val KEY_MAINTENANCE_MODE = "maintenance_mode"
    private const val KEY_MAINTENANCE_MSG = "maintenance_message"

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
        "banner_enabled" to true,
        "maintenance_mode" to false
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

    // SplashFragment के लिए मेंटेनेंस मोड विधियाँ
    fun isMaintenanceMode(context: Context? = null): Boolean {
        return if (context != null) {
            getPrefs(context).getBoolean(KEY_MAINTENANCE_MODE, false)
        } else false
    }

    fun getMaintenanceMessage(context: Context? = null): String {
        return if (context != null) {
            getPrefs(context).getString(KEY_MAINTENANCE_MSG, null)
                ?: "MERA KISAN पर रखरखाव (Maintenance) चल रहा है। कृपया कुछ समय बाद पुनः प्रयास करें।"
        } else {
            "MERA KISAN पर रखरखाव चल रहा है।"
        }
    }

    fun setMaintenanceMode(context: Context, isMaintenance: Boolean, message: String? = null) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_MAINTENANCE_MODE, isMaintenance)
            if (message != null) putString(KEY_MAINTENANCE_MSG, message)
            apply()
        }
    }

    // SplashFragment व अन्य कॉलर हेतु रिमोट सिंक
    fun syncRemoteConfig(context: Context, onComplete: (() -> Unit)? = null) {
        syncWithBackend(context, onComplete)
    }

    fun syncWithBackend(context: Context, onComplete: (() -> Unit)? = null) {
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
                // ऑफ़लाइन होने पर डिफ़ॉल्ट स्थानीय कॉन्फ़िग सुरक्षित रखें
            } finally {
                onComplete?.invoke()
            }
        }
    }
}
