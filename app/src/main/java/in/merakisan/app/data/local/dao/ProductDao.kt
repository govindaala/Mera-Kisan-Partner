package `in`.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

data class ProductDto(
    @SerializedName("id", alternate = ["_id", "productId", "cropId"])
    val id: String = "",

    @SerializedName("sellerUid", alternate = ["seller_uid", "farmerUid", "farmer_uid"])
    val sellerUid: String = "",

    @SerializedName("name", alternate = ["productName", "cropName"])
    val name: String = "",

    @SerializedName("category")
    val category: String = "grain",

    @SerializedName("variety")
    val variety: String = "desi",

    @SerializedName("description")
    val description: String = "",

    @SerializedName("pricePaise", alternate = ["price_paise", "price"])
    val pricePaise: Long = 0L,

    @SerializedName("stockQuantity", alternate = ["stock_quantity", "stockKg", "quantity"])
    val stockQuantity: Double = 0.0,

    @SerializedName("unit")
    val unit: String = "kg",

    @SerializedName("minimumOrderQuantity", alternate = ["min_order", "minimumOrderKg"])
    val minimumOrderQuantity: Double = 1.0,

    @SerializedName("harvestDate", alternate = ["harvest_date"])
    val harvestDate: String? = null,

    @SerializedName("village")
    val village: String = "",

    @SerializedName("district")
    val district: String = "",

    @SerializedName("photos")
    val photos: List<String> = emptyList(),

    @SerializedName("videos")
    val videos: List<String> = emptyList(),

    @SerializedName("verificationStatus", alternate = ["verification_status"])
    val verificationStatus: String = "farmer_declared",

    @SerializedName("organicVerified", alternate = ["organic_verified"])
    val organicVerified: Boolean = false,

    @SerializedName("status")
    val status: String = "ACTIVE",

    @SerializedName("createdAt", alternate = ["created_at"])
    val createdAt: Long? = null
) {
    fun getFormattedPrice(): String {
        return "₹${pricePaise / 100.0}"
    }

    fun isSmallQuantityAvailable(): Boolean {
        return stockQuantity in 1.0..25.0
    }
}
