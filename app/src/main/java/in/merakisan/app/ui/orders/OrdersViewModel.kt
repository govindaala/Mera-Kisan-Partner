// app/src/main/java/in/merakisan/app/ui/orders/OrdersViewModel.kt
package in.merakisan.app.ui.orders

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import in.merakisan.app.core.network.ApiClient
import in.merakisan.app.core.network.model.OrderDto
import in.merakisan.app.core.security.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class OrdersUiState {
    object Loading : OrdersUiState()
    data class Success(val orders: List<OrderDto>) : OrdersUiState()
    data class Error(val message: String) : OrdersUiState()
    object Empty : OrdersUiState()
}

class OrdersViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = ApiClient.getApiService(application)

    private val _uiState = MutableStateFlow<OrdersUiState>(OrdersUiState.Loading)
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    private var currentRole: String = "buyer"

    init {
        val userRole = SessionManager.getUserRole(application)
        currentRole = if (userRole == "FARMER") "farmer" else "buyer"
        loadOrders(currentRole)
    }

    fun setRoleFilter(role: String) {
        currentRole = role
        loadOrders(currentRole)
    }

    fun loadOrders(role: String = currentRole) {
        viewModelScope.launch {
            _uiState.value = OrdersUiState.Loading
            try {
                val response = apiService.getUserOrders(role = role)
                if (response.isSuccessful && response.body()?.success == true) {
                    val orders = response.body()?.data ?: emptyList()
                    if (orders.isEmpty()) {
                        _uiState.value = OrdersUiState.Empty
                    } else {
                        _uiState.value = OrdersUiState.Success(orders)
                    }
                } else {
                    val msg = response.body()?.error?.message ?: "ऑर्डर्स लोड करने में त्रुटि (HTTP ${response.code()})"
                    _uiState.value = OrdersUiState.Error(msg)
                }
            } catch (e: Exception) {
                _uiState.value = OrdersUiState.Error("नेटवर्क संपर्क विफल: ${e.localizedMessage}")
            }
        }
    }
}
