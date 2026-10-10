// app/src/main/java/in/merakisan/app/ui/favorites/FollowedFarmerAdapter.kt
package in.merakisan.app.ui.favorites

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import in.merakisan.app.core.config.FeatureManager
import in.merakisan.app.core.network.model.FollowedFarmerDto
import in.merakisan.app.databinding.ItemFollowedFarmerCardBinding
import java.util.Locale

class FollowedFarmerAdapter(
    private val onFarmerClick: (FollowedFarmerDto) -> Unit,
    private val onCallClick: (FollowedFarmerDto) -> Unit,
    private val onWhatsAppClick: (FollowedFarmerDto) -> Unit,
    private val onUnfollowClick: (FollowedFarmerDto) -> Unit
) : ListAdapter<FollowedFarmerDto, FollowedFarmerAdapter.FarmerViewHolder>(FarmerDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FarmerViewHolder {
        val binding = ItemFollowedFarmerCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FarmerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FarmerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FarmerViewHolder(
        private val binding: ItemFollowedFarmerCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(farmer: FollowedFarmerDto) {
            val context = binding.root.context
            binding.tvFollowedFarmerName.text = farmer.name
            binding.tvFarmerAvatarInitial.text = farmer.name.take(1)

            val loc = if (!farmer.village.isNullOrBlank()) {
                "📍 ग्राम: ${farmer.village} • ज़िला: ${farmer.district}"
            } else {
                "📍 ज़िला: ${farmer.district}"
            }
            binding.tvFollowedFarmerLocation.text = loc
            binding.tvFarmerActiveCropsCount.text = "🌾 ${farmer.activeCropsCount} फसलें उपलब्ध"
            binding.tvFarmerRatingDisplay.text = String.format(Locale.getDefault(), "⭐ %.1f", farmer.rating)

            when (farmer.verificationStatus) {
                "verified_farmer" -> {
                    binding.tvFarmerVerifiedTag.visibility = View.VISIBLE
                    binding.tvFarmerVerifiedTag.text = "✔ Verified"
                }
                "organic_certified" -> {
                    binding.tvFarmerVerifiedTag.visibility = View.VISIBLE
                    binding.tvFarmerVerifiedTag.text = "🌿 Organic"
                }
                else -> {
                    binding.tvFarmerVerifiedTag.visibility = View.GONE
                }
            }

            // रिमोट फ़ीचर गार्ड्स (FeatureManager)
            val isCallEnabled = FeatureManager.isEnabled(context, "call")
            val isWhatsAppEnabled = FeatureManager.isEnabled(context, "whatsapp")

            binding.btnFarmerCall.visibility = if (isCallEnabled && !farmer.phone.isNullOrBlank()) View.VISIBLE else View.GONE
            binding.btnFarmerWhatsApp.visibility = if (isWhatsAppEnabled && !farmer.phone.isNullOrBlank()) View.VISIBLE else View.GONE

            binding.root.setOnClickListener { onFarmerClick(farmer) }
            binding.btnFarmerCall.setOnClickListener { onCallClick(farmer) }
            binding.btnFarmerWhatsApp.setOnClickListener { onWhatsAppClick(farmer) }
            binding.btnUnfollowFarmer.setOnClickListener { onUnfollowClick(farmer) }
        }
    }

    class FarmerDiffCallback : DiffUtil.ItemCallback<FollowedFarmerDto>() {
        override fun areItemsTheSame(oldItem: FollowedFarmerDto, newItem: FollowedFarmerDto): Boolean {
            return oldItem.followId == newItem.followId
        }

        override fun areContentsTheSame(oldItem: FollowedFarmerDto, newItem: FollowedFarmerDto): Boolean {
            return oldItem == newItem
        }
    }
}
