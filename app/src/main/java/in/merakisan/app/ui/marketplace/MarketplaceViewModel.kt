// app/src/main/java/in/merakisan/app/ui/marketplace/MarketplaceViewModel.kt
package `in`.merakisan.app.ui.marketplace

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.network.model.ProductDto
import `in`.merakisan.app.data.local.AppDatabase
import `in`.merakisan.app.data.local.entity.toDto
import `in`.merakisan.app.data.local.entity.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Marketplace UI स्टेट मशीन (MarketplaceFragment के साथ 100% संगत)
 */
sealed class MarketplaceUiState {
    object Loading : MarketplaceUiState()
    data class Success(
        val products: List<ProductDto> = emptyList(),
        val data: List<ProductDto> = products
    ) : MarketplaceUiState()
    data class Error(
        val message: String = "",
        val error: String = message
    ) : MarketplaceUiState()
    object Empty : MarketplaceUiState()
    object Idle : MarketplaceUiState()
    data class Offline(
        val products: List<ProductDto> = emptyList()
    ) : MarketplaceUiState()
}

/**
 * MERA KISAN Marketplace View Model
 * पूर्ण कार्यक्षमता: फ़िल्टरिंग, सर्च, ऑफ़लाइन कैश और UI स्टेट सिंक्रोनाइज़ेशन
 */
class MarketplaceViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = ApiClient.getApiService(application)
    private val productDao = AppDatabase.getInstance(application).productDao()

    // 1. MarketplaceFragment द्वारा अपेक्षित मुख्य UI स्टेट
    private val _uiState = MutableLiveData<MarketplaceUiState>(MarketplaceUiState.Idle)
    val uiState: LiveData<MarketplaceUiState> get() = _uiState

    // 2. लेगेसी कॉलिंग कंपैटिबिलिटी
    private val _products = MutableLiveData<List<ProductDto>>()
    val products: LiveData<List<ProductDto>> get() = _products

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    // आंतरिक फ़िल्टर अवस्था
    private var currentCategory: String? = null
    private var isSmallQuantityOnly: Boolean = false
    private var isOrganicOnly: Boolean = false
    private var currentSearchQuery: String = ""
    private var allLoadedProducts: List<ProductDto> = emptyList()

    // 3. MarketplaceFragment:74 द्वारा कॉल किया जाने वाला मुख्य लोडर
    fun loadMarketplaceProducts() {
        loadProducts(
            category = currentCategory,
            isSmallQuantityOnly = isSmallQuantityOnly,
            isOrganicOnly = isOrganicOnly
        )
    }

    // 4. MarketplaceFragment:82, 93, 96, 99 द्वारा कॉल किए जाने वाले फ़िल्टर मेथड्स
    fun setCategoryFilter(category: String?) {
        currentCategory = if (category.isNullOrBlank() || category.equals("all", ignoreCase = true)) null else category
        loadMarketplaceProducts()
    }

    // 5. MarketplaceFragment:83, 87 द्वारा कॉल किया जाने वाला छोटी मात्रा फ़िल्टर
    fun setSmallQuantityFilter(enabled: Boolean) {
        isSmallQuantityOnly = enabled
        loadMarketplaceProducts()
    }

    // 6. MarketplaceFragment:84, 90 द्वारा कॉल किया जाने वाला जैविक फ़िल्टर
    fun setOrganicFilter(enabled: Boolean) {
        isOrganicOnly = enabled
        loadMarketplaceProducts()
    }

    // 7. MarketplaceFragment:109 द्वारा कॉल किया जाने वाला सर्च मेथड
    fun searchProducts(query: String) {
        currentSearchQuery = query
        filterCurrentProducts()
    }

    fun loadProducts(
        category: String? = null,
        isSmallQuantityOnly: Boolean = false,
        isOrganicOnly: Boolean = false
    ) {
        _isLoading.value = true
        _error.value = null
        _uiState.value = MarketplaceUiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getProducts(
                    category = category,
                    maxQuantity = if (isSmallQuantityOnly) 25.0 else null,
                    organicOnly = if (isOrganicOnly) true else null,
                    limit = 50,
                    page = 1
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    val remoteProducts = response.body()?.data ?: emptyList()
                    allLoadedProducts = remoteProducts

                    productDao.insertProducts(remoteProducts.map { it.toEntity() })

                    withContext(Dispatchers.Main) {
                        _products.value = remoteProducts
                        _isLoading.value = false
                        if (remoteProducts.isEmpty()) {
                            _uiState.value = MarketplaceUiState.Empty
                        } else {
                            _uiState.value = MarketplaceUiState.Success(remoteProducts)
                        }
                    }
                } else {
                    fallbackToCache()
                }
            } catch (_: Exception) {
                fallbackToCache()
            }
        }
    }

    private suspend fun fallbackToCache() {
        val cached = productDao.getAllProducts().firstOrNull()?.map { it.toDto() } ?: emptyList()
        allLoadedProducts = cached
        withContext(Dispatchers.Main) {
            _products.value = cached
            _isLoading.value = false
            if (cached.isEmpty()) {
                _error.value = "कोई उत्पाद उपलब्ध नहीं है।"
                _uiState.value = MarketplaceUiState.Empty
            } else {
                _uiState.value = MarketplaceUiState.Success(cached)
            }
        }
    }

    fun filterProducts(query: String) {
        searchProducts(query)
    }

    private fun filterCurrentProducts() {
        val q = currentSearchQuery.trim().lowercase()
        if (q.isBlank()) {
            _products.value = allLoadedProducts
            if (allLoadedProducts.isEmpty()) {
                _uiState.value = MarketplaceUiState.Empty
            } else {
                _uiState.value = MarketplaceUiState.Success(allLoadedProducts)
            }
            return
        }

        val filtered = allLoadedProducts.filter { product ->
            val nameMatch = product.name.lowercase().contains(q)
            val categoryMatch = product.category.lowercase().contains(q)
            val varietyMatch = product.variety.orEmpty().lowercase().contains(q)
            val districtMatch = product.district.orEmpty().lowercase().contains(q)
            val villageMatch = product.village.orEmpty().lowercase().contains(q)
            val statusMatch = product.status.orEmpty().lowercase() == "active"

            statusMatch && (nameMatch || categoryMatch || varietyMatch || districtMatch || villageMatch)
        }

        _products.value = filtered
        if (filtered.isEmpty()) {
            _uiState.value = MarketplaceUiState.Empty
        } else {
            _uiState.value = MarketplaceUiState.Success(filtered)
        }
    }
}
