// app/src/main/java/in/merakisan/app/ui/marketplace/ProductDetailViewModel.kt
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

sealed class ProductDetailUiState {
    object Loading : ProductDetailUiState()
    data class Success(val product: ProductDto) : ProductDetailUiState()
    data class Error(val message: String) : ProductDetailUiState()
}

class ProductDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = ApiClient.getApiService(application)

    private val _uiState = MutableStateFlow<ProductDetailUiState>(ProductDetailUiState.Loading)
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    fun loadProduct(productId: String) {
        viewModelScope.launch {
            _uiState.value = ProductDetailUiState.Loading
            try {
                val response = apiService.getProductDetails(productId)
                if (response.isSuccessful && response.body()?.success == true) {
                    val product = response.body()?.data
                    if (product != null) {
                        _uiState.value = ProductDetailUiState.Success(product)
                    } else {
                        _uiState.value = ProductDetailUiState.Error("फसल का डेटा प्राप्त नहीं हुआ।")
                    }
                } else {
                    val msg = response.body()?.error?.message ?: "त्रुटि: HTTP ${response.code()}"
                    _uiState.value = ProductDetailUiState.Error(msg)
                }
            } catch (e: Exception) {
                _uiState.value = ProductDetailUiState.Error("नेटवर्क संपर्क विफल: ${e.localizedMessage}")
            }
        }
    }
}
