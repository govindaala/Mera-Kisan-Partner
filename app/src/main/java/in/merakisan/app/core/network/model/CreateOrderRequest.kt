// app/src/main/java/in/merakisan/app/core/network/model/CreateOrderRequest.kt
package `in`.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

data class CreateOrderRequest(
    @SerializedName("productId", alternate = ["product_id"])
    val productId: String = "",

    @SerializedName("sellerUid", alternate = ["seller_uid", "farmerUid", "farmer_uid"])
    val sellerUid: String = "",

    @SerializedName("buyerUid", alternate = ["buyer_uid"])
    val buyerUid: String = "",

    @SerializedName("quantity")
    val quantity: Double = 1.0,

    @SerializedName("unit")
    val unit: String = "kg",

    @SerializedName("totalAmountPaise", alternate = ["total_amount_paise", "totalAmount"])
    val totalAmountPaise: Long = 0L,

    @SerializedName("paymentMethod", alternate = ["payment_method"])
    val paymentMethod: String = "cod",

    @SerializedName("deliveryAddress", alternate = ["delivery_address", "address"])
    val deliveryAddress: String = "",

    @SerializedName("buyerPhone", alternate = ["buyer_phone", "phone"])
    val buyerPhone: String = ""
)
