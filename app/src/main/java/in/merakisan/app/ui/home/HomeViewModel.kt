// app/src/main/java/in/merakisan/app/ui/home/HomeViewModel.kt
package `in`.merakisan.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.merakisan.app.core.config.FeatureManager
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

    private val appContext = application.applicationContext
    private val apiService = ApiClient.getApiService(appContext)
    private val sessionManager = SessionManager(appContext)

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        _uiState.value = HomeUiState.Loading

        viewModelScope.launch {
            val previousCrash = ApiClient.consumeLastCrashSummary(appContext)

            try {
                // Background me remote config sync karein (bina UI block kiye)
                withContext(Dispatchers.IO) {
                    FeatureManager.syncRemoteConfig(appContext)
                }

                val session = try {
                    sessionManager.getUserSession()
                } catch (_: Throwable) {
                    null
                }

                val response = withContext(Dispatchers.IO) {
                    try {
                        apiService.getProducts()
                    } catch (_: Throwable) {
                        null
                    }
                }

                val rawList: List<ProductDto> = if (response != null && response.isSuccessful) {
                    response.body()?.data ?: emptyList()
                } else {
                    emptyList()
                }

                val productList = rawList.mapNotNull {
                    try {
                        it.sanitize()
                    } catch (_: Throwable) {
                        null
                    }
                }

                val fresh = productList.filter {
                    it.verificationStatus == "farmer_declared" || it.createdAt != null
                }.ifEmpty { productList }.take(10)

                val smallQty = productList.filter {
                    it.stockQuantity in 0.5..25.0
                }

                val userName = session?.name?.ifBlank { "किसान साथी" } ?: "किसान साथी"
                val roleStr = session?.role?.ifBlank { "BUYER" } ?: "BUYER"
                val activeRole = if (roleStr.equals("FARMER", ignoreCase = true)) "FARMER" else "BUYER"

                _uiState.value = HomeUiState.Success(
                    userName = userName,
                    locationText = "मध्य प्रदेश",
                    activeRole = activeRole,
                    activeProduceCount = productList.size,
                    pendingOrdersCount = 0,
                    announcementBanner = previousCrash,
                    freshProducts = fresh,
                    freshToday = fresh,
                    nearbyProducts = productList,
                    smallQuantityProducts = smallQty
                )
            } catch (t: Throwable) {
                // Network ya parsing error par bhi app crash ya freeze na ho, safe state dikhe
                _uiState.value = HomeUiState.Success(
                    userName = "किसान साथी",
                    locationText = "मध्य प्रदेश",
                    activeRole = "BUYER",
                    announcementBanner = previousCrash ?: "ऑफ़लाइन मोड: सर्वर से डेटा सिंक हो रहा है",
                    freshProducts = emptyList(),
                    freshToday = emptyList(),
                    nearbyProducts = emptyList(),
                    smallQuantityProducts = emptyList()
                )
            }
        }
    }

    fun loadDashboardData() {
        loadHomeData()
    }
}
