// app/src/main/java/in/merakisan/app/core/config/RemoteConfigRepository.kt
package `in`.merakisan.app.core.config

import android.content.Context
import android.content.SharedPreferences
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.network.model.AppConfigDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RemoteConfigRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("mera_kisan_remote_config", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_MAINTENANCE_MODE = "cfg_maintenance_mode"
        private const val KEY_MIN_VERSION = "cfg_min_version"
        private const val KEY_BANNER_TEXT = "cfg_banner_text"
        private const val KEY_LAST_SYNC_TIME = "cfg_last_sync_timestamp"
        private const val PREFIX_FEATURE = "feat_"
    }

    suspend fun fetchAndCacheConfig(): Result<AppConfigDto> = withContext(Dispatchers.IO) {
        try {
            val apiService = ApiClient.getApiService(context)
            val response = apiService.getRemoteConfig()

            if (response.isSuccessful && response.body()?.success == true) {
                val config = response.body()?.data ?: return@withContext Result.failure(
                    Exception("Remote config data empty")
                )

                val editor = prefs.edit()
                editor.putBoolean(KEY_MAINTENANCE_MODE, config.maintenanceMode)
                editor.putString(KEY_MIN_VERSION, (config.minAppVersion as String?) ?: "1.0.0")
                editor.putString(KEY_BANNER_TEXT, config.bannerText ?: "")
                editor.putLong(KEY_LAST_SYNC_TIME, System.currentTimeMillis())

                val safeFeatures: Map<String, Any>? = config.features
                safeFeatures?.forEach { (rawKey, value) ->
                    val key = rawKey.trim().lowercase()
                    val isCore = FeatureManager.isCoreFeatureDefaultOn(key)

                    val isEnabled = when (value) {
                        is Boolean -> value
                        is String -> value.equals("ON", ignoreCase = true) ||
                                value.equals("ENABLED", ignoreCase = true) ||
                                value.equals("BETA", ignoreCase = true) ||
                                value.equals("true", ignoreCase = true)
                        is Number -> value.toInt() > 0
                        is Map<*, *> -> {
                            val killSwitch = value["kill_switch"]?.toString()?.equals("true", ignoreCase = true) == true
                            if (killSwitch) {
                                false
                            } else {
                                val status = value["status"]?.toString() ?: ""
                                val enabledFlag = value["enabled"]?.toString() ?: ""
                                status.equals("ON", ignoreCase = true) ||
                                        status.equals("ENABLED", ignoreCase = true) ||
                                        status.equals("BETA", ignoreCase = true) ||
                                        enabledFlag.equals("true", ignoreCase = true) ||
                                        isCore
                            }
                        }
                        else -> isCore
                    }
                    editor.putBoolean("$PREFIX_FEATURE$key", isEnabled)
                }

                editor.apply()
                Result.success(config)
            } else {
                val errorMsg = response.body()?.error?.message ?: response.message()
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Throwable) {
            Result.failure(Exception(e.localizedMessage ?: "Config sync error"))
        }
    }

    fun isFeatureActive(featureKey: String, defaultState: Boolean = false): Boolean {
        if (prefs.getBoolean(KEY_MAINTENANCE_MODE, false)) {
            return false
        }
        val key = featureKey.trim().lowercase()
        val isCore = FeatureManager.isCoreFeatureDefaultOn(key)
        val effectiveDefault = defaultState || isCore

        if (!prefs.contains("$PREFIX_FEATURE$key")) {
            return effectiveDefault
        }
        val storedValue = prefs.getBoolean("$PREFIX_FEATURE$key", effectiveDefault)
        return if (isCore) true else storedValue
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
