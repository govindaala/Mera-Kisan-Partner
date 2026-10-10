// app/src/main/java/in/merakisan/app/ui/requests/BuyerRequestsViewModel.kt
package in.merakisan.app.ui.requests

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import in.merakisan.app.core.network.model.BuyerRequestDto
import in.merakisan.app.core.network.model.CreateBuyerRequestPayload
import in.merakisan.app.core.security.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class BuyerRequestsUiState {
    object Loading : BuyerRequestsUiState()
    data class Success(val requests: List<BuyerRequestDto>) : BuyerRequestsUiState()
    data class Error(val message: String) : BuyerRequestsUiState()
    object Empty : BuyerRequestsUiState()
}

class BuyerRequestsViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<BuyerRequestsUiState>(BuyerRequestsUiState.Loading)
    val uiState: StateFlow<BuyerRequestsUiState> = _uiState.asStateFlow()

    // स्थानीय कैश व इन-मेमोरी सूची (जब तक बैकएंड REST एंडपॉइंट से सिंक्रोनाइज़ हो)
    private val localRequestsList = mutableListOf<BuyerRequestDto>()

    init {
        loadBuyerRequests()
    }

    fun loadBuyerRequests() {
        viewModelScope.launch {
            _uiState.value = BuyerRequestsUiState.Loading
            try {
                // नेटवर्क विलंब सिमुलेशन और प्रारंभिक डेटा
                delay(600)
                if (localRequestsList.isEmpty()) {
                    seedInitialRequests()
                }

                if (localRequestsList.isEmpty()) {
                    _uiState.value = BuyerRequestsUiState.Empty
                } else {
                    _uiState.value = BuyerRequestsUiState.Success(localRequestsList.toList())
                }
            } catch (e: Exception) {
                _uiState.value = BuyerRequestsUiState.Error("मांग सूची लोड करने में समस्या: ${e.localizedMessage}")
            }
        }
    }

    fun postNewRequest(payload: CreateBuyerRequestPayload) {
        viewModelScope.launch {
            val userUid = SessionManager.getUserUid(getApplication()) ?: "buyer_guest"
            val newId = "REQ_${System.currentTimeMillis().toString().takeLast(6)}"

            val newRequest = BuyerRequestDto(
                requestId = newId,
                buyerUid = userUid,
                buyerName = "स्थानीय ग्राहक",
                buyerPhone = "9826000000",
                productName = payload.productName,
                category = payload.category,
                variety = payload.variety,
                quantity = payload.quantity,
                unit = payload.unit,
                targetPricePaise = payload.targetPricePaise,
                district = payload.district,
                village = payload.village,
                requiredDate = payload.requiredDate,
                notes = payload.notes,
                status = "ACTIVE",
                offersCount = 0,
                createdAt = "आज"
            )

            localRequestsList.add(0, newRequest)
            _uiState.value = BuyerRequestsUiState.Success(localRequestsList.toList())
        }
    }

    private fun seedInitialRequests() {
        localRequestsList.add(
            BuyerRequestDto(
                requestId = "REQ_1001",
                buyerUid = "B_901",
                buyerName = "कैलाश शर्मा",
                buyerPhone = "9826112233",
                productName = "देशी गेहूं (शरबती)",
                category = "अनाज",
                variety = "बिना केमिकल / शुद्ध",
                quantity = 50.0,
                unit = "kg",
                targetPricePaise = 4200, // ₹42.00
                village = "गरोठ",
                district = "मंदसौर",
                requiredDate = "15 अक्टूबर तक",
                notes = "घर के खाने के लिए शुद्ध देशी शरबती गेहूं चाहिए।",
                status = "ACTIVE",
                offersCount = 2,
                createdAt = "कल"
            )
        )
        localRequestsList.add(
            BuyerRequestDto(
                requestId = "REQ_1002",
                buyerUid = "B_902",
                buyerName = "राकेश वर्मा",
                buyerPhone = "9826445566",
                productName = "कच्ची घानी मूंगफली तेल",
                category = "तेल",
                variety = "कोल्ड प्रेस्ड",
                quantity = 10.0,
                unit = "लीटर",
                targetPricePaise = 22000, // ₹220.00
                village = "शामगढ़",
                district = "मंदसौर",
                requiredDate = "तुरंत",
                notes = "लकड़ी की घानी का निकला हुआ शुद्ध तेल चाहिए।",
                status = "ACTIVE",
                offersCount = 1,
                createdAt = "आज"
            )
        )
    }
}
