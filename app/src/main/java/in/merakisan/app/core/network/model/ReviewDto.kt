// app/src/main/java/in/merakisan/app/core/network/model/ReviewDto.kt
package `in`.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

data class ReviewDto(

    @SerializedName(
        value = "review_id",
        alternate = [
            "id",
            "_id",
            "reviewId"
        ]
    )
    val reviewId: String = "",

    @SerializedName(
        value = "order_id",
        alternate = ["orderId"]
    )
    val orderId: String = "",

    @SerializedName(
        value = "product_id",
        alternate = ["productId"]
    )
    val productId: String = "",

    @SerializedName(
        value = "buyer_uid",
        alternate = [
            "buyerUid",
            "reviewerUid"
        ]
    )
    val reviewerUid: String = "",

    @SerializedName(
        value = "buyer_name",
        alternate = [
            "buyerName",
            "reviewerName"
        ]
    )
    val reviewerName: String = "सत्यापित खरीदार",

    @SerializedName(
        value = "seller_uid",
        alternate = [
            "targetUid",
            "target_uid"
        ]
    )
    val targetUid: String = "",

    @SerializedName("rating")
    val rating: Float = 5.0f,

    @SerializedName(
        value = "quality_rating",
        alternate = ["qualityRating"]
    )
    val qualityRating: Float = 5.0f,

    @SerializedName(
        value = "delivery_rating",
        alternate = ["deliveryRating"]
    )
    val deliveryRating: Float = 5.0f,

    @SerializedName("comment")
    val comment: String = "",

    @SerializedName(
        value = "created_at",
        alternate = ["createdAt"]
    )
    val createdAt: String? = null
)
