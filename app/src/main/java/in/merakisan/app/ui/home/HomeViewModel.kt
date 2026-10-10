// app/src/main/java/in/merakisan/app/ui/home/HomeViewModel.kt
package in.merakisan.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import in.merakisan.app.core.config.FeatureManager
import in.merakisan.app.core.network.ApiClient
import in.merakisan.app.core.network.model.ProductDto
import in.merakisan.app.core.security.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val activeRole: String,
        val userName: String,
        val locationText: String,
        val announcementBanner: String?,
        val freshProducts: List<ProductDto>,
        val activeProduceCount: Int,
        val pendingOrdersCount: Int
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val context = getApplication<Application>()
                val role = SessionManager.getUserRole(context)
                val uid = SessionManager.getUserUid(context) ?: ""

                val banner = if (FeatureManager.isEnabled(context, "banner_enabled")) {
                    "ताज़ा देशी फ़सलें सीधे किसानों से खरीदें — बिचौलिया मुक्त बाज़ार।"
                } else null

                // नेटवर्क API से ताज़ा फसलें लोड करना (टाइप-सुरक्षित अनुबंध)
                val apiService = ApiClient.getApiService(context)
                val response = apiService.getProducts()
                val products = if (response.isSuccessful && response.body()?.success == true) {
                    response.body()?.data ?: emptyList()
                } else {
                    emptyList()
                }

                val freshTodayList = products.filter { it.status == "active" }.take(5)
                val farmerProduceCount = products.count { it.sellerUid == uid && it.status == "active" }
                val pendingOrders = 0

                _uiState.value = HomeUiState.Success(
                    activeRole = role,
                    userName = "किसान साथी",
                    locationText = "📍 ग्राम: बर्ड़िया अमरा • मंदसौर",
                    announcementBanner = banner,
                    freshProducts = freshTodayList,
                    activeProduceCount = farmerProduceCount,
                    pendingOrdersCount = pendingOrders
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error("डैशबोर्ड डेटा लोड करने में समस्या: ${e.localizedMessage}")
            }
        }
    }
}
