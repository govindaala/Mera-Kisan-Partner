// app/src/main/java/in/merakisan/app/core/network/model/ApiResponse.kt
package `in`.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

/**
 * MERA KISAN Standardized REST API Envelope
 */
data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: T? = null,

    @SerializedName("error")
    val error: ApiError? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("request_id")
    val requestId: String? = null
)

data class ApiError(
    @SerializedName("code")
    val code: String,

    @SerializedName("message")
    val message: String
)

data class AppConfigResponse(
    @SerializedName("payment_enabled")
    val paymentEnabled: Boolean = false,

    @SerializedName("boost_feature")
    val boostFeature: Boolean = false,

    @SerializedName("feature_flags")
    val featureFlags: Map<String, Any> = emptyMap(),

    @SerializedName("remote_config")
    val remoteConfig: Map<String, Any> = emptyMap()
)

/**
 * चेकआउट स्क्रीन द्वारा ऑर्डर निर्माण का अनुरोध मॉडल
 */
data class CreateOrderRequest(
    @SerializedName("product_id")
    val productId: String = "",

    @SerializedName("quantity")
    val quantity: Double = 0.0,

    @SerializedName("payment_method")
    val paymentMethod: String = "cod",

    @SerializedName("delivery_type")
    val deliveryType: String? = "pickup",

    @SerializedName("delivery_address")
    val deliveryAddress: String? = null,

    @SerializedName("notes")
    val notes: String? = null
)
