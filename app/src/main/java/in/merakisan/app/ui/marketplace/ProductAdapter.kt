// app/src/main/java/in/merakisan/app/ui/marketplace/ProductAdapter.kt
package in.merakisan.app.ui.marketplace

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import in.merakisan.app.core.config.FeatureManager
import in.merakisan.app.core.network.model.ProductDto
import in.merakisan.app.databinding.ItemProductCardBinding
import java.util.Locale

class ProductAdapter(
    private val onProductClick: (ProductDto) -> Unit,
    private val onCallClick: (ProductDto) -> Unit,
    private val onWhatsAppClick: (ProductDto) -> Unit
) : ListAdapter<ProductDto, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProductViewHolder(
        private val binding: ItemProductCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: ProductDto) {
            val context = binding.root.context

            // 1. नाम व किस्म
            val displayName = if (!product.variety.isNullOrBlank()) {
                "${product.name} (${product.variety})"
            } else {
                product.name
            }
            binding.tvProductName.text = displayName

            // 2. कीमत व उपलब्ध मात्रा (price_paise से रुपये में सुरक्षित गणना)
            val priceRupees = product.pricePaise / 100.0
            val formattedPrice = String.format(Locale.getDefault(), "₹%.0f / %s", priceRupees, product.unit)
            binding.tvProductPrice.text = "$formattedPrice (उपलब्ध: ${product.stockQuantity} ${product.unit})"

            // 3. छोटी मात्रा बैज (1–25 kg USP)
            val isSmallQty = product.stockQuantity <= 25.0 || (product.minOrderQuantity ?: 1.0) <= 25.0
            binding.tvSmallQuantityBadge.visibility = if (isSmallQty) View.VISIBLE else View.GONE

            // 4. किसान नाम व लोकेशन प्राइवेसी (सटीक GPS कभी नहीं, केवल गांव व ज़िला)
            val loc = if (!product.village.isNullOrBlank()) {
                "किसान: ${product.sellerName} • ${product.village}, ${product.district}"
            } else {
                "किसान: ${product.sellerName} • ${product.district}"
            }
            binding.tvFarmerLocation.text = loc

            // 5. सत्यापन बैज
            when (product.verificationStatus) {
                "organic_certified" -> {
                    binding.tvVerificationBadge.visibility = View.VISIBLE
                    binding.tvVerificationBadge.text = "🌿 Organic Certified"
                }
                "verified_farmer" -> {
                    binding.tvVerificationBadge.visibility = View.VISIBLE
                    binding.tvVerificationBadge.text = "✔ Mera Kisan Verified"
                }
                else -> {
                    binding.tvVerificationBadge.visibility = View.GONE
                }
            }

            // 6. ताज़ा फसल / कटाई तिथि
            if (!product.harvestDate.isNullOrBlank()) {
                binding.tvHarvestInfo.visibility = View.VISIBLE
                binding.tvHarvestInfo.text = "कटाई तिथि: ${product.harvestDate}"
            } else {
                binding.tvHarvestInfo.visibility = View.GONE
            }

            // 7. तस्वीर लोडिंग (Coil इंजन)
            if (product.photos.isNotEmpty()) {
                binding.ivProductImage.load(product.photos[0]) {
                    crossfade(true)
                }
            }

            // 8. रिमोट फ़ीचर गार्ड (WhatsApp व Call बटन नियंत्रण)
            val isCallEnabled = FeatureManager.isEnabled(context, "call")
            val isWhatsAppEnabled = FeatureManager.isEnabled(context, "whatsapp")

            binding.btnCall.visibility = if (isCallEnabled && !product.sellerPhone.isNullOrBlank()) View.VISIBLE else View.GONE
            binding.btnWhatsApp.visibility = if (isWhatsAppEnabled && !product.sellerPhone.isNullOrBlank()) View.VISIBLE else View.GONE

            // 9. क्लिक लिसनर्स
            binding.root.setOnClickListener { onProductClick(product) }
            binding.btnViewDetails.setOnClickListener { onProductClick(product) }
            binding.btnCall.setOnClickListener { onCallClick(product) }
            binding.btnWhatsApp.setOnClickListener { onWhatsAppClick(product) }
        }
    }

    class ProductDiffCallback : DiffUtil.ItemCallback<ProductDto>() {
        override fun areItemsTheSame(oldItem: ProductDto, newItem: ProductDto): Boolean {
            return oldItem.productId == newItem.productId
        }

        override fun areContentsTheSame(oldItem: ProductDto, newItem: ProductDto): Boolean {
            return oldItem == newItem
        }
    }
}
