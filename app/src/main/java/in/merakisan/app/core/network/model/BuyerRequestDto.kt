package `in`.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

data class BuyerRequestDto(
    @SerializedName("id", alternate = ["_id", "requestId"])
    val id: String = "",

    @SerializedName("buyerUid", alternate = ["buyer_uid"])
    val buyerUid: String = "",

    @SerializedName("buyerName", alternate = ["buyer_name"])
    val buyerName: String = "",

    @SerializedName("productName", alternate = ["product_name"])
    val productName: String = "",

    @SerializedName("category")
    val category: String = "grain",

    @SerializedName("variety")
    val variety: String = "",

    @SerializedName("quantity")
    val quantity: Double = 1.0,

    @SerializedName("unit")
    val unit: String = "kg",

    @SerializedName("targetPricePaise", alternate = ["target_price_paise"])
    val targetPricePaise: Long? = null,

    @SerializedName("district")
    val district: String = "",

    @SerializedName("village")
    val village: String = "",

    @SerializedName("requiredByDate", alternate = ["required_by_date"])
    val requiredByDate: String? = null,

    @SerializedName("description")
    val description: String = "",

    @SerializedName("status")
    val status: String = "ACTIVE",

    @SerializedName("createdAt", alternate = ["created_at"])
    val createdAt: Long? = null
)
