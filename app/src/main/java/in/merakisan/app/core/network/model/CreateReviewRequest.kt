// app/src/main/java/in/merakisan/app/core/network/model/CreateReviewRequest.kt
package `in`.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

data class CreateReviewRequest(

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
        value = "target_uid",
        alternate = [
            "targetUid",
            "seller_uid"
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
    val comment: String = ""
)
