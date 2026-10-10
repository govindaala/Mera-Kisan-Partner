// app/src/main/java/in/merakisan/app/ui/auth/AuthViewModel.kt
package in.merakisan.app.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import in.merakisan.app.core.security.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class GoogleLinked(val account: GoogleSignInAccount) : AuthUiState()
    data class Success(val uid: String, val phone: String, val name: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var googleAccount: GoogleSignInAccount? = null

    fun onGoogleSignInSuccess(account: GoogleSignInAccount) {
        googleAccount = account
        _uiState.value = AuthUiState.GoogleLinked(account)
    }

    /**
     * Google क्रेडेंशियल्स और यूज़र द्वारा दर्ज चालू मोबाइल नंबर को स्थानीय रूप से सत्यापित और सहेजना
     */
    fun finalizeAuthentication(enteredPhone: String) {
        val cleanPhone = enteredPhone.trim()

        if (cleanPhone.length != 10 || !cleanPhone.all { it.isDigit() }) {
            _uiState.value = AuthUiState.Error("कृपया मान्य 10-अंकों का मोबाइल नंबर दर्ज करें")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            val context = getApplication<Application>()
            val uid = googleAccount?.id ?: "usr_${System.currentTimeMillis()}"
            val name = googleAccount?.displayName ?: "किसान साथी"
            val emailToken = googleAccount?.idToken ?: "token_$uid"

            // Keystore-backed सुरक्षित SharedPreferences में सत्र सहेजना
            val currentRole = SessionManager.getUserRole(context).ifBlank { "BUYER" }
            SessionManager.saveSession(
                context = context,
                token = emailToken,
                uid = uid,
                role = currentRole
            )

            _uiState.value = AuthUiState.Success(
                uid = uid,
                phone = cleanPhone,
                name = name
            )
        }
    }

    fun continueAsGuest() {
        val context = getApplication<Application>()
        val guestUid = "guest_${System.currentTimeMillis()}"
        val currentRole = SessionManager.getUserRole(context).ifBlank { "BUYER" }

        SessionManager.saveSession(
            context = context,
            token = "guest_session_token",
            uid = guestUid,
            role = currentRole
        )

        _uiState.value = AuthUiState.Success(
            uid = guestUid,
            phone = "",
            name = "अतिथि उपयोगकर्ता"
        )
    }

    fun resetError() {
        _uiState.value = AuthUiState.Idle
    }
}
