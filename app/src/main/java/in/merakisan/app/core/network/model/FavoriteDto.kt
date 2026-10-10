// app/src/main/java/in/merakisan/app/core/network/model/FavoriteDto.kt
package `in`.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

/**
 * पसंदीदा फसल या किसान का रिमोट संदर्भ
 */
data class FavoriteDto(
    @SerializedName("favorite_id")
    val favoriteId: String = "",

    @SerializedName("user_uid")
    val userUid: String = "",

    @SerializedName("target_id")
    val targetId: String = "",

    @SerializedName("type")
    val type: String = "product", // product, farmer

    @SerializedName("created_at")
    val createdAt: String = ""
)

/**
 * पसंदीदा फसल कार्ड्स हेतु पूर्ण UI मॉडल
 */
data class FavoriteProductDto(
    @SerializedName("favorite_id")
    val favoriteId: String = "",

    @SerializedName("product_id")
    val productId: String = "",

    @SerializedName("name")
    val name: String = "",

    @SerializedName("variety")
    val variety: String? = null,

    @SerializedName("price_paise")
    val pricePaise: Long = 0L,

    @SerializedName("unit")
    val unit: String = "kg",

    @SerializedName("stock_quantity")
    val stockQuantity: Double = 0.0,

    @SerializedName("seller_name")
    val sellerName: String = "",

    @SerializedName("village")
    val village: String? = null,

    @SerializedName("district")
    val district: String = "",

    @SerializedName("image_url")
    val imageUrl: String? = null
)

/**
 * अनुगमित किसान कार्ड्स हेतु पूर्ण डेटा मॉडल
 * FavoritesViewModel और FollowedFarmerAdapter के साथ 100% संगत
 */
data class FollowedFarmerDto(
    @SerializedName("farmer_id")
    val farmerId: String = "",

    @SerializedName("farmer_uid")
    val farmerUid: String = "",

    @SerializedName("name")
    val name: String = "",

    @SerializedName("farmer_name")
    val farmerName: String = "",

    @SerializedName("phone")
    val phone: String? = null,

    @SerializedName("farmer_phone")
    val farmerPhone: String? = null,

    @SerializedName("village")
    val village: String? = null,

    @SerializedName("district")
    val district: String? = null,

    @SerializedName("state")
    val state: String? = "Madhya Pradesh",

    @SerializedName("photo_url")
    val photoUrl: String? = null,

    @SerializedName("image_url")
    val imageUrl: String? = null,

    @SerializedName("rating")
    val rating: Double = 5.0,

    @SerializedName("total_products")
    val totalProducts: Int = 0,

    @SerializedName("active_products_count")
    val activeProductsCount: Int = 0,

    @SerializedName("followed_at")
    val followedAt: String = ""
)
