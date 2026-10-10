// app/src/main/java/in/merakisan/app/core/network/model/ReviewDto.kt
package in.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

/**
 * MERA KISAN Evidence-Based Rating & Review Models
 * केवल पूर्ण हुए (Completed) ऑर्डर्स पर वास्तविक रेटिंग हेतु डेटा अनुबंध
 */
data class ReviewDto(
    @SerializedName("review_id")
    val reviewId: String,

    @SerializedName("order_id")
    val orderId: String,

    @SerializedName("product_id")
    val productId: String,

    @SerializedName("reviewer_uid")
    val reviewerUid: String,

    @SerializedName("reviewer_name")
    val reviewerName: String,

    @SerializedName("target_uid")
    val targetUid: String, // किसान या खरीदार का UID

    @SerializedName("rating")
    val rating: Float, // 1.0 to 5.0 Stars

    @SerializedName("quality_rating")
    val qualityRating: Float = 5.0f,

    @SerializedName("delivery_rating")
    val deliveryRating: Float = 5.0f,

    @SerializedName("comment")
    val comment: String?,

    @SerializedName("created_at")
    val createdAt: String
)

data class CreateReviewRequest(
    @SerializedName("order_id")
    val orderId: String,

    @SerializedName("product_id")
    val productId: String,

    @SerializedName("target_uid")
    val targetUid: String,

    @SerializedName("rating")
    val rating: Float,

    @SerializedName("quality_rating")
    val qualityRating: Float,

    @SerializedName("delivery_rating")
    val deliveryRating: Float,

    @SerializedName("comment")
    val comment: String?
)
