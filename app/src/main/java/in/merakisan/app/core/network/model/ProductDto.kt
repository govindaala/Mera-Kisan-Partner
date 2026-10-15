package `in`.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

data class ProductDto(
    @SerializedName("productId", alternate = ["id", "cropId", "_id"])
    val productId: String = "",

    @SerializedName("sellerUid", alternate = ["sellerId", "farmerUid", "farmerId"])
    val sellerUid: String = "",

    @SerializedName("sellerName", alternate = ["farmerName", "farmer_name"])
    val sellerName: String = "",

    @SerializedName("sellerPhone", alternate = ["farmerPhone", "phone", "mobile"])
    val sellerPhone: String? = null,

    @SerializedName("name", alternate = ["cropName", "productName", "title"])
    val name: String = "",

    @SerializedName("category", alternate = ["categoryId"])
    val category: String = "",

    @SerializedName("variety")
    val variety: String? = "",

    @SerializedName("description")
    val description: String? = "",

    @SerializedName("pricePaise", alternate = ["price_paise", "price"])
    val pricePaise: Long = 0L,

    @SerializedName("unit")
    val unit: String = "kg",

    @SerializedName("stockQuantity", alternate = ["stock", "stockKg", "quantity"])
    val stockQuantity: Double = 0.0,

    @SerializedName("minOrderQuantity", alternate = ["minOrder", "minimumOrderKg"])
    val minOrderQuantity: Double = 1.0,

    @SerializedName("village")
    val village: String? = "",

    @SerializedName("district")
    val district: String = "",

    @SerializedName("state")
    val state: String = "Madhya Pradesh",

    @SerializedName("approxLat")
    val approxLat: Double? = null,

    @SerializedName("approxLng")
    val approxLng: Double? = null,

    @SerializedName("latitudeApprox")
    val latitudeApprox: Double? = null,

    @SerializedName("longitudeApprox")
    val longitudeApprox: Double? = null,

    @SerializedName("photos")
    val photos: List<String> = emptyList(),

    @SerializedName("imageUrls")
    val imageUrls: List<String> = emptyList(),

    @SerializedName("videoUrl", alternate = ["video_url"])
    val videoUrl: String? = null,

    @SerializedName("harvestDate", alternate = ["harvest_date"])
    val harvestDate: String? = "",

    @SerializedName("farmingType", alternate = ["farming_type"])
    val farmingType: String = "conventional",

    @SerializedName("verificationStatus", alternate = ["verification_status"])
    val verificationStatus: String = "unverified",

    @SerializedName("status")
    val status: String = "active",

    @SerializedName("isFeatured", alternate = ["featured"])
    val isFeatured: Boolean = false,

    @SerializedName("boosted", alternate = ["isBoosted", "boostStatus"])
    val boosted: Boolean = false,

    @SerializedName("createdAt", alternate = ["created_at"])
    val createdAt: String? = "",

    @SerializedName("updatedAt", alternate = ["updated_at"])
    val updatedAt: String? = ""
) {
    @Suppress("USELESS_ELVIS")
    fun sanitize(): ProductDto {
        val safePhotos = (photos ?: emptyList()).filterNotNull().ifEmpty {
            (imageUrls ?: emptyList()).filterNotNull()
        }
        val safeLat = approxLat ?: latitudeApprox
        val safeLng = approxLng ?: longitudeApprox

        return copy(
            productId = (productId ?: "").trim(),
            sellerUid = (sellerUid ?: "").trim(),
            sellerName = (sellerName ?: "").ifBlank { "किसान" },
            sellerPhone = sellerPhone?.trim(),
            name = (name ?: "").ifBlank { "फसल / उत्पाद" },
            category = (category ?: "").ifBlank { "अन्य" },
            variety = (variety ?: "").trim(),
            description = (description ?: "").trim(),
            pricePaise = if (pricePaise < 0L) 0L else pricePaise,
            unit = (unit ?: "").ifBlank { "kg" },
            stockQuantity = if (stockQuantity < 0.0) 0.0 else stockQuantity,
            minOrderQuantity = if (minOrderQuantity <= 0.0) 1.0 else minOrderQuantity,
            village = (village ?: "").trim(),
            district = (district ?: "").trim(),
            state = (state ?: "").ifBlank { "Madhya Pradesh" },
            approxLat = safeLat,
            approxLng = safeLng,
            latitudeApprox = safeLat,
            longitudeApprox = safeLng,
            photos = safePhotos,
            imageUrls = safePhotos,
            videoUrl = videoUrl?.trim()?.takeIf { it.isNotEmpty() },
            harvestDate = (harvestDate ?: "").trim(),
            farmingType = (farmingType ?: "").ifBlank { "conventional" },
            verificationStatus = (verificationStatus ?: "").ifBlank { "unverified" },
            status = (status ?: "").ifBlank { "active" },
            createdAt = (createdAt ?: "").trim(),
            updatedAt = (updatedAt ?: "").trim()
        )
    }
}
