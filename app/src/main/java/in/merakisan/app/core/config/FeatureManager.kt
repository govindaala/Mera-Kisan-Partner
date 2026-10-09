// app/src/main/java/in/merakisan/app/core/config/FeatureManager.kt
package in.merakisan.app.core.config

import android.content.Context
import android.content.SharedPreferences
import in.merakisan.app.core.network.ApiClient
import in.merakisan.app.core.network.model.AppConfigResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MERA KISAN Remote Feature Control Engine
 * 40 मास्टर फ़ीचर्स और मेंटेनेंस मोड को क्लाइंट साइड पर प्रबंधित करने वाला सिंगलटन
 */
object FeatureManager {

    private const val PREFS_NAME = "mera_kisan_feature_flags"
    private const val KEY_MAINTENANCE_ACTIVE = "maintenance_active"
    private const val KEY_MAINTENANCE_MSG = "maintenance_message"
    private const val KEY_MIN_VERSION = "min_app_version"

    // डिफ़ॉल्ट फ़ीचर बेसलाइन (यदि नेटवर्क उपलब्ध न हो)
    private val DEFAULT_FLAGS = mapOf(
        "small_quantity" to true,
        "whatsapp" to true,
        "call" to true,
        "distance_filter" to true,
        "online_order" to false,
        "make_offer" to false,
        "buy_now" to false,
        "crop_boost" to false,
        "admob" to false,
        "chat" to false,
        "auction" to false,
        "wallet" to false,
        "escrow" to false
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * बैकएंड (/v1/config) से नवीनतम 40 फ़्लैग्स फेच और स्टोर करता है
     */
    suspend fun syncRemoteConfig(context: Context): Result<AppConfigResponse> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.getApiService(context).getAppConfig()
            if (response.isSuccessful && response.body()?.success == true) {
                val config = response.body()?.data
                if (config != null) {
                    saveConfigToCache(context, config)
                    return@withContext Result.success(config)
                }
            }
            Result.failure(Exception(response.body()?.error?.message ?: "कॉन्फ़िगरेशन लोड करने में विफलता"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveConfigToCache(context: Context, config: AppConfigResponse) {
        val editor = getPrefs(context).edit()
        editor.putBoolean(KEY_MAINTENANCE_ACTIVE, config.maintenanceMode.active)
        editor.putString(KEY_MAINTENANCE_MSG, config.maintenanceMode.message ?: "")
        editor.putString(KEY_MIN_VERSION, config.minAppVersion)

        // 40 फ़ीचर्स को लोकली कैश करना
        for ((featureId, isEnabled) in config.features) {
            editor.putBoolean("feat_$featureId", isEnabled)
        }
        editor.apply()
    }

    /**
     * जाँच करता है कि कोई फ़ीचर सक्षम है या नहीं (OFF / BETA / ON)
     */
    fun isEnabled(context: Context, featureId: String): Boolean {
        val prefs = getPrefs(context)
        val key = "feat_$featureId"
        if (prefs.contains(key)) {
            return prefs.getBoolean(key, false)
        }
        return DEFAULT_FLAGS[featureId] ?: false
    }

    fun isMaintenanceMode(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_MAINTENANCE_ACTIVE, false)
    }

    fun getMaintenanceMessage(context: Context): String {
        return getPrefs(context).getString(KEY_MAINTENANCE_MSG, "रखरखाव जारी है। कृपया कुछ समय बाद पुनः प्रयास करें।")
            ?: "रखरखाव जारी है।"
    }

    fun getMinAppVersion(context: Context): String {
        return getPrefs(context).getString(KEY_MIN_VERSION, "1.0.0") ?: "1.0.0"
    }
}
