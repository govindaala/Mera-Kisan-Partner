package `in`.merakisan.app.core.config

import android.content.Context
import android.content.SharedPreferences
import `in`.merakisan.app.core.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FeatureManager {

    private const val PREF_NAME = "mera_kisan_features"
    private const val KEY_MAINTENANCE = "sys_maintenance_mode"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * बैकएंड /v1/config से रिमोट सेटिंग्स सिंक करता है
     */
    suspend fun syncRemoteConfig(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val apiService = ApiClient.getApiService(context)
            val response = apiService.getRemoteConfig()

            if (response.isSuccessful && response.body()?.success == true) {
                val config = response.body()?.data ?: return@withContext false
                val editor = getPrefs(context).edit()

                editor.putBoolean(KEY_MAINTENANCE, config.maintenanceMode)

                // 40 फ़ीचर्स की कैनोनिकल मैपिंग
                config.features.forEach { (key, value) ->
                    val isEnabled = when (value) {
                        is Boolean -> value
                        is String -> value.equals("ON", ignoreCase = true) || value.equals("BETA", ignoreCase = true)
                        is Number -> value.toInt() > 0
                        else -> false
                    }
                    editor.putBoolean("feat_$key", isEnabled)
                }

                editor.apply()
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun isFeatureEnabled(context: Context, featureKey: String, defaultValue: Boolean = false): Boolean {
        val prefs = getPrefs(context)
        if (prefs.getBoolean(KEY_MAINTENANCE, false)) {
            return false // मेंटेनेंस मोड सक्रिय होने पर सभी गैर-ज़रूरी फ़ीचर्स बंद
        }
        return prefs.getBoolean("feat_$featureKey", defaultValue)
    }

    fun isMaintenanceMode(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_MAINTENANCE, false)
    }
}
