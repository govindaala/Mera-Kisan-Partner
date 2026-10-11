// app/src/main/java/in/merakisan/app/ui/requests/BuyerRequestsViewModel.kt
package `in`.merakisan.app.ui.requests

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.network.model.BuyerRequestDto
import `in`.merakisan.app.core.security.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BuyerRequestsViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = ApiClient.getApiService(application)
    private val sessionManager = SessionManager(application)

    private val _requests = MutableLiveData<List<BuyerRequestDto>>()
    val requests: LiveData<List<BuyerRequestDto>> get() = _requests

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    private val _isCreateSuccess = MutableLiveData<Boolean>()
    val isCreateSuccess: LiveData<Boolean> get() = _isCreateSuccess

    private var allFetchedRequests: List<BuyerRequestDto> = emptyList()

    fun loadRequests() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getBuyerRequests()
                if (response.isSuccessful && response.body()?.success == true) {
                    val list = response.body()?.data ?: emptyList()
                    allFetchedRequests = list

                    withContext(Dispatchers.Main) {
                        _requests.value = list
                        _isLoading.value = false
                        if (list.isEmpty()) {
                            _errorMessage.value = "वर्तमान में 'मुझे चाहिए' बोर्ड पर कोई सक्रिय मांग नहीं है।"
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        allFetchedRequests = emptyList()
                        _requests.value = emptyList()
                        _isLoading.value = false
                        _errorMessage.value = "मांग सूची लोड करने में असमर्थ।"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    allFetchedRequests = emptyList()
                    _requests.value = emptyList()
                    _isLoading.value = false
                    _errorMessage.value = "इंटरनेट संपर्क विफलता: ${e.localizedMessage}"
                }
            }
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
        if (user == null || user.uid.isBlank()) {
            _errorMessage.value = "मांग पोस्ट करने के लिए कृपया पहले लॉगिन करें।"
            return
        }

        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dto = BuyerRequestDto(
                    buyerUid = user.uid,
                    buyerName = user.name.ifBlank { "ग्राहक" },
                    buyerPhone = user.phone.orEmpty(),
                    productName = productName,
                    category = category,
                    quantity = quantity,
                    unit = unit,
                    targetPricePaise = targetPricePaise,
                    district = user.district.orEmpty(),
                    village = user.village.orEmpty(),
                    notes = notes,
                    status = "active"
                )

                val response = apiService.createBuyerRequest(dto)
                if (response.isSuccessful && response.body()?.success == true) {
                    withContext(Dispatchers.Main) {
                        _isCreateSuccess.value = true
                        _isLoading.value = false
                        loadRequests()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _isCreateSuccess.value = false
                        _isLoading.value = false
                        _errorMessage.value = "मांग दर्ज करने में सर्वर विफलता।"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isCreateSuccess.value = false
                    _isLoading.value = false
                    _errorMessage.value = "नेटवर्क विफलता: ${e.localizedMessage}"
                }
            }
        }
    }

    fun filterRequests(query: String) {
        val q = query.trim().lowercase()
        if (q.isBlank()) {
            _requests.value = allFetchedRequests
            return
        }

        val filtered = allFetchedRequests.filter { item ->
            item.productName.lowercase().contains(q) ||
            item.category.lowercase().contains(q) ||
            item.district.lowercase().contains(q) ||
            item.notes.lowercase().contains(q)
        }
        _requests.value = filtered
    }
}
