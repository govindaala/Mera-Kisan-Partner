// app/src/main/java/in/merakisan/app/ui/favorites/FavoriteProductAdapter.kt
package in.merakisan.app.ui.favorites

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import in.merakisan.app.core.network.model.FavoriteProductDto
import in.merakisan.app.databinding.ItemFavoriteProductCardBinding
import java.util.Locale

class FavoriteProductAdapter(
    private val onProductClick: (FavoriteProductDto) -> Unit,
    private val onRemoveClick: (FavoriteProductDto) -> Unit
) : ListAdapter<FavoriteProductDto, FavoriteProductAdapter.FavoriteProductViewHolder>(FavoriteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteProductViewHolder {
        val binding = ItemFavoriteProductCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FavoriteProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavoriteProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FavoriteProductViewHolder(
        private val binding: ItemFavoriteProductCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FavoriteProductDto) {
            val title = if (!item.variety.isNullOrBlank()) "${item.name} (${item.variety})" else item.name
            binding.tvFavProductName.text = title

            val priceRupees = item.pricePaise / 100.0
            binding.tvFavProductPrice.text = String.format(Locale.getDefault(), "₹%.0f / %s", priceRupees, item.unit)

            val loc = if (!item.village.isNullOrBlank()) {
                "किसान: ${item.sellerName} • ${item.village}, ${item.district}"
            } else {
                "किसान: ${item.sellerName} • ${item.district}"
            }
            binding.tvFavProductSeller.text = loc
            binding.tvFavProductStock.text = "उपलब्ध: ${item.stockQuantity} ${item.unit}"

            if (!item.photoUrl.isNullOrBlank()) {
                binding.ivFavProductThumb.load(item.photoUrl) {
                    crossfade(true)
                }
            }

            binding.root.setOnClickListener { onProductClick(item) }
            binding.btnRemoveFavorite.setOnClickListener { onRemoveClick(item) }
        }
    }

    class FavoriteDiffCallback : DiffUtil.ItemCallback<FavoriteProductDto>() {
        override fun areItemsTheSame(oldItem: FavoriteProductDto, newItem: FavoriteProductDto): Boolean {
            return oldItem.favoriteId == newItem.favoriteId
        }

        override fun areContentsTheSame(oldItem: FavoriteProductDto, newItem: FavoriteProductDto): Boolean {
            return oldItem == newItem
        }
    }
}
