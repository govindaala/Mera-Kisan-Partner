// app/src/main/java/in/merakisan/app/core/network/model/ProductDto.kt
package `in`.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

/**
 * MERA KISAN Unified Produce Data Model
 * वित्तीय सुरक्षा: price_paise (Long) एवं दोनों वर्ज़न के फ़ील्ड्स का समेकन
 */
data class ProductDto(
    @SerializedName("product_id")
    val productId: String = "",

    @SerializedName("seller_uid")
    val sellerUid: String = "",

    @SerializedName("seller_name")
    val sellerName: String = "",

    @SerializedName("seller_phone")
    val sellerPhone: String? = null,

    @SerializedName("name")
    val name: String = "",

    @SerializedName("category")
    val category: String = "",

    @SerializedName("variety")
    val variety: String = "",

    @SerializedName("description")
    val description: String = "",

    @SerializedName("price_paise")
    val pricePaise: Long = 0L,

    @SerializedName("unit")
    val unit: String = "kg",

    @SerializedName("stock_quantity")
    val stockQuantity: Double = 0.0,

    @SerializedName("min_order_quantity")
    val minOrderQuantity: Double = 1.0,

    @SerializedName("village")
    val village: String = "",

    @SerializedName("district")
    val district: String = "",

    @SerializedName("state")
    val state: String = "Madhya Pradesh",

    @SerializedName("approx_lat")
    val approxLat: Double? = null,

    @SerializedName("approx_lng")
    val approxLng: Double? = null,

    @SerializedName("latitudeApprox")
    val latitudeApprox: Double? = null,

    @SerializedName("longitudeApprox")
    val longitudeApprox: Double? = null,

    @SerializedName("photos")
    val photos: List<String> = emptyList(),

    @SerializedName("image_urls")
    val imageUrls: List<String> = emptyList(),

    @SerializedName("video_url")
    val videoUrl: String? = null,

    @SerializedName("harvest_date")
    val harvestDate: String = "",

    @SerializedName("farming_type")
    val farmingType: String = "conventional",

    @SerializedName("verification_status")
    val verificationStatus: String = "unverified",

    @SerializedName("status")
    val status: String = "active",

    @SerializedName("created_at")
    val createdAt: String = "",

    @SerializedName("updated_at")
    val updatedAt: String = ""
)
