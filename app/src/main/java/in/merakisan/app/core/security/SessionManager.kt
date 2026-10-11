// app/src/main/java/in/merakisan/app/core/security/SessionManager.kt
package `in`.merakisan.app.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.gson.Gson
import `in`.merakisan.app.core.network.model.UserDto

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "mera_kisan_secure_session",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (_: Exception) {
        context.getSharedPreferences("mera_kisan_secure_fallback", Context.MODE_PRIVATE)
    }

    private val gson = Gson()

    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_SESSION = "user_session"
        private const val KEY_SELECTED_ROLE = "selected_role"
    }

    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun saveUserSession(user: UserDto) {
        val json = gson.toJson(user)
        prefs.edit().putString(KEY_USER_SESSION, json).apply()
    }

    fun getUserSession(): UserDto? {
        val json = prefs.getString(KEY_USER_SESSION, null) ?: return null
        return try {
            gson.fromJson(json, UserDto::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun saveSelectedRole(role: String) {
        prefs.edit().putString(KEY_SELECTED_ROLE, role).apply()
    }

    fun getSelectedRole(): String {
        return prefs.getString(KEY_SELECTED_ROLE, "both") ?: "both"
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
