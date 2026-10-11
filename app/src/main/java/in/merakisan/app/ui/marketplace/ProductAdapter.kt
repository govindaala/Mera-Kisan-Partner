// app/src/main/java/in/merakisan/app/ui/marketplace/ProductAdapter.kt
package `in`.merakisan.app.ui.marketplace

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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
 * पूर्ण फ़ीचर्स: MarketplaceFragment, HomeFragment दोनों के साथ 100% संगत
 */
class ProductAdapter(
    private val onProductClick: (ProductDto) -> Unit = {},
    private val onCallClick: (ProductDto) -> Unit = {},
    private val onWhatsAppClick: (ProductDto) -> Unit = {},
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
            val root = binding.root
            val ctx = root.context

            // 1. नाम व किस्म
            val title = if (!item.variety.isNullOrBlank()) "${item.name} (${item.variety})" else item.name
            findTextView(root, "tvProductName", "tvCropName", "tvTitle")?.text = title

            // 2. मूल्य
            val priceRupees = item.pricePaise / 100.0
            findTextView(root, "tvProductPrice", "tvPrice", "tvCropPrice")?.text =
                String.format(Locale.getDefault(), "₹%.0f / %s", priceRupees, item.unit)

            // 3. विक्रेता व स्थान
            val loc = if (!item.village.isNullOrBlank()) {
                "किसान: ${item.sellerName} • ${item.village},${item.district}"
            } else {
                "किसान: ${item.sellerName} •${item.district ?: ""}"
            }
            findTextView(root, "tvProductSeller", "tvSellerName", "tvFarmerName", "tvLocation")?.text = loc
            findTextView(root, "tvProductStock", "tvStock", "tvQuantity")?.text = "उपलब्ध: ${item.stockQuantity}${item.unit}"

            // 4. Coil इमेज लोडिंग
            val photoUrl = item.photos.firstOrNull() ?: item.imageUrls.firstOrNull()
            findImageView(root, "ivProductImage", "ivCropImage", "ivImage")?.let { iv ->
                if (!photoUrl.isNullOrBlank()) {
                    iv.load(photoUrl) {
                        crossfade(true)
                        placeholder(android.R.drawable.ic_menu_gallery)
                        error(android.R.drawable.ic_menu_gallery)
                        transformations(RoundedCornersTransformation(12f))
                    }
                } else {
                    iv.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            }

            // 5. सत्यापन बैज
            findView(root, "tvVerifiedBadge", "badgeVerified", "tvVerified")?.let { badge ->
                if (item.verificationStatus == "verified_farmer" || item.verificationStatus == "organic_certified") {
                    badge.visibility = View.VISIBLE
                    if (badge is TextView) {
                        badge.text = if (item.verificationStatus == "organic_certified") "जैविक प्रमाणित" else "सत्यापित किसान"
                    }
                } else {
                    badge.visibility = View.GONE
                }
            }

            // 6. क्लिक हैंडलर्स
            root.setOnClickListener { onProductClick(item) }
            findView(root, "btnCall", "ivCall")?.setOnClickListener { onCallClick(item) }
            findView(root, "btnWhatsApp", "btnWhatsapp", "ivWhatsApp")?.setOnClickListener { onWhatsAppClick(item) }
            findView(root, "btnFavorite", "ivFavorite", "btnFav")?.setOnClickListener { onFavoriteClick(item) }
        }

        private fun findTextView(root: View, vararg names: String): TextView? {
            for (name in names) {
                val id = root.resources.getIdentifier(name, "id", root.context.packageName)
                if (id != 0) {
                    val v = root.findViewById<View>(id)
                    if (v is TextView) return v
                }
            }
            return null
        }

        private fun findImageView(root: View, vararg names: String): ImageView? {
            for (name in names) {
                val id = root.resources.getIdentifier(name, "id", root.context.packageName)
                if (id != 0) {
                    val v = root.findViewById<View>(id)
                    if (v is ImageView) return v
                }
            }
            return null
        }

        private fun findView(root: View, vararg names: String): View? {
            for (name in names) {
                val id = root.resources.getIdentifier(name, "id", root.context.packageName)
                if (id != 0) {
                    val v = root.findViewById<View>(id)
                    if (v != null) return v
                }
            }
            return null
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
