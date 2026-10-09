// app/src/main/java/in/merakisan/app/core/security/SessionManager.kt
package in.merakisan.app.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * MERA KISAN Security Engine
 * Android Keystore आधारित एन्क्रिप्टेड स्टोरेज (Zero plaintext credentials)
 */
object SessionManager {

    private const val PREF_FILE_NAME = "mera_kisan_secure_prefs"
    private const val KEY_AUTH_TOKEN = "auth_token"
    private const val KEY_USER_UID = "user_uid"
    private const val KEY_USER_ROLE = "user_role" // FARMER, BUYER, BOTH

    private fun getEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREF_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveSession(context: Context, token: String, uid: String, role: String) {
        getEncryptedPrefs(context).edit()
            .putString(KEY_AUTH_TOKEN, token)
            .putString(KEY_USER_UID, uid)
            .putString(KEY_USER_ROLE, role)
            .apply()
    }

    fun getAuthToken(context: Context): String? {
        return getEncryptedPrefs(context).getString(KEY_AUTH_TOKEN, null)
    }

    fun getUserUid(context: Context): String? {
        return getEncryptedPrefs(context).getString(KEY_USER_UID, null)
    }

    fun getUserRole(context: Context): String {
        return getEncryptedPrefs(context).getString(KEY_USER_ROLE, "BUYER") ?: "BUYER"
    }

    fun clearSession(context: Context) {
        getEncryptedPrefs(context).edit().clear().apply()
    }
}
