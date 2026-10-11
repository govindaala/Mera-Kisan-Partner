// app/src/main/java/in/merakisan/app/core/config/FeatureManager.kt
package `in`.merakisan.app.core.config

import android.content.Context
import android.content.SharedPreferences
import `in`.merakisan.app.core.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FeatureManager {

    private const val PREF_NAME = "mera_kisan_features"
    private const val KEY_LAST_SYNC = "last_sync_timestamp"
    private const val TTL_MS = 5 * 60 * 1000L // 5 मिनट कैशे

    private val DEFAULT_FLAGS = mapOf(
        "payment" to false,
        "crop_boost" to false,
        "online_order" to true,
        "offers" to false,
        "mandi_price" to true,
        "buyer_request" to true,
        "admob" to false,
        "chat" to false,
        "oil_marketplace" to false
    )

    fun isEnabled(context: Context, featureKey: String): Boolean {
        val prefs = getPrefs(context)
        return prefs.getBoolean(featureKey, DEFAULT_FLAGS[featureKey] ?: false)
    }

    suspend fun syncRemoteConfig(context: Context) {
        val prefs = getPrefs(context)
        val lastSync = prefs.getLong(KEY_LAST_SYNC, 0L)
        if (System.currentTimeMillis() - lastSync < TTL_MS) return

        withContext(Dispatchers.IO) {
            try {
                val apiService = ApiClient.getApiService(context)
                val response = apiService.getPublicConfig()
                if (response.isSuccessful && response.body()?.success == true) {
                    val flags = response.body()?.data?.featureFlags ?: emptyMap()
                    val editor = prefs.edit()
                    for ((k, v) in flags) {
                        editor.putBoolean(k, v)
                    }
                    editor.putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                    editor.apply()
                }
            } catch (_: Exception) {
                // ऑफ़लाइन होने पर स्थानीय कैशे ही काम करेगा
            }
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
}
