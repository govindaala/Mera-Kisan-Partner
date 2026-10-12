// app/src/main/java/in/merakisan/app/ui/orders/OrdersViewModel.kt
package `in`.merakisan.app.ui.orders

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.network.UpdateOrderStatusRequest
import `in`.merakisan.app.core.network.model.OrderDto
import `in`.merakisan.app.core.security.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class OrdersUiState {
    object Loading : OrdersUiState()
    data class Success(val orders: List<OrderDto> = emptyList()) : OrdersUiState()
    object Empty : OrdersUiState()
    data class Error(val message: String = "") : OrdersUiState()
}

class OrdersViewModel(application: Application) : AndroidViewModel(application) {
    private val apiService = ApiClient.getApiService(application)
    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow<OrdersUiState>(OrdersUiState.Loading)
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    private var currentFilterRole: String = "buyer"
    private var allOrders: List<OrderDto> = emptyList()

    fun setRoleFilter(role: String) {
        currentFilterRole = role
        filterOrders()
    }

    fun loadOrders() {
        val user = sessionManager.getUserSession()
        if (user.uid.isBlank()) {
            _uiState.value = OrdersUiState.Empty
            return
        }
        _uiState.value = OrdersUiState.Loading
        viewModelScope.launch {
            try {
                val resp = apiService.getOrders()
                if (resp.isSuccessful && resp.body()?.success == true) {
                    allOrders = resp.body()?.data ?: emptyList()
                    filterOrders()
                } else {
                    _uiState.value = OrdersUiState.Empty
                }
            } catch (e: Exception) {
                _uiState.value = OrdersUiState.Error(e.localizedMessage ?: "त्रुटि")
            }
        }
    }

    private fun filterOrders() {
        val user = sessionManager.getUserSession()
        val filtered = allOrders.filter { order ->
            if (currentFilterRole.equals("farmer", ignoreCase = true)) {
                order.sellerUid == user.uid
            } else {
                order.buyerUid == user.uid
            }
        }
        _uiState.value = if (filtered.isEmpty()) OrdersUiState.Empty else OrdersUiState.Success(filtered)
    }

    fun updateOrderStatus(orderId: String, req: UpdateOrderStatusRequest) {
        viewModelScope.launch {
            try {
                val resp = apiService.updateOrderStatus(orderId, req)
                if (resp.isSuccessful) loadOrders()
            } catch (_: Exception) {}
        }
    }
}
