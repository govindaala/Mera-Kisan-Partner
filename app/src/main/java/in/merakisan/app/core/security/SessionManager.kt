// app/src/main/java/in/merakisan/app/core/security/SessionManager.kt
package `in`.merakisan.app.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject

/**
 * MERA KISAN — ENCRYPTED SESSION & ROLE STATE MANAGER
 * सुरक्षा मानक: कोई भी प्लेनटेक्स्ट पिन कभी सेव नहीं किया जाएगा (Rule 7 & merged.txt Sec 5)।
 */
object SessionManager {

    private const val ENCRYPTED_PREFS_FILE = "mera_kisan_secure_session_v2"
    private const val FALLBACK_PREFS_FILE = "mera_kisan_session_fallback_v2"

    private const val KEY_UID = "user_uid"
    private const val KEY_AUTH_TOKEN = "auth_id_token"
    private const val KEY_NAME = "user_name"
    private const val KEY_PHONE = "self_declared_phone"
    private const val KEY_EMAIL = "user_email"
    private const val KEY_ROLES_CSV = "user_roles_csv"
    private const val KEY_ACTIVE_MODE = "active_role_mode" // "FARMER" or "BUYER"
    private const val KEY_ONBOARDING_DONE = "onboarding_completed"
    private const val KEY_VILLAGE = "user_village"
    private const val KEY_DISTRICT = "user_district"
    private const val KEY_STATE = "user_state"
    private const val KEY_VERIFICATION_STATUS = "verification_status"

    // अधिकृत कृषि दवाई विक्रेता (Dealer) सत्र
    private const val KEY_DEALER_TOKEN = "agri_dealer_session_token"
    private const val KEY_DEALER_SHOP_NAME = "agri_dealer_shop_name"
    private const val KEY_DEALER_LOGIN_ID = "agri_dealer_login_id"

    // बार-बार खोजी गई मंडी का लोकल मोबाइल कैश (केवल रीड कैश, 0% डमी डेटा)
    private const val KEY_FREQUENT_MANDI_STATE = "frequent_mandi_state"
    private const val KEY_FREQUENT_MANDI_DISTRICT = "frequent_mandi_district"
    private const val KEY_FREQUENT_MANDI_CACHE_JSON = "frequent_mandi_rates_json"
    private const val KEY_FREQUENT_MANDI_CACHE_TIME = "frequent_mandi_rates_time"

    @Volatile
    private var cachedPrefs: SharedPreferences? = null

