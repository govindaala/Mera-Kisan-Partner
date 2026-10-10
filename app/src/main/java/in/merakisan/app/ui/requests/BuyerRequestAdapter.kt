// app/src/main/java/in/merakisan/app/ui/requests/BuyerRequestAdapter.kt
package in.merakisan.app.ui.requests

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import in.merakisan.app.core.config.FeatureManager
import in.merakisan.app.core.network.model.BuyerRequestDto
import in.merakisan.app.databinding.ItemBuyerRequestCardBinding
import java.util.Locale

class BuyerRequestAdapter(
    private val onSendOfferClick: (BuyerRequestDto) -> Unit,
    private val onCallClick: (BuyerRequestDto) -> Unit,
    private val onWhatsAppClick: (BuyerRequestDto) -> Unit
) : ListAdapter<BuyerRequestDto, BuyerRequestAdapter.BuyerRequestViewHolder>(BuyerRequestDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuyerRequestViewHolder {
        val binding = ItemBuyerRequestCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BuyerRequestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BuyerRequestViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BuyerRequestViewHolder(
        private val binding: ItemBuyerRequestCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(request: BuyerRequestDto) {
            val context = binding.root.context

            // 1. नाम व किस्म
            val title = if (!request.variety.isNullOrBlank()) {
                "${request.productName} (${request.variety})"
            } else {
                request.productName
            }
            binding.tvProductName.text = title

            // 2. मात्रा
            binding.tvQuantity.text = "मांग: ${request.quantity} ${request.unit}"

            // 3. लक्षित बजट (price_paise से सुरक्षित रूपांतरण)
            if (request.targetPricePaise != null && request.targetPricePaise > 0) {
                val targetRupees = request.targetPricePaise / 100.0
                binding.tvTargetPrice.visibility = View.VISIBLE
                binding.tvTargetPrice.text = String.format(Locale.getDefault(), "लक्षित दर: ₹%.0f / %s तक", targetRupees, request.unit)
            } else {
                binding.tvTargetPrice.visibility = View.GONE
            }

            // 4. आवश्यकता तिथि
            if (!request.requiredDate.isNullOrBlank()) {
                binding.tvRequiredDate.visibility = View.VISIBLE
                binding.tvRequiredDate.text = "आवश्यकता: ${request.requiredDate}"
            } else {
                binding.tvRequiredDate.visibility = View.GONE
            }

            // 5. खरीदार का नाम व लोकेशन प्राइवेसी
            val loc = if (!request.village.isNullOrBlank()) {
                "खरीदार: ${request.buyerName} • ${request.village}, ${request.district}"
            } else {
                "खरीदार: ${request.buyerName} • ${request.district}"
            }
            binding.tvBuyerLocation.text = loc

            // 6. टिप्पणी / विशेष विवरण
            if (!request.notes.isNullOrBlank()) {
                binding.tvNotes.visibility = View.VISIBLE
                binding.tvNotes.text = "टिप्पणी: ${request.notes}"
            } else {
                binding.tvNotes.visibility = View.GONE
            }

            // 7. ऑफ़र काउंटर
            binding.tvOffersCount.text = "${request.offersCount} किसानों ने संपर्क किया"

            // 8. रिमोट फ़ीचर गार्ड्स
            val isCallEnabled = FeatureManager.isEnabled(context, "call")
            val isWhatsAppEnabled = FeatureManager.isEnabled(context, "whatsapp")
            val isMakeOfferEnabled = FeatureManager.isEnabled(context, "make_offer")

            binding.btnCallBuyer.visibility = if (isCallEnabled && !request.buyerPhone.isNullOrBlank()) View.VISIBLE else View.GONE
            binding.btnWhatsAppBuyer.visibility = if (isWhatsAppEnabled && !request.buyerPhone.isNullOrBlank()) View.VISIBLE else View.GONE
            binding.btnSendOffer.visibility = if (isMakeOfferEnabled) View.VISIBLE else View.GONE

            // 9. क्लिक लिसनर्स
            binding.btnSendOffer.setOnClickListener { onSendOfferClick(request) }
            binding.btnCallBuyer.setOnClickListener { onCallClick(request) }
            binding.btnWhatsAppBuyer.setOnClickListener { onWhatsAppClick(request) }
        }
    }

    class BuyerRequestDiffCallback : DiffUtil.ItemCallback<BuyerRequestDto>() {
        override fun areItemsTheSame(oldItem: BuyerRequestDto, newItem: BuyerRequestDto): Boolean {
            return oldItem.requestId == newItem.requestId
        }

        override fun areContentsTheSame(oldItem: BuyerRequestDto, newItem: BuyerRequestDto): Boolean {
            return oldItem == newItem
        }
    }
}
