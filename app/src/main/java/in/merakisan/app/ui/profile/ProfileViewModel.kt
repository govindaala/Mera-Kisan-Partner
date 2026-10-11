// app/src/main/java/in/merakisan/app/ui/profile/ProfileViewModel.kt
package `in`.merakisan.app.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.security.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UserProfileUiModel(
    val uid: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val role: String = "buyer",
    val village: String = "",
    val district: String = "",
    val state: String = "Madhya Pradesh",
    val verificationStatus: String = "unverified",
    val isFarmer: Boolean = false,
    val isBuyer: Boolean = true
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val apiService = ApiClient.getApiService(application)

    private val _profile = MutableLiveData<UserProfileUiModel>()
    val profile: LiveData<UserProfileUiModel> get() = _profile

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun loadProfile() {
        val userSession = sessionManager.getUserSession()
        if (userSession == null || userSession.uid.isBlank()) {
            _profile.value = UserProfileUiModel(name = "अतिथि उपयोगकर्ता (लॉगिन करें)")
            return
        }

        // स्थानीय सत्र से त्वरित लोड
        _profile.value = UserProfileUiModel(
            uid = userSession.uid,
            name = userSession.name.ifBlank { "किसान साथी" },
            phone = userSession.phone,
            email = userSession.email,
            role = userSession.role,
            village = userSession.village,
            district = userSession.district,
            state = userSession.state,
            verificationStatus = userSession.verificationStatus,
            isFarmer = userSession.role.contains("farmer", ignoreCase = true) || userSession.role.contains("both", ignoreCase = true),
            isBuyer = userSession.role.contains("buyer", ignoreCase = true) || userSession.role.contains("both", ignoreCase = true)
        )

        // बैकएंड API से नवीनतम डेटा सिंक
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getCurrentUser()
                if (response.isSuccessful && response.body()?.success == true) {
                    val remoteData = response.body()?.data
                    if (remoteData != null) {
                        sessionManager.saveUserSession(remoteData)
                        withContext(Dispatchers.Main) {
                            _profile.value = UserProfileUiModel(
                                uid = remoteData.uid,
                                name = remoteData.name,
                                phone = remoteData.phone.orEmpty(),
                                email = remoteData.email.orEmpty(),
                                role = remoteData.role,
                                village = remoteData.village.orEmpty(),
                                district = remoteData.district.orEmpty(),
                                state = remoteData.state.orEmpty(),
                                verificationStatus = remoteData.verificationStatus,
                                isFarmer = remoteData.role.contains("farmer", true) || remoteData.role.contains("both", true),
                                isBuyer = remoteData.role.contains("buyer", true) || remoteData.role.contains("both", true)
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = e.localizedMessage
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    fun logout() {
        sessionManager.clearSession()
        _profile.value = UserProfileUiModel(name = "अतिथि उपयोगकर्ता (लॉगिन करें)")
    }
}
