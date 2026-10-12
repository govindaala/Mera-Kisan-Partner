// app/src/main/java/in/merakisan/app/ui/mandi/MandiPricesViewModel.kt
package `in`.merakisan.app.ui.mandi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.network.model.MandiPriceDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MandiUiState {
    object FeatureDisabled : MandiUiState()
    object Loading : MandiUiState()
    data class Success(val prices: List<MandiPriceDto> = emptyList()) : MandiUiState()
    data class Empty(val message: String = "आज का कोई मंडी भाव उपलब्ध नहीं है।") : MandiUiState()
    data class Error(val message: String = "") : MandiUiState()
}

class MandiPricesViewModel(application: Application) : AndroidViewModel(application) {
    private val apiService = ApiClient.getApiService(application)
    private val _uiState = MutableStateFlow<MandiUiState>(MandiUiState.Loading)
    val uiState: StateFlow<MandiUiState> = _uiState.asStateFlow()

    fun loadMandiPrices(district: String? = null, forceRefresh: Boolean = false) {
        _uiState.value = MandiUiState.Loading
        viewModelScope.launch {
            try {
                val resp = apiService.getMandiPrices(district)
                if (resp.isSuccessful && resp.body()?.success == true) {
                    val list = resp.body()?.data ?: emptyList()
                    _uiState.value = if (list.isEmpty()) MandiUiState.Empty() else MandiUiState.Success(list)
                } else {
                    _uiState.value = MandiUiState.Empty()
                }
            } catch (e: Exception) {
                _uiState.value = MandiUiState.Error(e.localizedMessage ?: "नेटवर्क त्रुटि")
            }
        }
    }
}
