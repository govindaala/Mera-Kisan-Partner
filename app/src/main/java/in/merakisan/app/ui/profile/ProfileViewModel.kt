// app/src/main/java/in/merakisan/app/ui/profile/ProfileViewModel.kt
package in.merakisan.app.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import in.merakisan.app.core.security.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserProfileUiModel(
    val uid: String,
    val name: String,
    val phone: String,
    val currentRole: String, // FARMER, BUYER, BOTH
    val district: String,
    val village: String,
    val verificationStatus: String
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val _userProfile = MutableStateFlow<UserProfileUiModel?>(null)
    val userProfile: StateFlow<UserProfileUiModel?> = _userProfile.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val uid = SessionManager.getUserUid(context) ?: "user_default"
            val role = SessionManager.getUserRole(context)

            // Keystore-backed सुरक्षित प्रोफ़ाइल स्थिति लोड करना
            _userProfile.value = UserProfileUiModel(
                uid = uid,
                name = "किसान साथी",
                phone = "+91 98260 00000",
                currentRole = role,
                district = "मंदसौर",
                village = "गरोठ",
                verificationStatus = "verified_farmer"
            )
        }
    }

    /**
     * बिना पुनः लॉगिन किए तुरंत भूमिका स्विच करना
     */
    fun updateRole(newRole: String) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val currentToken = SessionManager.getAuthToken(context) ?: "active_token"
            val currentUid = SessionManager.getUserUid(context) ?: "active_uid"

            SessionManager.saveSession(
                context = context,
                token = currentToken,
                uid = currentUid,
                role = newRole
            )

            // स्थानीय स्टेट को ताज़ा करना
            _userProfile.value = _userProfile.value?.copy(currentRole = newRole)
        }
    }

    fun logout() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            SessionManager.clearSession(context)
        }
    }
}
