// app/src/main/java/in/merakisan/app/ui/home/HomeViewModel.kt
package `in`.merakisan.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.network.model.ProductDto
import `in`.merakisan.app.core.security.SessionManager
import `in`.merakisan.app.data.local.AppDatabase
import `in`.merakisan.app.data.local.entity.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val freshToday: List<ProductDto> = emptyList(),
        val nearbyProducts: List<ProductDto> = emptyList(),
        val smallQuantityProducts: List<ProductDto> = emptyList()
    ) : HomeUiState()
    object Empty : HomeUiState()
    data class Error(val userFriendlyMessage: String) : HomeUiState()
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = ApiClient.getApiService(application)
    private val productDao = AppDatabase.getInstance(application).productDao()
    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    /**
     * होम डैशबोर्ड डेटा लोडिंग इंजन (Defensive & Offline-First)
     */
    fun loadDashboardData() {
        _uiState.value = HomeUiState.Loading

        viewModelScope.launch {
            try {
                // 1. नेटवर्क से डेटा प्राप्त करना
                val response = withContext(Dispatchers.IO) {
                    apiService.getProducts()
                }

                // 2. Gson / Retrofit रैपर रिज़ॉल्यूशन
                val productList: List<ProductDto> = if (response.isSuccessful) {
                    val body = response.body()
                    when {
                        // यदि बैकएंड { "success": true, "data": [ ... ] } भेज रहा है
                        body?.data != null -> body.data
                        else -> emptyList()
                    }
                } else {
                    emptyList()
                }

                if (productList.isNotEmpty()) {
                    // 3. सफल नेटवर्क डेटा को लोकल रूम डेटाबेस में कैश करना (Master Spec v1.1)
                    withContext(Dispatchers.IO) {
                        try {
                            val entities = productList.map { it.toEntity() }
                            productDao.insertProducts(entities)
                        } catch (_: Exception) {
                            // डेटाबेस कैशिंग विफलता से UI नहीं रुकेगी
                        }
                    }

                    // 4. श्रेणियों के अनुसार डेटा फ़िल्टर करना
                    val freshToday = productList.filter { 
                        it.verificationStatus == "farmer_declared" || it.createdAt != null 
                    }.take(10)

                    val smallQty = productList.filter { 
                        it.stockQuantity in 1.0..25.0 
                    }

                    _uiState.value = HomeUiState.Success(
                        freshToday = freshToday,
                        nearbyProducts = productList,
                        smallQuantityProducts = smallQty
                    )
                } else {
                    // 5. सर्वर से खाली डेटा आने पर लोकल रूम कैश से रिकवरी
                    loadFromLocalCache("वर्तमान में कोई नई उपज सूचीबद्ध नहीं है")
                }

            } catch (e: Exception) {
                // 6. JSON मिसमैच या नेटवर्क फेलियर पर सुरक्षित फ़ॉलबैक (क्रैश/कच्चे अपवाद की रोकथाम)
                loadFromLocalCache("नेटवर्क धीमा है, ऑफ़लाइन डेटा दिखाया जा रहा है")
            }
        }
    }

    /**
     * ऑफ़लाइन फ़ॉलबैक: इंटरनेट न होने पर SQLite/Room से डेटा लोड करता है
     */
    private suspend fun loadFromLocalCache(fallbackMessage: String) {
        val cachedEntities = withContext(Dispatchers.IO) {
            try {
                productDao.getAllProductsList()
            } catch (_: Exception) {
                emptyList()
            }
        }

        if (cachedEntities.isNotEmpty()) {
            val cachedProducts = cachedEntities.map { it.toDto() }
            _uiState.value = HomeUiState.Success(
                freshToday = cachedProducts.take(10),
                nearbyProducts = cachedProducts,
                smallQuantityProducts = cachedProducts.filter { it.stockQuantity in 1.0..25.0 }
            )
        } else {
            _uiState.value = HomeUiState.Empty
        }
    }
}
