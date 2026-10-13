package `in`.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

data class CreateReviewRequest(
    @SerializedName("product_id", alternate = ["productId"])
    val productId: String = "",

    @SerializedName("order_id", alternate = ["orderId"])
    val orderId: String = "",

    @SerializedName("rating")
    val rating: Float = 5.0f,

    @SerializedName("quality_rating", alternate = ["qualityRating"])
    val qualityRating: Float = 5.0f,

    @SerializedName("delivery_rating", alternate = ["deliveryRating"])
    val deliveryRating: Float = 5.0f,

    @SerializedName("comment")
    val comment: String = ""
)

data class ReviewDto(
    @SerializedName("id", alternate = ["_id", "reviewId"])
    val id: String = "",

    @SerializedName("orderId", alternate = ["order_id"])
    val orderId: String = "",

    @SerializedName("productId", alternate = ["product_id"])
    val productId: String = "",

    @SerializedName("buyerUid", alternate = ["buyer_uid"])
    val buyerUid: String = "",

    @SerializedName("buyerName", alternate = ["buyer_name"])
    val buyerName: String = "सत्यापित खरीदार",

    @SerializedName("rating")
    val rating: Float = 5.0f,

    @SerializedName("qualityRating", alternate = ["quality_rating"])
    val qualityRating: Float = 5.0f,

    @SerializedName("deliveryRating", alternate = ["delivery_rating"])
    val deliveryRating: Float = 5.0f,

    @SerializedName("comment")
    val comment: String = "",

    @SerializedName("createdAt", alternate = ["created_at"])
    val createdAt: Long? = null
)
