// app/src/main/java/in/merakisan/app/ui/profile/ProfileViewModel.kt
package `in`.merakisan.app.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.security.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserProfileUiModel(
    val uid: String = "",
    val name: String = "",
    val phone: String = "",
    val currentRole: String = "both"
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionManager = SessionManager(application)
    private val apiService = ApiClient.getApiService(application)

    private val _userProfile = MutableStateFlow<UserProfileUiModel?>(null)
    val userProfile: StateFlow<UserProfileUiModel?> = _userProfile.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        val session = sessionManager.getUserSession()
        _userProfile.value = UserProfileUiModel(session.uid, session.name, session.phone, session.role)

        viewModelScope.launch {
            try {
                val resp = apiService.getCurrentUser()
                if (resp.isSuccessful && resp.body()?.success == true) {
                    val u = resp.body()?.data
                    if (u != null) {
                        sessionManager.saveSession(uid = u.uid, name = u.name, phone = u.phone, role = u.role)
                        _userProfile.value = UserProfileUiModel(u.uid, u.name, u.phone, u.role)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun updateRole(newRole: String) {
        val current = _userProfile.value ?: return
        sessionManager.saveSession(uid = current.uid, name = current.name, phone = current.phone, role = newRole)
        _userProfile.value = current.copy(currentRole = newRole)
    }
}
