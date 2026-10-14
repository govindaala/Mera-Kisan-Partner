// app/src/main/java/in/merakisan/app/core/network/model/ProductDto.kt
package `in`.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

data class ProductDto(
    @SerializedName("productId", alternate = ["id", "_id", "cropId", "product_id", "crop_id"])
    val productId: String = "",

    @SerializedName("sellerUid", alternate = ["seller_uid", "farmerUid", "farmer_uid", "sellerId", "seller_id"])
    val sellerUid: String = "",

    @SerializedName("sellerName", alternate = ["seller_name", "farmerName", "farmer_name"])
    val sellerName: String? = "किसान साथी",

    @SerializedName("sellerPhone", alternate = ["seller_phone", "farmerPhone", "farmer_phone", "phone"])
    val sellerPhone: String? = "",

    @SerializedName("name", alternate = ["productName", "cropName", "product_name", "crop_name", "title"])
    val name: String = "",

    @SerializedName("category", alternate = ["categoryId", "category_id"])
    val category: String = "grain",

    @SerializedName("variety")
    val variety: String? = "देशी",

    @SerializedName("description")
    val description: String? = "",

    @SerializedName("pricePaise", alternate = ["price_paise", "price"])
    val pricePaise: Long = 0L,

    @SerializedName("stockQuantity", alternate = ["stock_quantity", "stockKg", "quantity", "stock"])
    val stockQuantity: Double = 0.0,

    @SerializedName("unit")
    val unit: String = "kg",

    @SerializedName("minOrderQuantity", alternate = ["minimumOrderQuantity", "min_order", "minimumOrderKg", "minOrder", "min_order_quantity"])
    val minOrderQuantity: Double = 1.0,

    @SerializedName("village")
    val village: String? = "",

    @SerializedName("district")
    val district: String = "",

    @SerializedName("state")
    val state: String? = "Madhya Pradesh",

    @SerializedName("latitudeApprox", alternate = ["latitude_approx", "lat"])
    val latitudeApprox: Double? = null,

    @SerializedName("longitudeApprox", alternate = ["longitude_approx", "lng", "lon"])
    val longitudeApprox: Double? = null,

    @SerializedName("photos", alternate = ["images"])
    val photos: List<String> = emptyList(),

    @SerializedName("imageUrls", alternate = ["image_urls"])
    val imageUrls: List<String> = emptyList(),

    @SerializedName("videos", alternate = ["videoUrls", "video_urls"])
    val videos: List<String> = emptyList(),

    @SerializedName("videoUrl", alternate = ["video_url"])
    val videoUrl: String? = null,

    @SerializedName("harvestDate", alternate = ["harvest_date"])
    val harvestDate: String? = null,

    @SerializedName("farmingType", alternate = ["farming_type"])
    val farmingType: String = "natural",

    @SerializedName("verificationStatus", alternate = ["verification_status"])
    val verificationStatus: String = "farmer_declared",

    @SerializedName("organicVerified", alternate = ["organic_verified"])
    val organicVerified: Boolean = false,

    @SerializedName("isFeatured", alternate = ["is_featured", "featured"])
    val isFeatured: Boolean = false,

    @SerializedName("boosted", alternate = ["is_boosted", "isBoosted"])
    val boosted: Boolean = false,

    @SerializedName("status")
    val status: String = "ACTIVE",

    @SerializedName("createdAt", alternate = ["created_at"])
    val createdAt: String? = null,

    @SerializedName("updatedAt", alternate = ["updated_at"])
    val updatedAt: String? = null
) {
    // सहायक प्रॉपर्टीज (बिना बैकिंग फ़ील्ड के, ताकि Gson में डुप्लिकेट की एरर न आए)
    val id: String
        get() = productId

    val minimumOrderQuantity: Double
        get() = minOrderQuantity

    fun getFormattedPrice(): String {
        val displayRupees = if (pricePaise >= 100L) pricePaise / 100.0 else pricePaise.toDouble()
        val safeUnit = (unit as String?)?.ifBlank { "kg" } ?: "kg"
        return "₹${String.format("\%.0f", displayRupees)} / $safeUnit"
    }

    fun isSmallQuantityAvailable(): Boolean {
        return stockQuantity in 0.5..25.0
    }

    @Suppress("USELESS_CAST", "UNCHECKED_CAST")
    fun sanitize(): ProductDto {
        val rawPhotos = (photos as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        val rawImageUrls = (imageUrls as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        val mergedImages = if (rawPhotos.isNotEmpty()) rawPhotos else rawImageUrls

        val rawVideos = (videos as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        val safeVideoUrl = (videoUrl as String?) ?: rawVideos.firstOrNull()

        return ProductDto(
            productId = (productId as String?) ?: "",
            sellerUid = (sellerUid as String?) ?: "",
            sellerName = (sellerName as String?)?.ifBlank { "किसान साथी" } ?: "किसान साथी",
            sellerPhone = (sellerPhone as String?) ?: "",
            name = (name as String?)?.ifBlank { "किसान उत्पाद" } ?: "किसान उत्पाद",
            category = (category as String?)?.ifBlank { "grain" } ?: "grain",
            variety = (variety as String?)?.ifBlank { "देशी" } ?: "देशी",
            description = (description as String?) ?: "",
            pricePaise = if (pricePaise < 0L) 0L else pricePaise,
            stockQuantity = if (stockQuantity.isNaN() || stockQuantity < 0.0) 0.0 else stockQuantity,
            unit = (unit as String?)?.ifBlank { "kg" } ?: "kg",
            minOrderQuantity = if (minOrderQuantity.isNaN() || minOrderQuantity <= 0.0) 1.0 else minOrderQuantity,
            village = (village as String?) ?: "",
            district = (district as String?) ?: "",
            state = (state as String?)?.ifBlank { "Madhya Pradesh" } ?: "Madhya Pradesh",
            latitudeApprox = latitudeApprox,
            longitudeApprox = longitudeApprox,
            photos = mergedImages,
            imageUrls = mergedImages,
            videos = rawVideos,
            videoUrl = safeVideoUrl,
            harvestDate = harvestDate,
            farmingType = (farmingType as String?)?.ifBlank { "natural" } ?: "natural",
            verificationStatus = (verificationStatus as String?)?.ifBlank { "farmer_declared" } ?: "farmer_declared",
            organicVerified = organicVerified,
            isFeatured = isFeatured,
            boosted = boosted,
            status = (status as String?)?.ifBlank { "ACTIVE" } ?: "ACTIVE",
            createdAt = (createdAt as String?) ?: "",
            updatedAt = (updatedAt as String?) ?: ""
        )
    }
}
