// app/src/main/java/in/merakisan/app/ui/marketplace/ProductAdapter.kt
package `in`.merakisan.app.ui.marketplace

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import `in`.merakisan.app.core.network.model.ProductDto
import `in`.merakisan.app.databinding.ItemProductCardBinding
import java.util.Locale

class ProductAdapter(
    private val onProductClick: (ProductDto) -> Unit = {},
    private val onCallClick: (ProductDto) -> Unit = {}
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
            val title = if (item.variety.isNotBlank()) "${item.name} (${item.variety})" else item.name
            binding.tvProductName.text = title

            val priceRupees = item.pricePaise / 100.0
            binding.tvProductPrice.text = String.format(Locale.getDefault(), "₹%.0f / %s", priceRupees, item.unit)

            val loc = if (item.village.isNotBlank()) {
                "किसान: ${item.sellerName} • ${item.village},${item.district}"
            } else {
                "किसान: ${item.sellerName} •${item.district}"
            }
            binding.tvProductSeller.text = loc
            binding.tvProductStock.text = "उपलब्ध: ${item.stockQuantity}${item.unit}"

            binding.root.setOnClickListener { onProductClick(item) }
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
