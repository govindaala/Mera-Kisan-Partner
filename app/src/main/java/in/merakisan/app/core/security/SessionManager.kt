// app/src/main/java/in/merakisan/app/core/security/SessionManager.kt
package `in`.merakisan.app.core.security

import android.content.Context
import android.content.SharedPreferences

/**
 * MERA KISAN Central Session Manager
 */
object SessionManager {

    private const val PREF_NAME = "mera_kisan_secure_session"
    private const val KEY_AUTH_TOKEN = "auth_token"
    private const val KEY_USER_UID = "user_uid"
    private const val KEY_USER_PHONE = "user_phone"
    private const val KEY_USER_ROLE = "user_role"
    private const val KEY_FCM_TOKEN = "fcm_token"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveSession(context: Context, token: String, uid: String, role: String, phone: String? = null) {
        getPrefs(context).edit().apply {
            putString(KEY_AUTH_TOKEN, token)
            putString(KEY_USER_UID, uid)
            putString(KEY_USER_ROLE, role)
            if (phone != null) putString(KEY_USER_PHONE, phone)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    fun saveAuthToken(context: Context, token: String) {
        getPrefs(context).edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    fun getAuthToken(context: Context): String? {
        return getPrefs(context).getString(KEY_AUTH_TOKEN, null)
    }

    fun saveUserSession(context: Context, uid: String, phone: String?, role: String) {
        saveSession(context, getAuthToken(context) ?: "", uid, role, phone)
    }

    fun getUserUid(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_UID, null)
    }

    fun getUserPhone(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_PHONE, null)
    }

    fun getUserRole(context: Context): String {
        return getPrefs(context).getString(KEY_USER_ROLE, "BUYER") ?: "BUYER"
    }

    fun setUserRole(context: Context, role: String) {
        getPrefs(context).edit().putString(KEY_USER_ROLE, role).apply()
    }

    fun saveFcmToken(context: Context, token: String) {
        getPrefs(context).edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    fun getFcmToken(context: Context): String? {
        return getPrefs(context).getString(KEY_FCM_TOKEN, null)
    }

    fun isLoggedIn(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun clearSession(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
