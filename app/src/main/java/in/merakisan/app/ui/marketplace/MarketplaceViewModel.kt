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

class MarketplaceViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = ApiClient.getApiService(application)
    private val productDao = AppDatabase.getInstance(application).productDao()

    private val _products = MutableLiveData<List<ProductDto>>()
    val products: LiveData<List<ProductDto>> get() = _products

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private var allLoadedProducts: List<ProductDto> = emptyList()

    fun loadProducts(
        category: String? = null,
        isSmallQuantityOnly: Boolean = false,
        isOrganicOnly: Boolean = false
    ) {
        _isLoading.value = true
        _error.value = null

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
            }
        }
    }

    fun filterProducts(query: String) {
        val q = query.trim().lowercase()
        if (q.isBlank()) {
            _products.value = allLoadedProducts
            return
        }

        // लाइन 92 सेफ़्टी: सभी स्ट्रिंग्स पर .orEmpty() और सेफ़ कॉल
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
    }
}
