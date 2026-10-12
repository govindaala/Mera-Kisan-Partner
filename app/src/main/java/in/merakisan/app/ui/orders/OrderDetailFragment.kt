// app/src/main/java/in/merakisan/app/ui/orders/OrderDetailFragment.kt
package `in`.merakisan.app.ui.orders

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.network.UpdateOrderStatusRequest
import `in`.merakisan.app.core.network.model.OrderDto
import `in`.merakisan.app.databinding.FragmentOrderDetailBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class OrderDetailFragment : Fragment() {

    private var _binding: FragmentOrderDetailBinding? = null
    private val binding get() = _binding!!
    private var currentOrderId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentOrderId = arguments?.getString("orderId", "").orEmpty()
        loadOrderDetail()

        findView("btnBack")?.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun loadOrderDetail() {
        if (currentOrderId.isBlank()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val api = ApiClient.getApiService(requireContext())
                val resp = api.getOrderDetail(currentOrderId)
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful && resp.body()?.success == true) {
                        resp.body()?.data?.let { bindOrder(it) }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun bindOrder(order: OrderDto) {
        findTextView("tvOrderId")?.text = "ऑर्डर ID: #${order.orderId.takeLast(8)}"
        findTextView("tvProductName")?.text = order.productName
        findTextView("tvOrderQuantity")?.text = "मात्रा: ${order.quantity}${order.unit}"
        val totalRupees = order.totalAmountPaise / 100.0
        findTextView("tvTotalAmount")?.text = String.format(Locale.getDefault(), "कुल राशि: ₹%.0f", totalRupees)
        findTextView("tvOrderStatus")?.text = "स्थिति: ${order.orderStatus}"

        findView("btnPrimaryAction")?.setOnClickListener {
            updateStatus("ACCEPTED", "स्वीकृत")
        }
    }

    private fun updateStatus(status: String, notes: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val api = ApiClient.getApiService(requireContext())
                val req = UpdateOrderStatusRequest(nextStatus = status, notes = notes)
                val resp = api.updateOrderStatus(currentOrderId, req)
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful) {
                        Toast.makeText(context, "स्थिति अद्यतित हुई", Toast.LENGTH_SHORT).show()
                        loadOrderDetail()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun findTextView(name: String): android.widget.TextView? {
        val id = resources.getIdentifier(name, "id", requireContext().packageName)
        return if (id != 0) binding.root.findViewById(id) else null
    }

    private fun findView(name: String): View? {
        val id = resources.getIdentifier(name, "id", requireContext().packageName)
        return if (id != 0) binding.root.findViewById(id) else null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
