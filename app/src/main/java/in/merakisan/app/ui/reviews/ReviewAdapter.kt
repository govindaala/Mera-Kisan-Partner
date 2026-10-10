// app/src/main/java/in/merakisan/app/ui/reviews/ReviewAdapter.kt
package in.merakisan.app.ui.reviews

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import in.merakisan.app.core.network.model.ReviewDto
import in.merakisan.app.databinding.ItemReviewCardBinding
import java.util.Locale

class ReviewAdapter : ListAdapter<ReviewDto, ReviewAdapter.ReviewViewHolder>(ReviewDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ReviewViewHolder(
        private val binding: ItemReviewCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(review: ReviewDto) {
            binding.tvReviewerName.text = review.reviewerName
            binding.tvReviewerInitial.text = review.reviewerName.trim().take(1)
            binding.tvReviewDate.text = review.createdAt
            binding.tvReviewStarBadge.text = String.format(Locale.getDefault(), "★ %.1f", review.rating)

            binding.tvQualityScore.text = String.format(Locale.getDefault(), "फसल गुणवत्ता: ★ %.1f", review.qualityRating)
            binding.tvDeliveryScore.text = String.format(Locale.getDefault(), "डिलीवरी समय: ★ %.1f", review.deliveryRating)

            binding.tvReviewComment.text = review.comment ?: "कोई लिखित टिप्पणी नहीं दी गई।"
        }
    }

    class ReviewDiffCallback : DiffUtil.ItemCallback<ReviewDto>() {
        override fun areItemsTheSame(oldItem: ReviewDto, newItem: ReviewDto): Boolean {
            return oldItem.reviewId == newItem.reviewId
        }

        override fun areContentsTheSame(oldItem: ReviewDto, newItem: ReviewDto): Boolean {
            return oldItem == newItem
        }
    }
}
