// app/src/main/java/in/merakisan/app/ui/requests/BuyerRequestsViewModel.kt
package `in`.merakisan.app.ui.requests

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.network.model.BuyerRequestDto
import `in`.merakisan.app.core.network.model.CreateBuyerRequestPayload
import `in`.merakisan.app.core.security.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed class BuyerRequestsUiState {
    object Loading : BuyerRequestsUiState()

    data class Success(
        val requests: List<BuyerRequestDto> = emptyList()
    ) : BuyerRequestsUiState()

    object Empty : BuyerRequestsUiState()

    data class Error(
        val message: String = ""
    ) : BuyerRequestsUiState()
}

class BuyerRequestsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val apiService = ApiClient.getApiService(application)
    private val sessionManager = SessionManager(application)

    private val _uiState =
        MutableStateFlow<BuyerRequestsUiState>(
            BuyerRequestsUiState.Loading
        )

    val uiState: StateFlow<BuyerRequestsUiState> =
        _uiState.asStateFlow()

    private var allRequests: List<BuyerRequestDto> = emptyList()

    fun loadBuyerRequests() {
        _uiState.value = BuyerRequestsUiState.Loading

        viewModelScope.launch {
            try {
                val response = apiService.getBuyerRequests()

                if (response.isSuccessful &&
                    response.body()?.success == true
                ) {
                    val requests =
                        response.body()?.data.orEmpty()

                    allRequests = requests

                    _uiState.value = if (requests.isEmpty()) {
                        BuyerRequestsUiState.Empty
                    } else {
                        BuyerRequestsUiState.Success(requests)
                    }
                } else {
                    _uiState.value = BuyerRequestsUiState.Error(
                        response.message().ifBlank {
                            "खरीदार की मांगें प्राप्त नहीं हो सकीं।"
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = BuyerRequestsUiState.Error(
                    e.localizedMessage ?: "मांगें लोड करने में त्रुटि हुई।"
                )
            }
        }
    }

    fun postNewRequest(request: BuyerRequestDto) {
        val finalRequest = if (request.requestId.isBlank()) {
            request.copy(
                requestId = "REQ_${UUID.randomUUID()}"
            )
        } else {
            request
        }

        viewModelScope.launch {
            try {
                val response =
                    apiService.createBuyerRequest(finalRequest)

                if (response.isSuccessful &&
                    response.body()?.success == true
                ) {
                    loadBuyerRequests()
                } else {
                    _uiState.value = BuyerRequestsUiState.Error(
                        response.message().ifBlank {
                            "मांग पोस्ट नहीं हो सकी।"
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = BuyerRequestsUiState.Error(
                    e.localizedMessage ?: "मांग पोस्ट करने में त्रुटि हुई।"
                )
            }
        }
    }

    fun postNewRequest(payload: CreateBuyerRequestPayload) {
        val user = sessionManager.getUserSession()

        val request = BuyerRequestDto(
            requestId = "REQ_${UUID.randomUUID()}",
            buyerUid = user.uid,
            buyerName = user.name.ifBlank { "ग्राहक साथी" },
            buyerPhone = user.phone,
            productName = payload.productName,
            category = payload.category,
            variety = payload.variety,
            quantity = payload.quantity,
            unit = payload.unit,
            targetPricePaise = payload.targetPricePaise,
            district = payload.district,
            village = "",
            notes = payload.notes,
            status = "active"
        )

        postNewRequest(request)
    }

    fun filterRequests(query: String) {
        val normalizedQuery = query.trim().lowercase()

        if (normalizedQuery.isBlank()) {
            _uiState.value = if (allRequests.isEmpty()) {
                BuyerRequestsUiState.Empty
            } else {
                BuyerRequestsUiState.Success(allRequests)
            }
            return
        }

        val filtered = allRequests.filter { request ->
            request.productName.lowercase()
                .contains(normalizedQuery) ||
            request.category.lowercase()
                .contains(normalizedQuery) ||
            request.district.lowercase()
                .contains(normalizedQuery) ||
            request.notes.orEmpty().lowercase()
                .contains(normalizedQuery)
        }

        _uiState.value = if (filtered.isEmpty()) {
            BuyerRequestsUiState.Empty
        } else {
            BuyerRequestsUiState.Success(filtered)
        }
    }
}
