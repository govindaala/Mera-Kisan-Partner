// app/src/main/java/in/merakisan/app/ui/home/HomeViewModel.kt
package `in`.merakisan.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.network.model.ProductDto
import `in`.merakisan.app.core.security.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class HomeUiState {
    object Loading : HomeUiState()

    data class Success(
        val userName: String = "किसान साथी",
        val locationText: String = "मध्य प्रदेश",
        val activeRole: String = "BUYER",
        val activeProduceCount: Int = 0,
        val pendingOrdersCount: Int = 0,
        val announcementBanner: String? = null,
        val freshProducts: List<ProductDto> = emptyList(),
        val freshToday: List<ProductDto> = emptyList(),
        val nearbyProducts: List<ProductDto> = emptyList(),
        val smallQuantityProducts: List<ProductDto> = emptyList()
    ) : HomeUiState()

    data class Error(val message: String = "") : HomeUiState()
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = ApiClient.getApiService(application)
    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        _uiState.value = HomeUiState.Loading

        viewModelScope.launch {
            try {
                val session = sessionManager.getUserSession()

                val response = withContext(Dispatchers.IO) {
                    apiService.getProducts()
                }

                val productList: List<ProductDto> = if (response.isSuccessful) {
                    response.body()?.data ?: emptyList()
                } else {
                    emptyList()
                }

                val fresh = productList.filter {
                    it.verificationStatus == "farmer_declared" || it.createdAt != null
                }.ifEmpty { productList }.take(10)

                val smallQty = productList.filter {
                    it.stockQuantity in 1.0..25.0
                }

                _uiState.value = HomeUiState.Success(
                    userName = session.name.ifBlank { "किसान साथी" },
                    locationText = session.role.ifBlank { "मध्य प्रदेश" },
                    activeRole = if (session.role.equals("FARMER", ignoreCase = true)) "FARMER" else "BUYER",
                    activeProduceCount = productList.size,
                    pendingOrdersCount = 0,
                    announcementBanner = null,
                    freshProducts = fresh,
                    freshToday = fresh,
                    nearbyProducts = productList,
                    smallQuantityProducts = smallQty
                )
            } catch (_: Exception) {
                _uiState.value = HomeUiState.Error(
                    message = "डेटा लोड नहीं हो सका। कृपया इंटरनेट चेक करें।"
                )
            }
        }
    }

    fun loadDashboardData() {
        loadHomeData()
    }
}
