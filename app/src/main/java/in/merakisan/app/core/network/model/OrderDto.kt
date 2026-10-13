// app/src/main/java/in/merakisan/app/core/network/model/OrderDto.kt
package `in`.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

data class OrderDto(

    @SerializedName(
        value = "id",
        alternate = [
            "_id",
            "orderId",
            "order_id"
        ]
    )
    val id: String = "",

    @SerializedName(
        value = "buyerUid",
        alternate = ["buyer_uid"]
    )
    val buyerUid: String = "",

    @SerializedName(
        value = "sellerUid",
        alternate = ["seller_uid"]
    )
    val sellerUid: String = "",

    @SerializedName(
        value = "productId",
        alternate = ["product_id"]
    )
    val productId: String = "",

    @SerializedName(
        value = "productName",
        alternate = ["product_name"]
    )
    val productName: String = "",

    @SerializedName("quantity")
    val quantity: Double = 1.0,

    @SerializedName("unit")
    val unit: String = "kg",

    @SerializedName(
        value = "totalAmountPaise",
        alternate = [
            "total_amount_paise",
            "totalAmount"
        ]
    )
    val totalAmountPaise: Long = 0L,

    @SerializedName(
        value = "paymentStatus",
        alternate = ["payment_status"]
    )
    val paymentStatus: String = "COD_PENDING",

    @SerializedName(
        value = "orderStatus",
        alternate = [
            "order_status",
            "status"
        ]
    )
    val orderStatus: String = "CREATED",

    @SerializedName(
        value = "deliveryAddress",
        alternate = [
            "delivery_address",
            "shippingAddress"
        ]
    )
    val deliveryAddress: String = "",

    @SerializedName(
        value = "created_at",
        alternate = ["createdAt"]
    )
    val createdAt: String = ""
) {

    val orderId: String
        get() = id

    fun getFormattedTotal(): String {
        return "₹${totalAmountPaise / 100.0}"
    }
}
