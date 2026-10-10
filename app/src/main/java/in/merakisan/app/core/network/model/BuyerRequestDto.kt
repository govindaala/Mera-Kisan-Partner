// app/src/main/java/in/merakisan/app/core/network/model/BuyerRequestDto.kt
package in.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

/**
 * MERA KISAN Buyer Demand Board Data Contract
 * ग्राहक की मांग ("मुझे चाहिए") का डेटा मॉडल
 */
data class BuyerRequestDto(
    @SerializedName("request_id")
    val requestId: String,

    @SerializedName("buyer_uid")
    val buyerUid: String,

    @SerializedName("buyer_name")
    val buyerName: String,

    @SerializedName("buyer_phone")
    val buyerPhone: String? = null,

    @SerializedName("product_name")
    val productName: String,

    @SerializedName("category")
    val category: String,

    @SerializedName("variety")
    val variety: String? = null,

    @SerializedName("quantity")
    val quantity: Double,

    @SerializedName("unit")
    val unit: String,

    // वित्तीय मान केवल पूर्णांक पैसे में (Floating-point त्रुटि शून्य)
    @SerializedName("target_price_paise")
    val targetPricePaise: Long? = null,

    @SerializedName("village")
    val village: String? = null,

    @SerializedName("district")
    val district: String,

    @SerializedName("required_date")
    val requiredDate: String? = null,

    @SerializedName("notes")
    val notes: String? = null,

    @SerializedName("status")
    val status: String = "ACTIVE", // ACTIVE, FULFILLED, EXPIRED, CANCELLED

    @SerializedName("offers_count")
    val offersCount: Int = 0,

    @SerializedName("created_at")
    val createdAt: String? = null
)

/**
 * मांग दर्ज करने का Request Payload
 */
data class CreateBuyerRequestPayload(
    @SerializedName("product_name")
    val productName: String,

    @SerializedName("category")
    val category: String,

    @SerializedName("variety")
    val variety: String? = null,

    @SerializedName("quantity")
    val quantity: Double,

    @SerializedName("unit")
    val unit: String,

    @SerializedName("target_price_paise")
    val targetPricePaise: Long? = null,

    @SerializedName("district")
    val district: String,

    @SerializedName("village")
    val village: String? = null,

    @SerializedName("required_date")
    val requiredDate: String? = null,

    @SerializedName("notes")
    val notes: String? = null
)
