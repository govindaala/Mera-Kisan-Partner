package `in`.merakisan.app.core.state

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object RoleStateManager {

    const val ROLE_FARMER = "FARMER"
    const val ROLE_BUYER = "BUYER"
    const val ROLE_BOTH = "BOTH"

    private const val PREF_USER = "mera_kisan_user"
    private const val PREF_SESSION = "mera_kisan_session"
    private const val KEY_ROLE = "user_role"

    private val _roleFlow = MutableStateFlow(ROLE_BOTH)
    val roleFlow: StateFlow<String> = _roleFlow.asStateFlow()

    fun init(context: Context) {
        val savedRole = getPersistedRole(context)
        _roleFlow.value = normalizeRole(savedRole)
    }

    fun getCurrentRole(): String = _roleFlow.value

    fun setRole(context: Context, newRole: String) {
        val normalized = normalizeRole(newRole)
        _roleFlow.value = normalized

        try {
            val userPrefs = context.getSharedPreferences(PREF_USER, Context.MODE_PRIVATE)
            userPrefs.edit()
                .putString(KEY_ROLE, normalized)
                .putString("role", normalized)
                .apply()

            val sessionPrefs = context.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE)
            sessionPrefs.edit()
                .putString(KEY_ROLE, normalized)
                .apply()

            val defaultPrefs = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
            defaultPrefs.edit()
                .putString(KEY_ROLE, normalized)
                .apply()
        } catch (_: Throwable) {}
    }

    fun isFarmer(): Boolean {
        val current = _roleFlow.value
        return current == ROLE_FARMER || current == ROLE_BOTH
    }

    fun isBuyer(): Boolean {
        val current = _roleFlow.value
        return current == ROLE_BUYER || current == ROLE_BOTH
    }

    fun isDualRole(): Boolean = _roleFlow.value == ROLE_BOTH

    private fun getPersistedRole(context: Context): String {
        return try {
            val userPrefs = context.getSharedPreferences(PREF_USER, Context.MODE_PRIVATE)
            var role = userPrefs.getString(KEY_ROLE, null)
            if (role.isNullOrBlank()) {
                val sessionPrefs = context.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE)
                role = sessionPrefs.getString(KEY_ROLE, null)
            }
            normalizeRole(role ?: ROLE_BOTH)
        } catch (_: Throwable) {
            ROLE_BOTH
        }
    }

    private fun normalizeRole(raw: String): String {
        return when (raw.trim().uppercase()) {
            ROLE_FARMER, "SELLER", "FARMER_SELLER" -> ROLE_FARMER
            ROLE_BUYER, "CONSUMER" -> ROLE_BUYER
            else -> ROLE_BOTH
        }
    }
}
