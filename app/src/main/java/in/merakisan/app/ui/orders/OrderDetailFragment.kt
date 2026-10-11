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
import `in`.merakisan.app.core.network.model.OrderDto
import `in`.merakisan.app.core.network.model.UpdateOrderStatusRequest
import `in`.merakisan.app.core.security.SessionManager
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
    private var currentOrder: OrderDto? = null

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
        if (currentOrderId.isNotBlank()) {
            loadOrderDetail(currentOrderId)
        } else {
            Toast.makeText(context, "ऑर्डर आईडी अमान्य है।", Toast.LENGTH_SHORT).show()
        }

        findView("btnBack")?.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun loadOrderDetail(orderId: String) {
        findView("progressBar")?.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiService = ApiClient.getApiService(requireContext())
                val response = apiService.getOrderDetail(orderId)

                withContext(Dispatchers.Main) {
                    findView("progressBar")?.visibility = View.GONE
                    if (response.isSuccessful && response.body()?.success == true) {
                        val order = response.body()?.data
                        if (order != null) {
                            bindOrderData(order)
                        }
                    } else {
                        Toast.makeText(context, "ऑर्डर विवरण प्राप्त नहीं हो सका।", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    findView("progressBar")?.visibility = View.GONE
                    Toast.makeText(context, "नेटवर्क त्रुटि: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun bindOrderData(order: OrderDto) {
        currentOrder = order
        val sessionManager = SessionManager(requireContext())
        val currentUser = sessionManager.getUserSession()
        val isSeller = currentUser?.uid == order.sellerUid

        // 1. टेक्स्ट व राशि प्रदर्शन (IntegerPaise से सुरक्षित)
        findTextView("tvOrderId")?.text = "ऑर्डर आईडी: #${order.orderId.takeLast(8)}"
        findTextView("tvProductName")?.text = order.productName
        findTextView("tvOrderQuantity")?.text = "मात्रा: ${order.quantity}${order.unit}"

        val totalRupees = order.totalAmountPaise / 100.0
        findTextView("tvTotalAmount")?.text = String.format(Locale.getDefault(), "कुल राशि: ₹%.0f", totalRupees)
        findTextView("tvPaymentMethod")?.text = "भुगतान प्रकार: ${order.paymentMethod.uppercase()}"
        findTextView("tvOrderStatus")?.text = "स्थिति: ${formatStatusName(order.orderStatus)}"
        findTextView("tvDeliveryAddress")?.text = "डिलीवरी पता: ${order.deliveryAddress}"

        // 2. संपर्क बटन (Call / WhatsApp)
        val counterpartPhone = if (isSeller) order.buyerPhone else order.sellerPhone
        findView("btnCall")?.setOnClickListener {
            if (!counterpartPhone.isNullOrBlank()) {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$counterpartPhone")))
            } else {
                Toast.makeText(context, "फ़ोन नंबर उपलब्ध नहीं है।", Toast.LENGTH_SHORT).show()
            }
        }

        findView("btnWhatsApp")?.setOnClickListener {
            if (!counterpartPhone.isNullOrBlank()) {
                val clean = counterpartPhone.replace("+", "").replace(" ", "").trim()
                val url = "https://api.whatsapp.com/send?phone=$clean&text=${Uri.encode("नमस्ते, Mera Kisan ऑर्डर #${order.orderId.takeLast(6)} के संबंध में संपर्क कर रहे हैं।")}"
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } else {
                Toast.makeText(context, "WhatsApp नंबर उपलब्ध नहीं है।", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. स्टेट मशीन ट्रांज़िशन्स (Zero-Trust State Machine)
        setupStateActions(order, isSeller)
    }

    private fun setupStateActions(order: OrderDto, isSeller: Boolean) {
        val btnPrimary = findView("btnPrimaryAction")
        val btnSecondary = findView("btnSecondaryAction")

        btnPrimary?.visibility = View.GONE
        btnSecondary?.visibility = View.GONE

        if (isSeller) {
            // किसान की स्वीकृत क्रियाएं
            when (order.orderStatus.uppercase()) {
                "PLACED" -> {
                    setActionButton(btnPrimary, "ऑर्डर स्वीकार करें (Accept)") {
                        executeStatusUpdate("ACCEPTED", "किसान द्वारा ऑर्डर स्वीकार किया गया")
                    }
                    setActionButton(btnSecondary, "रद्द करें (Reject)") {
                        executeStatusUpdate("CANCELLED", "किसान द्वारा ऑर्डर रद्द किया गया")
                    }
                }
                "ACCEPTED" -> {
                    setActionButton(btnPrimary, "डिस्पैच / रवाना करें (In Transit)") {
                        executeStatusUpdate("IN_TRANSIT", "उपज गंतव्य के लिए रवाना की गई")
                    }
                }
                "IN_TRANSIT" -> {
                    setActionButton(btnPrimary, "डिलीवरी पूर्ण दर्ज करें (Delivered)") {
                        executeStatusUpdate("DELIVERED", "उपज ग्राहक तक पहुँच चुकी है")
                    }
                }
            }
        } else {
            // खरीदार की स्वीकृत क्रियाएं
            when (order.orderStatus.uppercase()) {
                "PLACED" -> {
                    setActionButton(btnSecondary, "ऑर्डर रद्द करें") {
                        executeStatusUpdate("CANCELLED", "खरीदार द्वारा ऑर्डर रद्द किया गया")
                    }
                }
                "IN_TRANSIT" -> {
                    setActionButton(btnPrimary, "प्राप्ति की पुष्टि करें (Confirm Received)") {
                        executeStatusUpdate("DELIVERED", "खरीदार ने उपज प्राप्त कर ली")
                    }
                }
                "DELIVERED" -> {
                    setActionButton(btnSecondary, "विवाद दर्ज करें (Dispute)") {
                        openDisputeDialog(order.orderId)
                    }
                }
            }
        }
    }

    private fun setActionButton(view: View?, title: String, action: () -> Unit) {
        view?.let {
            it.visibility = View.VISIBLE
            if (it is android.widget.Button) it.text = title
            it.setOnClickListener { action() }
        }
    }

    private fun executeStatusUpdate(newStatus: String, notes: String) {
        findView("progressBar")?.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiService = ApiClient.getApiService(requireContext())
                val req = UpdateOrderStatusRequest(status = newStatus, notes = notes)
                val response = apiService.updateOrderStatus(currentOrderId, req)

                withContext(Dispatchers.Main) {
                    findView("progressBar")?.visibility = View.GONE
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(context, "ऑर्डर स्थिति अद्यतित की गई!", Toast.LENGTH_SHORT).show()
                        loadOrderDetail(currentOrderId)
                    } else {
                        Toast.makeText(context, "स्थिति बदलने में विफलता।", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    findView("progressBar")?.visibility = View.GONE
                    Toast.makeText(context, "नेटवर्क त्रुटि: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openDisputeDialog(orderId: String) {
        val dialog = CreateDisputeDialogFragment.newInstance(orderId)
        dialog.show(parentFragmentManager, "CreateDisputeDialogFragment")
    }

    private fun formatStatusName(status: String): String {
        return when (status.uppercase()) {
            "PLACED" -> "नया ऑर्डर दर्ज"
            "ACCEPTED" -> "किसान द्वारा स्वीकृत"
            "IN_TRANSIT" -> "रास्ते में (रवाना)"
            "DELIVERED" -> "सफलतापूर्वक डिलीवर"
            "CANCELLED" -> "रद्द किया गया"
            "DISPUTED" -> "विवाद समीक्षाधीन"
            else -> status
        }
    }

    private fun findTextView(vararg names: String): android.widget.TextView? {
        val root = binding.root
        for (name in names) {
            val id = root.resources.getIdentifier(name, "id", root.context.packageName)
            if (id != 0) {
                val v = root.findViewById<View>(id)
                if (v is android.widget.TextView) return v
            }
        }
        return null
    }

    private fun findView(vararg names: String): View? {
        val root = binding.root
        for (name in names) {
            val id = root.resources.getIdentifier(name, "id", root.context.packageName)
            if (id != 0) {
                val v = root.findViewById<View>(id)
                if (v != null) return v
            }
        }
        return null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
