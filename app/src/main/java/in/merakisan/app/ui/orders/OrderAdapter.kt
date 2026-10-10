// app/src/main/java/in/merakisan/app/ui/orders/OrderAdapter.kt
package in.merakisan.app.ui.orders

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import in.merakisan.app.core.network.model.OrderDto
import in.merakisan.app.databinding.ItemOrderCardBinding
import java.util.Locale

class OrderAdapter(
    private val onOrderClick: (OrderDto) -> Unit
) : ListAdapter<OrderDto, OrderAdapter.OrderViewHolder>(OrderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class OrderViewHolder(
        private val binding: ItemOrderCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(order: OrderDto) {
            binding.tvOrderNumber.text = "ऑर्डर #${order.orderId.takeLast(8)}"
            binding.tvOrderProductName.text = order.productName
            binding.tvOrderQuantity.text = "मात्रा: ${order.quantity} ${order.unit}"

            // Integer Paise से सुरक्षित रुपये में गणना
            val totalRupees = order.totalAmountPaise / 100.0
            binding.tvOrderTotalAmount.text = String.format(Locale.getDefault(), "कुल: ₹%.2f", totalRupees)

            // ऑर्डर स्थिति व रंग बैज
            binding.tvOrderStatusBadge.text = order.orderStatus
            when (order.orderStatus) {
                "COMPLETED", "DELIVERED" -> {
                    binding.tvOrderStatusBadge.setTextColor(Color.parseColor("#1B5E20"))
                    binding.tvOrderStatusBadge.setBackgroundColor(Color.parseColor("#E8F5E9"))
                }
                "ACCEPTED", "PROCESSING", "IN_TRANSIT" -> {
                    binding.tvOrderStatusBadge.setTextColor(Color.parseColor("#0D47A1"))
                    binding.tvOrderStatusBadge.setBackgroundColor(Color.parseColor("#E3F2FD"))
                }
                "CANCELLED" -> {
                    binding.tvOrderStatusBadge.setTextColor(Color.parseColor("#B71C1C"))
                    binding.tvOrderStatusBadge.setBackgroundColor(Color.parseColor("#FFEBEE"))
                }
                else -> { // PLACED
                    binding.tvOrderStatusBadge.setTextColor(Color.parseColor("#E65100"))
                    binding.tvOrderStatusBadge.setBackgroundColor(Color.parseColor("#FFF3E0"))
                }
            }

            binding.tvOrderPaymentStatus.text = "भुगतान: ${order.paymentStatus}"
            val formattedDate = if (order.createdAt.length >= 10) order.createdAt.substring(0, 10) else order.createdAt
            binding.tvOrderDate.text = "दिनांक: $formattedDate"

            binding.root.setOnClickListener { onOrderClick(order) }
            binding.btnOrderDetails.setOnClickListener { onOrderClick(order) }
        }
    }

    class OrderDiffCallback : DiffUtil.ItemCallback<OrderDto>() {
        override fun areItemsTheSame(oldItem: OrderDto, newItem: OrderDto): Boolean {
            return oldItem.orderId == newItem.orderId
        }

        override fun areContentsTheSame(oldItem: OrderDto, newItem: OrderDto): Boolean {
            return oldItem == newItem
        }
    }
}
