// app/src/main/java/in/merakisan/app/ui/marketplace/MarketplaceViewModel.kt
package in.merakisan.app.ui.marketplace

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import in.merakisan.app.core.network.ApiClient
import in.merakisan.app.core.network.model.ProductDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MarketplaceUiState {
    object Loading : MarketplaceUiState()
    data class Success(val products: List<ProductDto>) : MarketplaceUiState()
    data class Error(val message: String) : MarketplaceUiState()
    object Empty : MarketplaceUiState()
}

class MarketplaceViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = ApiClient.getApiService(application)

    private val _uiState = MutableStateFlow<MarketplaceUiState>(MarketplaceUiState.Loading)
    val uiState: StateFlow<MarketplaceUiState> = _uiState.asStateFlow()

    private var allFetchedProducts: List<ProductDto> = emptyList()

    // फ़िल्टर स्टेट्स
    private var currentCategory: String? = null
    private var isSmallQuantityOnly: Boolean = false
    private var isOrganicOnly: Boolean = false
    private var currentSearchQuery: String = ""

    init {
        loadMarketplaceProducts()
    }

    fun loadMarketplaceProducts() {
        viewModelScope.launch {
            _uiState.value = MarketplaceUiState.Loading
            try {
                val response = apiService.getProducts(
                    category = currentCategory,
                    maxQuantity = if (isSmallQuantityOnly) 25.0 else null,
                    organicOnly = if (isOrganicOnly) true else null,
                    limit = 50,
                    page = 1
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    val products = response.body()?.data ?: emptyList()
                    allFetchedProducts = products
                    applyLocalFilters()
                } else {
                    val errorMsg = response.body()?.error?.message ?: "फसलें लोड करने में त्रुटि (HTTP ${response.code()})"
                    _uiState.value = MarketplaceUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _uiState.value = MarketplaceUiState.Error("नेटवर्क संपर्क विफल: ${e.localizedMessage}")
            }
        }
    }

    fun setSmallQuantityFilter(enabled: Boolean) {
        isSmallQuantityOnly = enabled
        loadMarketplaceProducts()
    }

    fun setOrganicFilter(enabled: Boolean) {
        isOrganicOnly = enabled
        loadMarketplaceProducts()
    }

    fun setCategoryFilter(category: String?) {
        currentCategory = category
        loadMarketplaceProducts()
    }

    fun searchProducts(query: String) {
        currentSearchQuery = query.trim().lowercase()
        applyLocalFilters()
    }

    private fun applyLocalFilters() {
        val filtered = allFetchedProducts.filter { product ->
            val matchesQuery = currentSearchQuery.isBlank() ||
                    product.name.lowercase().contains(currentSearchQuery) ||
                    (product.variety?.lowercase()?.contains(currentSearchQuery) == true) ||
                    product.sellerName.lowercase().contains(currentSearchQuery) ||
                    product.district.lowercase().contains(currentSearchQuery)

            matchesQuery
        }

        if (filtered.isEmpty()) {
            _uiState.value = MarketplaceUiState.Empty
        } else {
            _uiState.value = MarketplaceUiState.Success(filtered)
        }
    }
}
