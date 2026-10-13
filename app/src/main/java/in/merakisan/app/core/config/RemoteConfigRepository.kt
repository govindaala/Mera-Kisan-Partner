package `in`.merakisan.app.core.config

import android.content.Context
import android.content.SharedPreferences
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.network.model.AppConfigDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MERA KISAN — रिमोट कॉन्फ़िगरेशन एवं फ़ीचर फ़्लैग रिपॉजिटरी
 * Master Spec v1.1 & Audit Security Standard
 */
class RemoteConfigRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("mera_kisan_remote_config", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_MAINTENANCE_MODE = "cfg_maintenance_mode"
        private const val KEY_MIN_VERSION = "cfg_min_version"
        private const val KEY_BANNER_TEXT = "cfg_banner_text"
        private const val KEY_LAST_SYNC_TIME = "cfg_last_sync_timestamp"
        private const val PREFIX_FEATURE = "feat_"
    }

    /**
     * सर्वर (/v1/config) से नवीनतम फ़ीचर फ़्लैग्स लाकर SharedPreferences में सुरक्षित करता है।
     */
    suspend fun fetchAndCacheConfig(): Result<AppConfigDto> = withContext(Dispatchers.IO) {
        try {
            val apiService = ApiClient.getApiService(context)
            val response = apiService.getRemoteConfig()

            if (response.isSuccessful && response.body()?.success == true) {
                val config = response.body()?.data ?: return@withContext Result.failure(
                    Exception("रिमोट कॉन्फ़िग डेटा रिक्त प्राप्त हुआ।")
                )

                val editor = prefs.edit()

                // 1. सिस्टम स्तर की सेटिंग्स
                editor.putBoolean(KEY_MAINTENANCE_MODE, config.maintenanceMode)
                editor.putString(KEY_MIN_VERSION, config.minAppVersion)
                editor.putString(KEY_BANNER_TEXT, config.bannerText ?: "")
                editor.putLong(KEY_LAST_SYNC_TIME, System.currentTimeMillis())

                // 2. 40 फ़ीचर्स की सुरक्षित स्थिति (ON, BETA = true; OFF, 0% = false)
                config.features.forEach { (key, value) ->
                    val isEnabled = when (value) {
                        is Boolean -> value
                        is String -> value.equals("ON", ignoreCase = true) || value.equals("BETA", ignoreCase = true)
                        is Number -> value.toInt() > 0
                        is Map<*, *> -> {
                            val status = value["status"]?.toString() ?: ""
                            status.equals("ON", ignoreCase = true) || status.equals("BETA", ignoreCase = true)
                        }
                        else -> false
                    }
                    editor.putBoolean("$PREFIX_FEATURE$key", isEnabled)
                }

                editor.apply()
                Result.success(config)
            } else {
                val errorMsg = response.body()?.error?.message ?: response.message()
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * स्थानीय रूप से कैश्ड फ़ीचर की स्थिति लौटाता है (ऑफ़लाइन सुरक्षित)।
     */
    fun isFeatureActive(featureKey: String, defaultState: Boolean = false): Boolean {
        // यदि मेंटेनेंस मोड सक्रिय है, तो केवल आवश्यक सुरक्षा फ़ीचर्स चलेंगे
        if (prefs.getBoolean(KEY_MAINTENANCE_MODE, false)) {
            return false
        }
        return prefs.getBoolean("$PREFIX_FEATURE$featureKey", defaultState)
    }

    fun isMaintenanceMode(): Boolean {
        return prefs.getBoolean(KEY_MAINTENANCE_MODE, false)
    }

    fun getBannerText(): String? {
        val banner = prefs.getString(KEY_BANNER_TEXT, null)
        return if (banner.isNullOrBlank()) null else banner
    }

    fun getMinimumRequiredVersion(): String {
        return prefs.getString(KEY_MIN_VERSION, "1.0.0") ?: "1.0.0"
    }
}
