// app/src/main/java/in/merakisan/app/core/network/model/CreateReviewRequest.kt
package `in`.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

/**
 * MERA KISAN — समीक्षा सबमिशन नेटवर्क अनुरोध पेलोड
 * ReviewsViewModel और AddReviewDialogFragment दोनों के साथ 100% संरेखित
 */
data class CreateReviewRequest(
    @SerializedName("order_id")
    val orderId: String = "",
    @SerializedName("product_id")
    val productId: String = "",
    @SerializedName("target_uid")
    val targetUid: String = "",
    @SerializedName("rating")
    val rating: Float = 5.0f,
    @SerializedName("quality_rating")
    val qualityRating: Float = 5.0f,
    @SerializedName("delivery_rating")
    val deliveryRating: Float = 5.0f,
    @SerializedName("comment")
    val comment: String = ""
)
