// app/src/main/java/in/merakisan/app/core/network/model/ProductDto.kt
package `in`.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

data class ProductDto(
    @SerializedName("id", alternate = ["_id", "productId", "cropId", "product_id", "crop_id"])
    val id: String = "",

    @SerializedName("sellerUid", alternate = ["seller_uid", "farmerUid", "farmer_uid", "sellerId"])
    val sellerUid: String = "",

    @SerializedName("name", alternate = ["productName", "cropName", "product_name", "crop_name", "title"])
    val name: String = "",

    @SerializedName("category", alternate = ["categoryId"])
    val category: String = "grain",

    @SerializedName("variety")
    val variety: String = "desi",

    @SerializedName("description")
    val description: String = "",

    @SerializedName("pricePaise", alternate = ["price_paise", "price"])
    val pricePaise: Long = 0L,

    @SerializedName("stockQuantity", alternate = ["stock_quantity", "stockKg", "quantity", "stock"])
    val stockQuantity: Double = 0.0,

    @SerializedName("unit")
    val unit: String = "kg",

    @SerializedName("minimumOrderQuantity", alternate = ["min_order", "minimumOrderKg", "minOrder"])
    val minimumOrderQuantity: Double = 1.0,

    @SerializedName("harvestDate", alternate = ["harvest_date"])
    val harvestDate: String? = null,

    @SerializedName("village")
    val village: String = "",

    @SerializedName("district")
    val district: String = "",

    @SerializedName("photos", alternate = ["images", "imageUrls"])
    val photos: List<String> = emptyList(),

    @SerializedName("videos", alternate = ["videoUrls"])
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
        val displayRupees = if (pricePaise >= 100L) pricePaise / 100.0 else pricePaise.toDouble()
        val safeUnit = (unit as String?)?.ifBlank { "kg" } ?: "kg"
        return "₹${String.format("\%.0f", displayRupees)} / $safeUnit"
    }

    fun isSmallQuantityAvailable(): Boolean {
        return stockQuantity in 0.5..25.0
    }

    /**
     * Gson Java Reflection dwara inject kiye gaye kisi bhi null field ko
     * safe default value me badalta hai taaki UI thread par NullPointerException na aaye.
     */
    @Suppress("USELESS_CAST", "UNCHECKED_CAST")
    fun sanitize(): ProductDto {
        val rawPhotos = (photos as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        val rawVideos = (videos as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

        return ProductDto(
            id = (id as String?) ?: "",
            sellerUid = (sellerUid as String?) ?: "",
            name = (name as String?)?.ifBlank { "किसान उत्पाद" } ?: "किसान उत्पाद",
            category = (category as String?)?.ifBlank { "grain" } ?: "grain",
            variety = (variety as String?)?.ifBlank { "देशी" } ?: "देशी",
            description = (description as String?) ?: "",
            pricePaise = if (pricePaise < 0L) 0L else pricePaise,
            stockQuantity = if (stockQuantity.isNaN() || stockQuantity < 0.0) 0.0 else stockQuantity,
            unit = (unit as String?)?.ifBlank { "kg" } ?: "kg",
            minimumOrderQuantity = if (minimumOrderQuantity.isNaN() || minimumOrderQuantity <= 0.0) 1.0 else minimumOrderQuantity,
            harvestDate = harvestDate,
            village = (village as String?) ?: "",
            district = (district as String?) ?: "",
            photos = rawPhotos,
            videos = rawVideos,
            verificationStatus = (verificationStatus as String?)?.ifBlank { "farmer_declared" } ?: "farmer_declared",
            organicVerified = organicVerified,
            status = (status as String?)?.ifBlank { "ACTIVE" } ?: "ACTIVE",
            createdAt = createdAt ?: System.currentTimeMillis()
        )
    }
}
