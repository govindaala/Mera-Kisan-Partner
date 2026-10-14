// app/src/main/java/in/merakisan/app/core/network/model/BuyerRequestDto.kt
package `in`.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

data class BuyerRequestDto(

    @SerializedName(
        value = "request_id",
        alternate = ["requestId", "id", "_id"]
    )
    val id: String = "",

    @SerializedName(
        value = "buyer_uid",
        alternate = ["buyerUid", "buyer_uid"]
    )
    val buyerUid: String = "",

    @SerializedName(
        value = "buyer_name",
        alternate = ["buyerName"]
    )
    val buyerName: String = "",

    @SerializedName(
        value = "product_name",
        alternate = ["productName"]
    )
    val productName: String = "",

    @SerializedName("category")
    val category: String = "grain",

    @SerializedName("variety")
    val variety: String = "",

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
        alternate = ["requiredByDate", "requiredDate"]
    )
    val requiredByDate: String? = null,

    @SerializedName(
        value = "notes",
        alternate = ["description", "note"]
    )
    val description: String = "",

    @SerializedName("status")
    val status: String = "active",

    @SerializedName(
        value = "created_at",
        alternate = ["createdAt"]
    )
    val createdAt: Long? = null,

    @SerializedName(
        value = "offers_count",
        alternate = ["offersCount"]
    )
    val offersCount: Int = 0,

    // Backend से फोन नंबर न मिलने पर खाली रहेगा।
    // किसी दूसरे उपयोगकर्ता का नंबर अनुमान से नहीं भरा जाएगा।
    @SerializedName(
        value = "buyer_phone",
        alternate = ["buyerPhone"]
    )
    val buyerPhone: String = ""
) {

    // पुराने UI कोड के साथ compatibility properties

    val requestId: String
        get() = id

    val requiredDate: String?
        get() = requiredByDate

    val notes: String
        get() = description
}
