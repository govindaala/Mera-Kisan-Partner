// app/src/main/java/in/merakisan/app/core/config/RemoteConfigRepository.kt
package `in`.merakisan.app.core.config

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class RemoteConfigRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("mera_kisan_config", Context.MODE_PRIVATE)

    suspend fun fetchAndCacheConfig(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                FeatureManager.syncRemoteConfig(context)
                val url = URL("https://mera-kisan-backend.vercel.app/api/v1/config")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                if (conn.responseCode in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val data = json.optJSONObject("data") ?: json
                    val features = data.optJSONObject("features")

                    if (features != null) {
                        val editor = prefs.edit()
                        val keys = features.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val cleanKey = key.trim().lowercase(Locale.ROOT)
                            val defaultOn = FeatureManager.isCoreFeatureDefaultOn(cleanKey)
                            val rawVal = features.opt(key)
                            val enabled = when (rawVal) {
                                is Boolean -> rawVal
                                is String -> rawVal.equals("ON", true) || rawVal.equals("BETA", true) || rawVal.equals("true", true)
                                is JSONObject -> !rawVal.optBoolean("kill_switch", false) &&
                                    (rawVal.optBoolean("enabled", defaultOn) || rawVal.optString("status").equals("ON", true))
                                else -> defaultOn
                            }
                            editor.putBoolean(cleanKey, enabled)
                        }
                        editor.apply()
                    }
                    true
                } else {
                    false
                }
            } catch (_: Throwable) {
                false
            }
        }
    }

    fun isFeatureEnabled(featureKey: String): Boolean {
        val cleanKey = featureKey.trim().lowercase(Locale.ROOT)
        val defaultOn = FeatureManager.isCoreFeatureDefaultOn(cleanKey)
        return prefs.getBoolean(cleanKey, defaultOn)
    }
}
