// app/src/main/java/in/merakisan/app/ui/marketplace/ProductAdapter.kt
package `in`.merakisan.app.ui.marketplace

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import `in`.merakisan.app.core.network.model.ProductDto
import `in`.merakisan.app.databinding.ItemProductCardBinding
import java.util.Locale

/**
 * MERA KISAN Marketplace Produce Adapter
 * पूर्ण फ़ीचर्स: Coil इमेज लोडिंग, कॉलबैक सपोर्ट, स्टॉक व मूल्य प्रदर्शन
 */
class ProductAdapter(
    private val onProductClick: (ProductDto) -> Unit = {},
    private val onCallClick: (ProductDto) -> Unit = {},
    private val onFavoriteClick: (ProductDto) -> Unit = {}
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

        fun bind(item: ProductDto) {
            // 1. नाम व किस्म
            val title = if (item.variety.isNotBlank()) "${item.name} (${item.variety})" else item.name
            binding.tvProductName.text = title

            // 2. मूल्य (Integer Paise से सुरक्षित प्रदर्शन)
            val priceRupees = item.pricePaise / 100.0
            binding.tvProductPrice.text = String.format(Locale.getDefault(), "₹%.0f / %s", priceRupees, item.unit)

            // 3. विक्रेता व स्थान (गोपनीयता सुरक्षित)
            val loc = if (item.village.isNotBlank()) {
                "किसान: ${item.sellerName} • ${item.village},${item.district}"
            } else {
                "किसान: ${item.sellerName} •${item.district}"
            }
            binding.tvProductSeller.text = loc
            binding.tvProductStock.text = "उपलब्ध: ${item.stockQuantity}${item.unit}"

            // 4. Coil इमेज लोडिंग (Free-First Baseline)
            val photoUrl = item.photos.firstOrNull() ?: item.imageUrls.firstOrNull()
            if (!photoUrl.isNullOrBlank()) {
                binding.ivProductImage.load(photoUrl) {
                    crossfade(true)
                    placeholder(android.R.drawable.ic_menu_gallery)
                    error(android.R.drawable.ic_menu_gallery)
                    transformations(RoundedCornersTransformation(12f))
                }
            } else {
                binding.ivProductImage.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            // 5. सत्यापन बैज (प्रमाणित होने पर ही प्रदर्शित)
            if (item.verificationStatus == "verified_farmer" || item.verificationStatus == "organic_certified") {
                binding.tvVerifiedBadge?.visibility = View.VISIBLE
                binding.tvVerifiedBadge?.text = if (item.verificationStatus == "organic_certified") "जैविक प्रमाणित" else "सत्यापित किसान"
            } else {
                binding.tvVerifiedBadge?.visibility = View.GONE
            }

            // 6. क्लिक हैंडलर्स
            binding.root.setOnClickListener { onProductClick(item) }
            binding.btnCall?.setOnClickListener { onCallClick(item) }
            binding.btnFavorite?.setOnClickListener { onFavoriteClick(item) }
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
