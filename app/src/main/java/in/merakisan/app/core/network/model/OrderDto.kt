// app/src/main/java/in/merakisan/app/core/network/model/OrderDto.kt
package in.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

/**
 * MERA KISAN Complete Order Data Model
 * वित्तीय सुरक्षा: Integer Paise एवं स्टेट मशीन अनुबंध
 */
data class OrderDto(
    @SerializedName("order_id")
    val orderId: String,

    @SerializedName("product_id")
    val productId: String,

    @SerializedName("product_name")
    val productName: String,

    @SerializedName("seller_uid")
    val sellerUid: String,

    @SerializedName("seller_name")
    val sellerName: String,

    @SerializedName("seller_phone")
    val sellerPhone: String? = null,

    @SerializedName("buyer_uid")
    val buyerUid: String,

    @SerializedName("buyer_name")
    val buyerName: String,

    @SerializedName("buyer_phone")
    val buyerPhone: String? = null,

    @SerializedName("quantity")
    val quantity: Double,

    @SerializedName("unit")
    val unit: String = "kg",

    // वित्तीय सुरक्षा: पूर्णांक पैसे (Integer Paise)
    @SerializedName("total_amount_paise")
    val totalAmountPaise: Long,

    @SerializedName("order_status")
    val orderStatus: String, // PLACED, ACCEPTED, PROCESSING, IN_TRANSIT, DELIVERED, COMPLETED, DISPUTED, CANCELLED

    @SerializedName("payment_status")
    val paymentStatus: String, // COD_PENDING, PAID, ESCROW_LOCKED, REFUNDED

    @SerializedName("delivery_address")
    val deliveryAddress: String = "",

    @SerializedName("created_at")
    val createdAt: String = ""
)
