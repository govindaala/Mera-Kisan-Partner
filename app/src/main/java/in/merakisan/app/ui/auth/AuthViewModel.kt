// app/src/main/java/in/merakisan/app/ui/auth/AuthViewModel.kt
package `in`.merakisan.app.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import `in`.merakisan.app.core.security.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class GoogleLinked(val account: GoogleSignInAccount) : AuthUiState()
    data class Success(val role: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var currentAccount: GoogleSignInAccount? = null

    fun onGoogleSignInSuccess(account: GoogleSignInAccount?) {
        if (account == null) {
            _uiState.value = AuthUiState.Error("Google खाता प्राप्त नहीं हुआ।")
            return
        }
        currentAccount = account
        _uiState.value = AuthUiState.GoogleLinked(account)
    }

    fun finalizeAuthentication(phone: String) {
        val account = currentAccount
        if (account == null) {
            _uiState.value = AuthUiState.Error("सत्र समाप्त हुआ। कृपया Google से पुनः साइन इन करें।")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val uid = account.id ?: "usr_${System.currentTimeMillis()}"
            val token = account.idToken ?: "token_$uid"
            val name = account.displayName ?: "किसान साथी"

            sessionManager.saveAuthToken(token)
            sessionManager.saveUserSession(
                userId = uid,
                name = name,
                phone = phone,
                role = "both"
            )

            _uiState.value = AuthUiState.Success("both")
        }
    }

    fun continueAsGuest() {
        val guestUid = "guest_${System.currentTimeMillis()}"
        sessionManager.saveAuthToken("guest_session_token")
        sessionManager.saveUserSession(
            userId = guestUid,
            name = "अतिथि किसान",
            phone = "",
            role = "buyer"
        )
        _uiState.value = AuthUiState.Success("buyer")
    }
}
