// app/src/main/java/in/merakisan/app/core/network/model/ReviewDto.kt
package `in`.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

/**
 * MERA KISAN — समीक्षा एवं 5-स्टार रेटिंग डेटा मॉडल
 * Master Spec v1.1 Section 9 & 17: Verified Delivery Reviews
 */
data class ReviewDto(
    @SerializedName("review_id")
    val reviewId: String = "",
    @SerializedName("order_id")
    val orderId: String = "",
    @SerializedName("product_id")
    val productId: String = "",
    @SerializedName("reviewer_uid")
    val reviewerUid: String = "",
    @SerializedName("reviewer_name")
    val reviewerName: String = "",
    @SerializedName("target_uid")
    val targetUid: String = "",
    @SerializedName("rating")
    val rating: Float = 5.0f,
    @SerializedName("quality_rating")
    val qualityRating: Float = 5.0f,
    @SerializedName("delivery_rating")
    val deliveryRating: Float = 5.0f,
    @SerializedName("comment")
    val comment: String = "",
    @SerializedName("created_at")
    val createdAt: String = ""
)
