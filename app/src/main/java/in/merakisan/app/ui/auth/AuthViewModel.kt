// app/src/main/java/in/merakisan/app/ui/auth/AuthViewModel.kt
package `in`.merakisan.app.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.network.model.VerifyGoogleRequest
import `in`.merakisan.app.core.security.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val role: String, val isNewUser: Boolean) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val apiService = ApiClient.getApiService(application)

    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> get() = _authState

    fun handleGoogleSignInResult(account: GoogleSignInAccount?) {
        val idToken = account?.idToken
        if (idToken.isNullOrBlank()) {
            _authState.value = AuthState.Error("Google प्रमाणीकरण टोकन प्राप्त नहीं हुआ। कृपया पुनः प्रयास करें।")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = VerifyGoogleRequest(
                    idToken = idToken,
                    email = account.email.orEmpty(),
                    displayName = account.displayName.orEmpty(),
                    photoUrl = account.photoUrl?.toString()
                )

                val response = apiService.verifyGoogleToken(request)
                if (response.isSuccessful && response.body()?.success == true) {
                    val authData = response.body()?.data
                    if (authData != null) {
                        sessionManager.saveAuthToken(authData.token)
                        sessionManager.saveUserSession(authData.user)

                        withContext(Dispatchers.Main) {
                            _authState.value = AuthState.Success(
                                role = authData.user.role,
                                isNewUser = authData.isNewUser
                            )
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            _authState.value = AuthState.Error("सर्वर से अमान्य डेटा प्राप्त हुआ।")
                        }
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "प्रमाणीकरण विफल रहा।"
                    withContext(Dispatchers.Main) {
                        _authState.value = AuthState.Error("त्रुटि: $errorMsg")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _authState.value = AuthState.Error("नेटवर्क विफलता: ${e.localizedMessage}")
                }
            }
        }
    }
}
