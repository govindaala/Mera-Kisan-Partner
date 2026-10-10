// app/src/main/java/in/merakisan/app/ui/reviews/ReviewsViewModel.kt
package in.merakisan.app.ui.reviews

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import in.merakisan.app.core.network.model.CreateReviewRequest
import in.merakisan.app.core.network.model.ReviewDto
import in.merakisan.app.core.security.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ReviewsUiState {
    object Idle : ReviewsUiState()
    object Loading : ReviewsUiState()
    data class Loaded(val reviews: List<ReviewDto>) : ReviewsUiState()
    data class Submitted(val message: String) : ReviewsUiState()
    data class Error(val message: String) : ReviewsUiState()
}

class ReviewsViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ReviewsUiState>(ReviewsUiState.Idle)
    val uiState: StateFlow<ReviewsUiState> = _uiState.asStateFlow()

    private val localReviews = mutableListOf<ReviewDto>()

    fun loadReviewsForProduct(productId: String) {
        viewModelScope.launch {
            _uiState.value = ReviewsUiState.Loading
            if (localReviews.isEmpty()) {
                seedInitialReviews(productId)
            }
            _uiState.value = ReviewsUiState.Loaded(localReviews.filter { it.productId == productId })
        }
    }

    fun submitReview(request: CreateReviewRequest) {
        viewModelScope.launch {
            _uiState.value = ReviewsUiState.Loading

            val context = getApplication<Application>()
            val reviewerUid = SessionManager.getUserUid(context) ?: "buyer_guest"
            val newReview = ReviewDto(
                reviewId = "REV_${System.currentTimeMillis().toString().takeLast(6)}",
                orderId = request.orderId,
                productId = request.productId,
                reviewerUid = reviewerUid,
                reviewerName = "सत्यापित ग्राहक",
                targetUid = request.targetUid,
                rating = request.rating,
                qualityRating = request.qualityRating,
                deliveryRating = request.deliveryRating,
                comment = request.comment,
                createdAt = "आज"
            )

            localReviews.add(0, newReview)
            _uiState.value = ReviewsUiState.Submitted("आपकी समीक्षा सफलतापूर्वक दर्ज हो गई!")
        }
    }

    private fun seedInitialReviews(productId: String) {
        localReviews.add(
            ReviewDto(
                reviewId = "REV_101",
                orderId = "ORD_8901",
                productId = productId,
                reviewerUid = "B_201",
                reviewerName = "दिनेश पाटीदार",
                targetUid = "F_101",
                rating = 5.0f,
                qualityRating = 5.0f,
                deliveryRating = 5.0f,
                comment = "सोयाबीन की गुणवत्ता एकदम साफ थी, जैविक खाद का प्रयोग स्पष्ट दिखता है। धन्यवाद किसान भाई!",
                createdAt = "कल"
            )
        )
    }
}
