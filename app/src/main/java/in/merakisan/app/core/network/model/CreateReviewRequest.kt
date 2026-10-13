// app/src/main/java/in/merakisan/app/core/network/model/CreateReviewRequest.kt
package `in`.merakisan.app.core.network.model

/**
 * MERA KISAN — समीक्षा निर्माण नेटवर्क अनुरोध पेलोड
 * Master Spec v1.1 Section 9: Verified Buyer Review Request
 */
data class CreateReviewRequest(
    val orderId: String = "",
    val productId: String = "",
    val targetUid: String = "",
    val rating: Float = 5.0f,
    val comment: String = ""
)
