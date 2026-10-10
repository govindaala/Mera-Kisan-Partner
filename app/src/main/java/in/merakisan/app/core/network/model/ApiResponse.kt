// app/src/main/java/in/merakisan/app/core/network/model/ApiResponse.kt
package in.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

/**
 * MERA KISAN Standard API Response Envelope
 * बैकएंड से आने वाले हर JSON रिस्पॉन्स का बेस स्ट्रक्चर
 */
data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: T? = null,

    @SerializedName("error")
    val error: ApiError? = null,

    @SerializedName("timestamp")
    val timestamp: String? = null
)

data class ApiError(
    @SerializedName("code")
    val code: String,

    @SerializedName("message")
    val message: String
)

/**
 * Public Remote Config & 40 Feature Flags Contract (/v1/config)
 */
data class AppConfigResponse(
    @SerializedName("app_name")
    val appName: String,

    @SerializedName("tagline")
    val tagline: String,

    @SerializedName("min_app_version")
    val minAppVersion: String,

    @SerializedName("maintenance_mode")
    val maintenanceMode: MaintenanceModeConfig,

    @SerializedName("features")
    val features: Map<String, Boolean>
)

data class MaintenanceModeConfig(
    @SerializedName("active")
    val active: Boolean,

    @SerializedName("message")
    val message: String? = null
)

/**
 * Product Data Model (Integer Paise per Master Spec)
 */
data class ProductDto(
    @SerializedName("product_id")
    val productId: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("category")
    val category: String,

    @SerializedName("variety")
    val variety: String? = null,

    @SerializedName("description")
    val description: String? = null,

    // वित्तीय मान केवल पूर्णांक पैसे में (Floating-point त्रुटि शून्य)
    @SerializedName("price_paise")
    val pricePaise: Long,

    @SerializedName("stock_quantity")
    val stockQuantity: Double,

    @SerializedName("unit")
    val unit: String,

    @SerializedName("min_order_quantity")
    val minOrderQuantity: Double? = 1.0,

    @SerializedName("seller_uid")
    val sellerUid: String,

    @SerializedName("seller_name")
    val sellerName: String,

    @SerializedName("seller_phone")
    val sellerPhone: String? = null,

    @SerializedName("village")
    val village: String? = null,

    @SerializedName("district")
    val district: String,

    @SerializedName("state")
    val state: String? = null,

    // प्राइवेसी नियम: किसान का सटीक GPS नहीं, अनुमानित निर्देशांक
    @SerializedName("latitude_approx")
    val latitudeApprox: Double? = null,

    @SerializedName("longitude_approx")
    val longitudeApprox: Double? = null,

    @SerializedName("verification_status")
    val verificationStatus: String, // unverified, farmer_declared, verified_farmer, organic_certified

    @SerializedName("harvest_date")
    val harvestDate: String? = null,

    @SerializedName("photos")
    val photos: List<String> = emptyList(),

    @SerializedName("is_featured")
    val isFeatured: Boolean = false,

    @SerializedName("boosted")
    val boosted: Boolean = false,

    @SerializedName("status")
    val status: String = "active",

    @SerializedName("created_at")
    val createdAt: String? = null
)

/**
 * Order Creation Request Body
 */
data class CreateOrderRequest(
    @SerializedName("product_id")
    val productId: String,

    @SerializedName("quantity")
    val quantity: Double,

    @SerializedName("delivery_type")
    val deliveryType: String, // farmer_delivery, buyer_pickup

    @SerializedName("payment_method")
    val paymentMethod: String, // cod, online, token

    @SerializedName("delivery_address")
    val deliveryAddress: String? = null,

    @SerializedName("notes")
    val notes: String? = null
)

/**
 * Order Data Model
 */
