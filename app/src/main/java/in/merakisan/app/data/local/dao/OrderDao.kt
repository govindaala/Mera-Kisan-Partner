package `in`.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

data class OrderDto(
    @SerializedName("id", alternate = ["_id", "orderId", "order_id"])
    val id: String = "",

    @SerializedName("buyerUid", alternate = ["buyer_uid"])
    val buyerUid: String = "",

    @SerializedName("sellerUid", alternate = ["seller_uid"])
    val sellerUid: String = "",

    @SerializedName("productId", alternate = ["product_id"])
    val productId: String = "",

    @SerializedName("productName", alternate = ["product_name"])
    val productName: String = "",

    @SerializedName("quantity")
    val quantity: Double = 1.0,

    @SerializedName("unit")
    val unit: String = "kg",

    @SerializedName("totalAmountPaise", alternate = ["total_amount_paise", "totalAmount"])
    val totalAmountPaise: Long = 0L,

    @SerializedName("paymentStatus", alternate = ["payment_status"])
    val paymentStatus: String = "COD_PENDING",

    @SerializedName("orderStatus", alternate = ["order_status", "status"])
    val orderStatus: String = "CREATED",

    @SerializedName("deliveryAddress", alternate = ["delivery_address", "shippingAddress"])
    val deliveryAddress: String = "",

    @SerializedName("createdAt", alternate = ["created_at"])
    val createdAt: Long? = null
) {
    fun getFormattedTotal(): String {
        return "₹${totalAmountPaise / 100.0}"
    }
}
