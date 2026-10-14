// app/src/main/java/in/merakisan/app/core/network/model/BuyerRequestDto.kt
package `in`.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

data class BuyerRequestDto(

    @SerializedName(
        value = "request_id",
        alternate = ["id", "_id", "requestId"]
    )
    val requestId: String = "",

    @SerializedName(
        value = "buyer_uid",
        alternate = ["buyerUid"]
    )
    val buyerUid: String = "",

    @SerializedName(
        value = "buyer_name",
        alternate = ["buyerName"]
    )
    val buyerName: String = "",

    @SerializedName(
        value = "buyer_phone",
        alternate = ["buyerPhone", "phone"]
    )
    val buyerPhone: String = "",

    @SerializedName(
        value = "product_name",
        alternate = ["productName"]
    )
    val productName: String = "",

    @SerializedName("category")
    val category: String = "grain",

    @SerializedName("variety")
    val variety: String? = null,

    @SerializedName("quantity")
    val quantity: Double = 1.0,

    @SerializedName("unit")
    val unit: String = "kg",

    @SerializedName(
        value = "target_price_paise",
        alternate = ["targetPricePaise"]
    )
    val targetPricePaise: Long? = null,

    @SerializedName("district")
    val district: String = "",

    @SerializedName("village")
    val village: String = "",

    @SerializedName(
        value = "required_by_date",
        alternate = ["requiredByDate"]
    )
    val requiredByDate: String? = null,

    @SerializedName(
        value = "notes",
        alternate = ["description"]
    )
    val notes: String? = null,

    @SerializedName("status")
    val status: String = "active",

    @SerializedName(
        value = "created_at",
        alternate = ["createdAt"]
    )
    val createdAt: Long? = null
)
