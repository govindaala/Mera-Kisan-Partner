// app/src/main/java/in/merakisan/app/ui/favorites/FavoritesViewModel.kt
package in.merakisan.app.ui.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import in.merakisan.app.core.network.model.FavoriteProductDto
import in.merakisan.app.core.network.model.FollowedFarmerDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FavoritesUiState {
    object Loading : FavoritesUiState()
    data class ProductsLoaded(val products: List<FavoriteProductDto>) : FavoritesUiState()
    data class FarmersLoaded(val farmers: List<FollowedFarmerDto>) : FavoritesUiState()
    object Empty : FavoritesUiState()
    data class Error(val message: String) : FavoritesUiState()
}

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<FavoritesUiState>(FavoritesUiState.Loading)
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private val cachedProducts = mutableListOf<FavoriteProductDto>()
    private val cachedFarmers = mutableListOf<FollowedFarmerDto>()
    private var currentTab: Int = 0 // 0 = Products, 1 = Farmers

    init {
        loadInitialData()
    }

    fun selectTab(tabPosition: Int) {
        currentTab = tabPosition
        refreshTabUi()
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = FavoritesUiState.Loading
            delay(400) // सहज ट्रांजिशन
            if (cachedProducts.isEmpty() && cachedFarmers.isEmpty()) {
                seedInitialData()
            }
            refreshTabUi()
        }
    }

    private fun refreshTabUi() {
        if (currentTab == 0) {
            if (cachedProducts.isEmpty()) {
                _uiState.value = FavoritesUiState.Empty
            } else {
                _uiState.value = FavoritesUiState.ProductsLoaded(cachedProducts.toList())
            }
        } else {
            if (cachedFarmers.isEmpty()) {
                _uiState.value = FavoritesUiState.Empty
            } else {
                _uiState.value = FavoritesUiState.FarmersLoaded(cachedFarmers.toList())
            }
        }
    }

    fun removeProduct(product: FavoriteProductDto) {
        cachedProducts.removeAll { it.favoriteId == product.favoriteId }
        refreshTabUi()
    }

    fun unfollowFarmer(farmer: FollowedFarmerDto) {
        cachedFarmers.removeAll { it.followId == farmer.followId }
        refreshTabUi()
    }

    private fun seedInitialData() {
        cachedProducts.add(
            FavoriteProductDto(
                favoriteId = "fav_101",
                productId = "prod_soy_01",
                name = "देशी सोयाबीन",
                category = "अनाज व तिलहन",
                variety = "JS 335",
                pricePaise = 4600, // ₹46.00
                unit = "kg",
                stockQuantity = 20.0,
                sellerName = "रामलाल पाटीदार",
                village = "बर्ड़िया अमरा",
                district = "मंदसौर",
                photoUrl = null,
                verificationStatus = "verified_farmer",
                savedAt = "कल"
            )
        )
        cachedFarmers.add(
            FollowedFarmerDto(
                followId = "fol_201",
                farmerUid = "farmer_ramlal_01",
                name = "रामलाल पाटीदार",
                phone = "9826000000",
                village = "बर्ड़िया अमरा",
                district = "मंदसौर",
                activeCropsCount = 3,
                rating = 4.8,
                verificationStatus = "verified_farmer",
                followedAt = "2 अक्टूबर 2026"
            )
        )
    }
}
