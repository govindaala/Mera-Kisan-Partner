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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Marketplace UI State Machine
 * MarketplaceFragment.kt:120 ke when (state) ke sath 100% exhaustive aur perfectly aligned
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
}

/**
 * MERA KISAN Marketplace View Model
 * Free-First, Offline Cache aur StateFlow se reactive architecture
 */
class MarketplaceViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = ApiClient.getApiService(application)
    private val productDao = AppDatabase.getInstance(application).productDao()

    // 1. MarketplaceFragment.kt:117 ke collectLatest ke liye StateFlow (Exhaustive 4 States)
    private val _uiState = MutableStateFlow<MarketplaceUiState>(MarketplaceUiState.Loading)
    val uiState: StateFlow<MarketplaceUiState> = _uiState.asStateFlow()

    // 2. Backward compatibility LiveData
    private val _products = MutableLiveData<List<ProductDto>>()
    val products: LiveData<List<ProductDto>> get() = _products

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    // Filter states
    private var currentCategory: String? = null
    private var isSmallQuantityOnly: Boolean = false
    private var isOrganicOnly: Boolean = false
    private var currentSearchQuery: String = ""
    private var allLoadedProducts: List<ProductDto> = emptyList()

    fun loadMarketplaceProducts() {
        loadProducts(
            category = currentCategory,
            isSmallQuantityOnly = isSmallQuantityOnly,
            isOrganicOnly = isOrganicOnly
        )
    }

    fun setCategoryFilter(category: String?) {
        currentCategory = if (category.isNullOrBlank() || category.equals("all", ignoreCase = true)) null else category
        loadMarketplaceProducts()
    }

    fun setSmallQuantityFilter(enabled: Boolean) {
        isSmallQuantityOnly = enabled
        loadMarketplaceProducts()
    }

    fun setOrganicFilter(enabled: Boolean) {
        isOrganicOnly = enabled
        loadMarketplaceProducts()
    }

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
                _error.value = "Koi utpad uplabdh nahi hai."
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
