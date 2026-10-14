// app/src/main/java/in/merakisan/app/core/network/model/BuyerRequestDto.kt
package `in`.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

data class BuyerRequestDto(

    @SerializedName(
        value = "request_id",
        alternate = ["requestId", "id", "_id"]
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

    @SerializedName("notes", alternate = ["description"])
    val notes: String = "",

    @SerializedName("status")
    val status: String = "ACTIVE",

    @SerializedName(
        value = "created_at",
        alternate = ["createdAt"]
    )
    val createdAt: Long? = null,

    @SerializedName(
        value = "offers_count",
        alternate = ["offersCount"]
    )
    val offersCount: Int = 0

) {
    /**
     * Adapter compatibility property.
     * requiredDate और requiredByDate दोनों एक ही तारीख दर्शाते हैं।
     */
    val requiredDate: String?
        get() = requiredByDate

    /**
     * पुराने UI को description के नाम से टेक्स्ट चाहिए
     * तो यह property notes से वही टेक्स्ट उपलब्ध कराती है।
     */
    val description: String
        get() = notes

    /**
     * पुराने UI में id का उपयोग होने पर compatibility के लिए।
     */
    val id: String
        get() = requestId
}
