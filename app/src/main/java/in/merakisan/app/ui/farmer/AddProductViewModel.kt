// app/src/main/java/in/merakisan/app/ui/farmer/AddProductViewModel.kt
package in.merakisan.app.ui.farmer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import in.merakisan.app.core.network.ApiClient
import in.merakisan.app.core.network.model.ProductDto
import in.merakisan.app.core.security.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AddProductUiState {
    object Idle : AddProductUiState()
    object Loading : AddProductUiState()
    data class Success(val createdProduct: ProductDto) : AddProductUiState()
    data class Error(val message: String) : AddProductUiState()
}

class AddProductViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = ApiClient.getApiService(application)

    private val _uiState = MutableStateFlow<AddProductUiState>(AddProductUiState.Idle)
    val uiState: StateFlow<AddProductUiState> = _uiState.asStateFlow()

    fun publishProduct(
        name: String,
        category: String,
        variety: String?,
        priceRupees: Double,
        stock: Double,
        unit: String,
        minOrder: Double,
        harvestDate: String?,
        farmingMethod: String,
        village: String?,
        district: String,
        description: String?,
        photoUrl: String?
    ) {
        // 1. इनपुट वैलिडेशन
        if (name.isBlank()) {
            _uiState.value = AddProductUiState.Error("कृपया फसल का नाम दर्ज करें")
            return
        }
        if (priceRupees <= 0.0) {
            _uiState.value = AddProductUiState.Error("मान्य दर (कीमत) दर्ज करें")
            return
        }
        if (stock <= 0.0) {
            _uiState.value = AddProductUiState.Error("उपलब्ध मात्रा दर्ज करें")
            return
        }
        if (district.isBlank()) {
            _uiState.value = AddProductUiState.Error("ज़िला दर्ज करना अनिवार्य है")
            return
        }

        viewModelScope.launch {
            _uiState.value = AddProductUiState.Loading
            try {
                val context = getApplication<Application>()
                val sellerUid = SessionManager.getUserUid(context) ?: "farmer_${System.currentTimeMillis()}"

                // वित्तीय सुरक्षा: फ्लोटिंग पॉइंट एरर खत्म करने के लिए पैसे में रूपांतरण
                val pricePaise = (priceRupees * 100).toLong()

                val productDto = ProductDto(
                    productId = "",
                    name = name.trim(),
                    category = category.trim(),
                    variety = variety?.trim()?.ifBlank { null },
                    description = description?.trim()?.ifBlank { null },
                    pricePaise = pricePaise,
                    stockQuantity = stock,
                    unit = unit.trim(),
                    minOrderQuantity = if (minOrder > 0) minOrder else 1.0,
                    sellerUid = sellerUid,
                    sellerName = "किसान साथी",
                    sellerPhone = null,
                    village = village?.trim()?.ifBlank { null },
                    district = district.trim(),
                    state = "मध्य प्रदेश",
                    latitudeApprox = null,
                    longitudeApprox = null,
                    verificationStatus = farmingMethod,
                    harvestDate = harvestDate?.ifBlank { null },
                    photos = if (!photoUrl.isNullOrBlank()) listOf(photoUrl) else emptyList(),
                    isFeatured = false,
                    boosted = false,
                    status = "active",
                    createdAt = null
                )

                val response = apiService.createProduct(productDto)
                if (response.isSuccessful && response.body()?.success == true) {
                    val result = response.body()?.data
                    if (result != null) {
                        _uiState.value = AddProductUiState.Success(result)
                    } else {
                        _uiState.value = AddProductUiState.Error("सर्वर ने रिक्त उत्तर दिया")
                    }
                } else {
                    val errorMsg = response.body()?.error?.message ?: "फसल प्रकाशित नहीं हो सकी (HTTP ${response.code()})"
                    _uiState.value = AddProductUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _uiState.value = AddProductUiState.Error("नेटवर्क संपर्क त्रुटि: ${e.localizedMessage}")
            }
        }
    }

    fun resetState() {
        _uiState.value = AddProductUiState.Idle
    }
}
