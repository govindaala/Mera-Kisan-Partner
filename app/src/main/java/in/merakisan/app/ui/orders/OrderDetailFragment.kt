// app/src/main/java/in/merakisan/app/ui/orders/OrderDetailFragment.kt
package `in`.merakisan.app.ui.orders

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.network.UpdateOrderStatusRequest
import `in`.merakisan.app.core.network.model.OrderDto
import `in`.merakisan.app.core.security.SessionManager
import `in`.merakisan.app.databinding.FragmentOrderDetailBinding
import `in`.merakisan.app.ui.reviews.AddReviewDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * MERA KISAN — विस्तृत ऑर्डर प्रबंधन स्क्रीन
 * Master Spec v1.1 Section 8, 9, 11 & 18: State Machine, Review & Dispute Integration
 */
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

        setupListeners()
        loadOrderDetail()
    }

    private fun setupListeners() {
        findView("btnBack", "ivBack", "btnNavBack")?.setOnClickListener {
            findNavController().navigateUp()
        }

        // विवाद निवारण (Dispute) बटन
        findView("btnDispute", "btnOpenDispute", "btnReportIssue")?.setOnClickListener {
            if (currentOrderId.isNotBlank()) {
                val disputeDialog = CreateDisputeDialogFragment.newInstance(currentOrderId) { isSubmitted ->
                    if (isSubmitted) {
                        loadOrderDetail()
                    }
                }
                disputeDialog.show(parentFragmentManager, "CreateDisputeDialogFragment")
            }
        }

        // समीक्षा एवं 5-स्टार रेटिंग बटन (केवल डिलीवरी के बाद)
        findView("btnLeaveReview", "btnReview", "btnAddReview", "btnRateProduct")?.setOnClickListener {
            val order = currentOrder
            if (order != null) {
                val reviewDialog = AddReviewDialogFragment.newInstance(
                    orderId = order.orderId,
                    productId = order.productId,
                    sellerUid = order.sellerUid,
                    productName = order.productName
                ) {
                    loadOrderDetail()
                }
                reviewDialog.show(parentFragmentManager, "AddReviewDialogFragment")
            }
        }
    }

    private fun loadOrderDetail() {
        if (currentOrderId.isBlank()) return

        setLoading(true)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = ApiClient.getApiService(requireContext())
                val resp = api.getOrderDetail(currentOrderId)
                withContext(Dispatchers.Main) {
                    setLoading(false)
                    if (resp.isSuccessful && resp.body()?.success == true) {
                        val order = resp.body()?.data
                        if (order != null) {
                            currentOrder = order
                            bindOrder(order)
                        }
                    } else {
                        Toast.makeText(requireContext(), "ऑर्डर विवरण लोड करने में समस्या आई", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setLoading(false)
                    Toast.makeText(requireContext(), "नेटवर्क त्रुटि: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun bindOrder(order: OrderDto) {
        val sessionManager = SessionManager(requireContext())
        val userSession = sessionManager.getUserSession()
        val isFarmer = order.sellerUid == userSession.uid
        val isBuyer = order.buyerUid == userSession.uid

        // 1. टेक्स्ट फ़ील्ड्स बाइंडिंग
        findTextView("tvOrderId", "tvOrderNumber")?.text = "ऑर्डर ID: #${order.orderId.takeLast(8)}"
        findTextView("tvProductName", "tvCropName")?.text = order.productName
        findTextView("tvOrderQuantity", "tvQuantity")?.text = "मात्रा: ${order.quantity}${order.unit}"

        // Master Spec Rule 14: वित्तीय अखंडता (Paise to Rupee display conversion)
        val totalRupees = order.totalAmountPaise / 100.0
        findTextView("tvTotalAmount", "tvPrice")?.text = String.format(Locale.getDefault(), "कुल राशि: ₹%.0f", totalRupees)

        val status = order.orderStatus.uppercase(Locale.getDefault())
        findTextView("tvOrderStatus", "tvStatus")?.text = "स्थिति: $status"

        findTextView("tvDeliveryAddress", "tvAddress")?.apply {
            text = "डिलीवरी पता: ${order.deliveryAddress.ifBlank { "खेत से सीधा पिकअप / स्थानीय डिलीवरी" }}"
            isVisible = true
        }

        // 2. किसान एवं खरीदार भूमिका-आधारित एक्शन बटन्स का प्रबंधन
        val primaryBtn = findButton("btnPrimaryAction", "btnAction", "btnUpdateStatus")
        val reviewBtn = findView("btnLeaveReview", "btnReview", "btnAddReview", "btnRateProduct")
        val disputeBtn = findView("btnDispute", "btnOpenDispute", "btnReportIssue")

        // समीक्षा बटन केवल खरीदार को और केवल 'DELIVERED' स्थिति पर दिखाई देगा
        reviewBtn?.isVisible = isBuyer && (status == "DELIVERED" || status == "COMPLETED")

        // विवाद बटन सामान्य सक्रिय ऑर्डर्स या हालिया डिलीवरी पर दिखाई देगा
        disputeBtn?.isVisible = status != "CANCELLED" && status != "REFUNDED" && status != "DISPUTED"

        if (isFarmer) {
            // किसान के लिए स्थिति आगे बढ़ाने के विकल्प
            when (status) {
                "CREATED", "PLACED" -> {
                    primaryBtn?.isVisible = true
                    primaryBtn?.text = "ऑर्डर स्वीकार करें (Accept)"
                    primaryBtn?.setOnClickListener {
                        updateStatus("ACCEPTED", "किसान द्वारा ऑर्डर स्वीकार किया गया")
                    }
                }
                "ACCEPTED" -> {
                    primaryBtn?.isVisible = true
                    primaryBtn?.text = "माल रवाना करें (Mark Shipped)"
                    primaryBtn?.setOnClickListener {
                        updateStatus("SHIPPED", "फसल खेत से रवाना कर दी गई है")
                    }
                }
                "SHIPPED", "IN_TRANSIT" -> {
                    primaryBtn?.isVisible = true
                    primaryBtn?.text = "डिलीवरी पूर्ण चिन्हित करें (Delivered)"
                    primaryBtn?.setOnClickListener {
                        updateStatus("DELIVERED", "उपज खरीदार को सफलतापूर्वक डिलीवर हुई")
                    }
                }
                else -> {
                    primaryBtn?.isVisible = false
                }
            }
        } else {
            // खरीदार केवल तभी डिलीवरी की पुष्टि कर सकता है जब माल रास्ते में हो
            if (status == "SHIPPED" || status == "IN_TRANSIT") {
                primaryBtn?.isVisible = true
                primaryBtn?.text = "मुझे माल मिल गया (Confirm Delivery)"
                primaryBtn?.setOnClickListener {
                    updateStatus("DELIVERED", "खरीदार द्वारा डिलीवरी की पुष्टि की गई")
                }
            } else {
                primaryBtn?.isVisible = false
            }
        }
    }

    private fun updateStatus(nextStatus: String, notes: String) {
        setLoading(true)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = ApiClient.getApiService(requireContext())
                val req = UpdateOrderStatusRequest(
                    nextStatus = nextStatus,
                    status = nextStatus,
                    notes = notes
                )
                val resp = api.updateOrderStatus(currentOrderId, req)
                withContext(Dispatchers.Main) {
                    setLoading(false)
                    if (resp.isSuccessful) {
                        Toast.makeText(requireContext(), "ऑर्डर स्थिति अद्यतित: $nextStatus", Toast.LENGTH_SHORT).show()
                        loadOrderDetail()
                    } else {
                        Toast.makeText(requireContext(), "स्थिति अपडेट करने में समस्या आई", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setLoading(false)
                    Toast.makeText(requireContext(), "त्रुटि: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        findView("pbOrderDetailsLoading", "progressBar", "pbLoading")?.isVisible = isLoading
    }

    private fun findTextView(vararg candidateNames: String): TextView? {
        for (name in candidateNames) {
            val id = resources.getIdentifier(name, "id", requireContext().packageName)
            if (id != 0) {
                val v = binding.root.findViewById<View>(id)
                if (v is TextView) return v
            }
        }
        return null
    }

    private fun findButton(vararg candidateNames: String): Button? {
        for (name in candidateNames) {
            val id = resources.getIdentifier(name, "id", requireContext().packageName)
            if (id != 0) {
                val v = binding.root.findViewById<View>(id)
                if (v is Button) return v
            }
        }
        return null
    }

    private fun findView(vararg candidateNames: String): View? {
        for (name in candidateNames) {
            val id = resources.getIdentifier(name, "id", requireContext().packageName)
            if (id != 0) {
                val v = binding.root.findViewById<View>(id)
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