    private fun getSecurePrefs(context: Context): SharedPreferences {
        cachedPrefs?.let { return it }
        synchronized(this) {
            cachedPrefs?.let { return it }
            val appCtx = context.applicationContext
            val prefs = try {
                val masterKey = MasterKey.Builder(appCtx)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    appCtx,
                    ENCRYPTED_PREFS_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (_: Exception) {
                appCtx.getSharedPreferences(FALLBACK_PREFS_FILE, Context.MODE_PRIVATE)
            }
            cachedPrefs = prefs
            return prefs
        }
    }

    fun saveAuthToken(context: Context, token: String) {
        if (token.isBlank()) return
        getSecurePrefs(context).edit().putString(KEY_AUTH_TOKEN, token.trim()).apply()
    }

    fun getAuthToken(context: Context): String? =
        getSecurePrefs(context).getString(KEY_AUTH_TOKEN, null)

    fun saveOnboardedUserSession(
        context: Context,
        uid: String,
        name: String,
        phone: String,
        roles: List<String>,
        activeMode: String,
        village: String? = null,
        district: String? = null,
        state: String? = null,
        verificationStatus: String = "farmer_declared"
    ) {
        val cleanRoles = roles.map { it.trim().lowercase() }.filter { it in listOf("farmer", "buyer") }.distinct()
        val resolvedMode = when (activeMode.trim().uppercase()) {
            "BUYER" -> "BUYER"
            "FARMER", "SELLER" -> "FARMER"
            else -> if (cleanRoles.contains("farmer")) "FARMER" else "BUYER"
        }

        getSecurePrefs(context).edit().apply {
            putString(KEY_UID, uid.trim())
            putString(KEY_NAME, name.trim())
            putString(KEY_PHONE, phone.trim())
            putString(KEY_ROLES_CSV, cleanRoles.joinToString(","))
            putString(KEY_ACTIVE_MODE, resolvedMode)
            putBoolean(KEY_ONBOARDING_DONE, true)
            if (!village.isNullOrBlank()) putString(KEY_VILLAGE, village.trim())
            if (!district.isNullOrBlank()) putString(KEY_DISTRICT, district.trim())
            if (!state.isNullOrBlank()) putString(KEY_STATE, state.trim())
            putString(KEY_VERIFICATION_STATUS, verificationStatus.trim().lowercase())
            apply()
        }
    }

    fun isLoggedInAndOnboarded(context: Context): Boolean {
        val prefs = getSecurePrefs(context)
        val uid = prefs.getString(KEY_UID, null)
        val phone = prefs.getString(KEY_PHONE, null)
        val onboarded = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        return !uid.isNullOrBlank() && !phone.isNullOrBlank() && onboarded
    }

    fun getUserUid(context: Context): String? =
        getSecurePrefs(context).getString(KEY_UID, null)

    fun getUserName(context: Context): String =
        getSecurePrefs(context).getString(KEY_NAME, "") ?: ""

    fun getSelfDeclaredPhone(context: Context): String =
        getSecurePrefs(context).getString(KEY_PHONE, "") ?: ""

    fun getUserVillage(context: Context): String =
        getSecurePrefs(context).getString(KEY_VILLAGE, "") ?: ""

    fun getUserDistrict(context: Context): String =
        getSecurePrefs(context).getString(KEY_DISTRICT, "") ?: ""

    fun getUserState(context: Context): String =
        getSecurePrefs(context).getString(KEY_STATE, "") ?: ""

    fun getVerificationStatus(context: Context): String =
        getSecurePrefs(context).getString(KEY_VERIFICATION_STATUS, "farmer_declared") ?: "farmer_declared"

    fun getAllowedRoles(context: Context): List<String> {
        val csv = getSecurePrefs(context).getString(KEY_ROLES_CSV, "") ?: ""
        return csv.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
    }

    /**
     * वर्तमान सक्रिय मोड: हमेशा स्पष्ट रूप से "FARMER" या "BUYER" लौटाता है।
     * नियम: किसान मोड में खरीदार के विकल्प नहीं दिखेंगे, और खरीदार मोड में किसान के विकल्प नहीं दिखेंगे।
     */
    fun getActiveMode(context: Context): String {
        val saved = getSecurePrefs(context).getString(KEY_ACTIVE_MODE, "FARMER") ?: "FARMER"
        return if (saved.uppercase() == "BUYER") "BUYER" else "FARMER"
    }

    fun isFarmerModeActive(context: Context): Boolean =
        getActiveMode(context) == "FARMER"

    fun isBuyerModeActive(context: Context): Boolean =
        getActiveMode(context) == "BUYER"

    fun setActiveMode(context: Context, mode: String) {
        val resolved = if (mode.trim().uppercase() == "BUYER") "BUYER" else "FARMER"
        val currentRoles = getAllowedRoles(context).toMutableSet()
        currentRoles.add(resolved.lowercase())
        getSecurePrefs(context).edit()
            .putString(KEY_ACTIVE_MODE, resolved)
            .putString(KEY_ROLES_CSV, currentRoles.joinToString(","))
            .apply()
    }

    // अधिकृत कृषि दवाई विक्रेता (Dealer) सत्र प्रबंधन
    fun saveDealerSession(context: Context, token: String, loginId: String, shopName: String) {
        getSecurePrefs(context).edit()
            .putString(KEY_DEALER_TOKEN, token.trim())
            .putString(KEY_DEALER_LOGIN_ID, loginId.trim())
            .putString(KEY_DEALER_SHOP_NAME, shopName.trim())
            .apply()
    }

    fun getDealerToken(context: Context): String? =
        getSecurePrefs(context).getString(KEY_DEALER_TOKEN, null)

    fun getDealerShopName(context: Context): String =
        getSecurePrefs(context).getString(KEY_DEALER_SHOP_NAME, "") ?: ""

    fun clearDealerSession(context: Context) {
        getSecurePrefs(context).edit()
            .remove(KEY_DEALER_TOKEN)
            .remove(KEY_DEALER_LOGIN_ID)
            .remove(KEY_DEALER_SHOP_NAME)
            .apply()
    }

    // बार-बार खोजी गई मंडी का लोकल कैश (केवल यूज़र के मोबाइल में, हमारे फायरबेस पर 0 MB बोझ)
    fun saveFrequentMandiCache(
        context: Context,
        state: String,
        district: String,
        ratesArray: JSONArray
    ) {
        if (ratesArray.length() == 0) return
        getSecurePrefs(context).edit()
            .putString(KEY_FREQUENT_MANDI_STATE, state)
            .putString(KEY_FREQUENT_MANDI_DISTRICT, district)
            .putString(KEY_FREQUENT_MANDI_CACHE_JSON, ratesArray.toString())
            .putLong(KEY_FREQUENT_MANDI_CACHE_TIME, System.currentTimeMillis())
            .apply()
    }

    fun getFrequentMandiCache(context: Context): JSONObject? {
        val prefs = getSecurePrefs(context)
        val rawJson = prefs.getString(KEY_FREQUENT_MANDI_CACHE_JSON, null) ?: return null
        return try {
            JSONObject().apply {
                put("state", prefs.getString(KEY_FREQUENT_MANDI_STATE, "") ?: "")
                put("district", prefs.getString(KEY_FREQUENT_MANDI_DISTRICT, "") ?: "")
                put("cached_at_ms", prefs.getLong(KEY_FREQUENT_MANDI_CACHE_TIME, 0L))
                put("rates", JSONArray(rawJson))
            }
        } catch (_: Exception) {
            null
        }
    }

    fun clearAllSession(context: Context) {
        getSecurePrefs(context).edit().clear().apply()
    }
}
