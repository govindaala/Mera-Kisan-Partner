// app/src/main/java/in/merakisan/app/core/network/model/FavoriteDto.kt
package in.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

/**
 * MERA KISAN Favorites & Follow Data Models
 */
data class FavoriteProductDto(
    @SerializedName("favorite_id")
    val favoriteId: String,

    @SerializedName("product_id")
    val productId: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("category")
    val category: String,

    @SerializedName("variety")
    val variety: String?,

    // वित्तीय सुरक्षा: पूर्णांक पैसे (Integer Paise)
    @SerializedName("price_paise")
    val pricePaise: Long,

    @SerializedName("unit")
    val unit: String,

    @SerializedName("stock_quantity")
    val stockQuantity: Double,

    @SerializedName("seller_name")
    val sellerName: String,

    @SerializedName("village")
    val village: String?,

    @SerializedName("district")
    val district: String,

    @SerializedName("photo_url")
    val photoUrl: String?,

    @SerializedName("verification_status")
    val verificationStatus: String,

    @SerializedName("saved_at")
    val savedAt: String
)

data class FollowedFarmerDto(
    @SerializedName("follow_id")
    val followId: String,

    @SerializedName("farmer_uid")
    val farmerUid: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("phone")
    val phone: String?,

    @SerializedName("village")
    val village: String?,

    @SerializedName("district")
    val district: String,

    @SerializedName("active_crops_count")
    val activeCropsCount: Int,

    @SerializedName("rating")
    val rating: Double,

    @SerializedName("verification_status")
    val verificationStatus: String,

    @SerializedName("followed_at")
    val followedAt: String
)
