// app/src/main/java/in/merakisan/app/ui/mandi/MandiPriceAdapter.kt
package in.merakisan.app.ui.mandi

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import in.merakisan.app.core.network.model.MandiPriceDto
import in.merakisan.app.databinding.ItemMandiPriceBinding
import java.util.Locale

class MandiPriceAdapter(
    private val onItemClick: (MandiPriceDto) -> Unit
) : ListAdapter<MandiPriceDto, MandiPriceAdapter.MandiViewHolder>(MandiDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MandiViewHolder {
        val binding = ItemMandiPriceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MandiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MandiViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MandiViewHolder(
        private val binding: ItemMandiPriceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MandiPriceDto) {
            binding.tvMandiCommodity.text = item.commodity
            binding.tvMandiVariety.text = "किस्म: ${item.variety?.ifBlank { "सामान्य" } ?: "सामान्य"}"
            binding.tvMandiMarketLocation.text = "🏛️ मंडी: ${item.market} • ज़िला: ${item.district} (${item.state})"
            binding.tvMandiArrivalDate.text = item.arrivalDate

            // पैसे से रुपये प्रति क्विंटल रूपांतरण (वित्तीय सुरक्षा: integer paise to ₹)
            val minRs = item.minPricePaise / 100.0
            val modalRs = item.modalPricePaise / 100.0
            val maxRs = item.maxPricePaise / 100.0

            binding.tvMandiMinPrice.text = String.format(Locale.getDefault(), "₹%.0f/q", minRs)
            binding.tvMandiModalPrice.text = String.format(Locale.getDefault(), "₹%.0f/q", modalRs)
            binding.tvMandiMaxPrice.text = String.format(Locale.getDefault(), "₹%.0f/q", maxRs)

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    class MandiDiffCallback : DiffUtil.ItemCallback<MandiPriceDto>() {
        override fun areItemsTheSame(oldItem: MandiPriceDto, newItem: MandiPriceDto): Boolean {
            return oldItem.market == newItem.market && oldItem.commodity == newItem.commodity
        }

        override fun areContentsTheSame(oldItem: MandiPriceDto, newItem: MandiPriceDto): Boolean {
            return oldItem == newItem
        }
    }
}
