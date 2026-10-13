// app/src/main/java/in/merakisan/app/core/network/model/CreateOrderRequest.kt
package `in`.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

data class CreateOrderRequest(

    @SerializedName(
        value = "product_id",
        alternate = ["productId"]
    )
    val productId: String = "",

    @SerializedName(
        value = "seller_uid",
        alternate = [
            "sellerUid",
            "farmerUid",
            "farmer_uid"
        ]
    )
    val sellerUid: String = "",

    @SerializedName(
        value = "buyer_uid",
        alternate = ["buyerUid"]
    )
    val buyerUid: String = "",

    @SerializedName("quantity")
    val quantity: Double = 1.0,

    @SerializedName("unit")
    val unit: String = "kg",

    @SerializedName(
        value = "total_amount_paise",
        alternate = [
            "totalAmountPaise",
            "totalAmount"
        ]
    )
    val totalAmountPaise: Long = 0L,

    @SerializedName(
        value = "payment_method",
        alternate = ["paymentMethod"]
    )
    val paymentMethod: String = "cod",

    @SerializedName(
        value = "delivery_address",
        alternate = [
            "deliveryAddress",
            "address"
        ]
    )
    val deliveryAddress: String = "",

    @SerializedName(
        value = "buyer_phone",
        alternate = [
            "buyerPhone",
            "phone"
        ]
    )
    val buyerPhone: String = "",

    @SerializedName(
        value = "idempotency_key",
        alternate = ["idempotencyKey"]
    )
    val idempotencyKey: String = "",

    @SerializedName(
        value = "delivery_type",
        alternate = ["deliveryType"]
    )
    val deliveryType: String = "pickup"
)
