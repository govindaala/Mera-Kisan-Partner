// app/src/main/java/in/merakisan/app/core/config/FeatureManager.kt
package `in`.merakisan.app.core.config

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object FeatureManager {

    private const val PREF_CONFIG = "mera_kisan_config"
    private val memoryFlags = mutableMapOf<String, Boolean>()
    private var maintenanceActive = false
    private var maintenanceMessage = ""

    // Master Spec Sec 17: AdMob is strictly OFF by default
    private val defaultStates = mapOf(
        "admob" to false,
        "mandi_price" to true,
        "mandi_prices" to true,
        "buyer_request" to true,
        "demand_board" to true,
        "make_offer" to true,
        "small_quantity" to true,
        "crop_boost" to true,
        "ai_poster" to true,
        "oil_marketplace" to true,
        "farm_box" to true,
        "batch_traceability" to true
    )

    suspend fun syncRemoteConfig(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://mera-kisan-backend.vercel.app/api/v1/config")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                if (conn.responseCode in 200..299) {
                    val raw = conn.inputStream.bufferedReader().use { it.readText() }
                    val root = JSONObject(raw)
                    val data = root.optJSONObject("data") ?: root

                    maintenanceActive = data.optBoolean("maintenance_mode", false)
                    maintenanceMessage = data.optString("banner_text", "")

                    val prefs = context.getSharedPreferences(PREF_CONFIG, Context.MODE_PRIVATE)
                    val editor = prefs.edit()
                    editor.putBoolean("maintenance_mode", maintenanceActive)
                    editor.putString("maintenance_message", maintenanceMessage)

                    val featuresObj = data.optJSONObject("features")
                    if (featuresObj != null) {
                        val keys = featuresObj.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            val v = featuresObj.opt(k)
                            val enabled = (v == true || v == "ON" || v == "BETA")
                            memoryFlags[k] = enabled
                            editor.putBoolean(k, enabled)
                        }
                    }
                    editor.apply()
                }
            } catch (_: Throwable) {}
        }
    }

    fun isFeatureEnabled(key: String): Boolean {
        if (key.equals("admob", ignoreCase = true)) {
            return memoryFlags["admob"] ?: false
        }
        return memoryFlags[key] ?: defaultStates[key] ?: true
    }

    fun isEnabled(key: String): Boolean = isFeatureEnabled(key)

    fun isEnabled(context: Context, key: String): Boolean {
        val prefs = context.getSharedPreferences(PREF_CONFIG, Context.MODE_PRIVATE)
        if (prefs.contains(key)) {
            return prefs.getBoolean(key, true)
        }
        return isFeatureEnabled(key)
    }

    fun isMaintenanceMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_CONFIG, Context.MODE_PRIVATE)
        return prefs.getBoolean("maintenance_mode", maintenanceActive)
    }

    fun getMaintenanceMessage(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_CONFIG, Context.MODE_PRIVATE)
        return prefs.getString("maintenance_message", maintenanceMessage) ?: ""
    }
}
