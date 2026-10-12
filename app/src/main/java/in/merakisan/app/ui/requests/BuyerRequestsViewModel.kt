// app/src/main/java/in/merakisan/app/ui/requests/BuyerRequestsViewModel.kt
package `in`.merakisan.app.ui.requests

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.network.model.BuyerRequestDto
import `in`.merakisan.app.core.security.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed class BuyerRequestsUiState {
    object Loading : BuyerRequestsUiState()
    data class Success(val requests: List<BuyerRequestDto> = emptyList()) : BuyerRequestsUiState()
    object Empty : BuyerRequestsUiState()
    data class Error(val message: String = "") : BuyerRequestsUiState()
}

class BuyerRequestsViewModel(application: Application) : AndroidViewModel(application) {
    private val apiService = ApiClient.getApiService(application)
    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow<BuyerRequestsUiState>(BuyerRequestsUiState.Loading)
    val uiState: StateFlow<BuyerRequestsUiState> = _uiState.asStateFlow()

    private var allRequests: List<BuyerRequestDto> = emptyList()

    fun loadBuyerRequests() {
        _uiState.value = BuyerRequestsUiState.Loading
        viewModelScope.launch {
            try {
                val resp = apiService.getBuyerRequests()
                if (resp.isSuccessful && resp.body()?.success == true) {
                    val list = resp.body()?.data ?: emptyList()
                    allRequests = list
                    _uiState.value = if (list.isEmpty()) BuyerRequestsUiState.Empty else BuyerRequestsUiState.Success(list)
                } else {
                    _uiState.value = BuyerRequestsUiState.Empty
                }
            } catch (e: Exception) {
                _uiState.value = BuyerRequestsUiState.Error(e.localizedMessage ?: "त्रुटि")
            }
        }
    }

    fun postNewRequest(request: BuyerRequestDto) {
        val finalRequest = if (request.requestId.isNullOrBlank()) {
            request.copy(requestId = UUID.randomUUID().toString())
        } else {
            request
        }
        viewModelScope.launch {
            try {
                val resp = apiService.createBuyerRequest(finalRequest)
                if (resp.isSuccessful) loadBuyerRequests()
            } catch (_: Exception) {}
        }
    }

    fun createBuyerRequest(
        productName: String,
        category: String,
        quantity: Double,
        unit: String,
        targetPricePaise: Long,
        notes: String
    ) {
        val user = sessionManager.getUserSession()
        val dto = BuyerRequestDto(
            requestId = UUID.randomUUID().toString(),
            buyerUid = user.uid,
            buyerName = user.name.ifBlank { "ग्राहक" },
            buyerPhone = user.phone,
            productName = productName,
            category = category,
            quantity = quantity,
            unit = unit,
            targetPricePaise = targetPricePaise,
            district = "",
            village = "",
            notes = notes,
            status = "active"
        )
        postNewRequest(dto)
    }

    fun filterRequests(query: String) {
        val q = query.trim().lowercase()
        if (q.isBlank()) {
            _uiState.value = if (allRequests.isEmpty()) BuyerRequestsUiState.Empty else BuyerRequestsUiState.Success(allRequests)
            return
        }
        val filtered = allRequests.filter { item ->
            item.productName.lowercase().contains(q) ||
            item.category.lowercase().contains(q) ||
            item.district.orEmpty().lowercase().contains(q) ||
            item.notes.orEmpty().lowercase().contains(q)
        }
        _uiState.value = if (filtered.isEmpty()) BuyerRequestsUiState.Empty else BuyerRequestsUiState.Success(filtered)
    }
}
