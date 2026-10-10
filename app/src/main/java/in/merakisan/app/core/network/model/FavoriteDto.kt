// app/src/main/java/in/merakisan/app/core/network/model/FavoriteDto.kt
package in.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

/**
 * MERA KISAN Favorites & Follows Data Model
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
 * पसंदीदा फसल कार्ड्स के UI एडॉप्टर हेतु पूर्ण मॉडल
 * वित्तीय सुरक्षा: price_paise (Long)
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
