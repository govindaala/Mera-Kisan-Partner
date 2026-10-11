// app/src/main/java/in/merakisan/app/ui/orders/OrdersViewModel.kt
package `in`.merakisan.app.ui.orders

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.network.model.OrderDto
import `in`.merakisan.app.core.network.model.UpdateOrderStatusRequest
import `in`.merakisan.app.core.security.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OrdersViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = ApiClient.getApiService(application)
    private val sessionManager = SessionManager(application)

    private val _orders = MutableLiveData<List<OrderDto>>()
    val orders: LiveData<List<OrderDto>> get() = _orders

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    private var allFetchedOrders: List<OrderDto> = emptyList()

    fun loadOrders() {
        val user = sessionManager.getUserSession()
        if (user == null || user.uid.isBlank()) {
            _orders.value = emptyList()
            _errorMessage.value = "ऑर्डर देखने के लिए कृपया लॉगिन करें।"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getOrders()
                if (response.isSuccessful && response.body()?.success == true) {
                    val rawList = response.body()?.data ?: emptyList()
                    // यूज़र की भूमिका के अनुसार प्रासंगिक ऑर्डर ही दिखाना (IDOR से सुरक्षा)
                    val relevantList = rawList.filter { order ->
                        order.buyerUid == user.uid || order.sellerUid == user.uid
                    }
                    allFetchedOrders = relevantList

                    withContext(Dispatchers.Main) {
                        _orders.value = relevantList
                        _isLoading.value = false
                        if (relevantList.isEmpty()) {
                            _errorMessage.value = "कोई ऑर्डर उपलब्ध नहीं है।"
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _orders.value = emptyList()
                        _isLoading.value = false
                        _errorMessage.value = "ऑर्डर सूची लोड करने में विफलता।"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _orders.value = emptyList()
                    _isLoading.value = false
                    _errorMessage.value = "नेटवर्क कनेक्शन त्रुटि: ${e.localizedMessage}"
                }
            }
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: String, notes: String = "") {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val req = UpdateOrderStatusRequest(status = newStatus, notes = notes)
                val response = apiService.updateOrderStatus(orderId, req)

                if (response.isSuccessful && response.body()?.success == true) {
                    withContext(Dispatchers.Main) {
                        loadOrders()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _isLoading.value = false
                        _errorMessage.value = "स्टेटस बदलने में विफलता।"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                    _errorMessage.value = "नेटवर्क विफलता: ${e.localizedMessage}"
                }
            }
        }
    }
}
