// app/src/main/java/in/merakisan/app/ui/orders/OrderDetailFragment.kt
package in.merakisan.app.ui.orders

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import in.merakisan.app.core.network.ApiClient
import in.merakisan.app.core.network.model.OrderDto
import in.merakisan.app.core.security.SessionManager
import in.merakisan.app.databinding.FragmentOrderDetailBinding
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.util.Locale

class OrderDetailFragment : Fragment() {

    private var _binding: FragmentOrderDetailBinding? = null
    private val binding get() = _binding!!

    private var orderId: String = ""
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

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        orderId = arguments?.getString("orderId") ?: ""
        if (orderId.isBlank()) {
            Toast.makeText(requireContext(), "अमान्य ऑर्डर संदर्भ", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        loadOrderDetails()
        setupDisputeButton()
    }

    private fun loadOrderDetails() {
        lifecycleScope.launch {
            try {
                val apiService = ApiClient.getApiService(requireContext())
                val response = apiService.getOrderDetail(orderId)

                if (response.isSuccessful && response.body()?.success == true) {
                    val order = response.body()?.data
                    if (order != null) {
                        currentOrder = order
                        bindOrderToUi(order)
                    }
                } else {
                    Toast.makeText(requireContext(), "ऑर्डर लोड नहीं हो सका", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "नेटवर्क संपर्क विफल: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun bindOrderToUi(order: OrderDto) {
        val currentUid = SessionManager.getUserUid(requireContext())
        val isSeller = order.sellerUid == currentUid

        binding.tvOrderDetailId.text = "ऑर्डर #${order.orderId.takeLast(8)}"
        binding.tvOrderDetailStatusBadge.text = order.orderStatus

        val totalRupees = order.totalAmountPaise / 100.0
        val unitPriceRupees = (order.totalAmountPaise / order.quantity) / 100.0

        binding.tvOrderDetailProductName.text = order.productName
        binding.tvOrderDetailQuantity.text = String.format(Locale.getDefault(), "मात्रा: %.1f %s @ ₹%.0f/%s", order.quantity, order.unit, unitPriceRupees, order.unit)
        binding.tvOrderDetailTotalAmount.text = String.format(Locale.getDefault(), "कुल देय: ₹%.2f", totalRupees)
        binding.tvOrderDetailPaymentMethod.text = "भुगतान स्थिति: ${order.paymentStatus}"
        binding.tvOrderTimestamp.text = "दिनांक: ${order.createdAt}"
        binding.tvDeliveryAddressText.text = "डिलीवरी पता: ${order.deliveryAddress.ifBlank { "खेत से पिकअप" }}"

        // टाइमलाइन स्ट्रिप
        updateTimelineStrip(order.orderStatus)

        // काउंटरपार्टी संपर्क (किसान या खरीदार)
        if (isSeller) {
            binding.tvCounterpartyTitle.text = "ग्राहक (खरीदार) संपर्क:"
            binding.tvCounterpartyName.text = order.buyerName
            setupContactButtons(order.buyerPhone)
        } else {
            binding.tvCounterpartyTitle.text = "विक्रेता (किसान) संपर्क:"
            binding.tvCounterpartyName.text = order.sellerName
            setupContactButtons(order.sellerPhone)
        }

        // स्टेट मशीन एक्शन बटन (Farmer vs Buyer)
        setupStateActionButtons(order, isSeller)
    }

    private fun updateTimelineStrip(status: String) {
        val timelineText = when (status) {
            "PLACED" -> "⏳ 1. ऑर्डर दर्ज (PLACED) ➔ 2. स्वीकृति प्रतीक्षारत"
            "ACCEPTED" -> "✔ 1. दर्ज ➔ ✔ 2. किसान द्वारा स्वीकृत ➔ ⏳ 3. तैयारी जारी (PROCESSING)"
            "PROCESSING" -> "✔ 1. दर्ज ➔ ✔ 2. स्वीकृत ➔ ⏳ 3. पैकेजिंग जारी (PROCESSING)"
            "IN_TRANSIT" -> "✔ 1. दर्ज ➔ ✔ 2. स्वीकृत ➔ ✔ 3. रवाना (IN_TRANSIT) ➔ ⏳ डिलीवरी शेष"
            "DELIVERED" -> "✔ 1. दर्ज ➔ ✔ 2. स्वीकृत ➔ ✔ 3. डिलीवर (DELIVERED) ➔ ⏳ पूर्णता प्रतीक्षारत"
            "COMPLETED" -> "✔ 1. दर्ज ➔ ✔ 2. स्वीकृत ➔ ✔ 3. डिलीवर ➔ ✔ 4. पूर्ण (COMPLETED)"
            "DISPUTED" -> "⚠️ विवाद विचाराधीन (DISPUTED) — एडमिन हस्तक्षेप सक्रिय"
            "CANCELLED" -> "❌ ऑर्डर रद्द किया गया (CANCELLED)"
            else -> status
        }
        binding.tvTimelineDescription.text = timelineText
    }

    private fun setupContactButtons(phone: String?) {
        binding.btnOrderCall.setOnClickListener {
            if (!phone.isNullOrBlank()) {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
            } else {
                Toast.makeText(requireContext(), "फ़ोन नंबर उपलब्ध नहीं है", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnOrderWhatsApp.setOnClickListener {
            if (!phone.isNullOrBlank()) {
                val url = "https://api.whatsapp.com/send?phone=+91$phone&text=${URLEncoder.encode("नमस्ते, MERA KISAN ऑर्डर #${orderId.takeLast(6)} के संदर्भ में संपर्क किया है।", "UTF-8")}"
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "WhatsApp इंस्टॉल नहीं है", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupStateActionButtons(order: OrderDto, isSeller: Boolean) {
        binding.llBottomActionContainer.visibility = View.VISIBLE

        when {
            // किसान की स्वीकृत क्रियाएं
            isSeller && order.orderStatus == "PLACED" -> {
                binding.btnPrimaryAction.text = "ऑर्डर स्वीकार करें (Accept Order)"
                binding.btnPrimaryAction.setOnClickListener {
                    transitionOrderStatus("ACCEPTED")
                }
            }
            isSeller && (order.orderStatus == "ACCEPTED" || order.orderStatus == "PROCESSING") -> {
                binding.btnPrimaryAction.text = "माल रवाना करें (Mark In Transit)"
                binding.btnPrimaryAction.setOnClickListener {
                    transitionOrderStatus("IN_TRANSIT")
                }
            }

            // खरीदार की स्वीकृत क्रियाएं
            !isSeller && order.orderStatus == "IN_TRANSIT" -> {
                binding.btnPrimaryAction.text = "माल मिल गया — डिलीवरी पुष्टि करें"
                binding.btnPrimaryAction.setOnClickListener {
                    transitionOrderStatus("DELIVERED")
                }
            }
            !isSeller && order.orderStatus == "DELIVERED" -> {
                binding.btnPrimaryAction.text = "संतुष्ट — ऑर्डर पूर्ण करें (Complete)"
                binding.btnPrimaryAction.setOnClickListener {
                    transitionOrderStatus("COMPLETED")
                }
            }

            else -> {
                // कोई क्रिया लंबित नहीं (उदा. CANCELLED, COMPLETED, DISPUTED)
                binding.llBottomActionContainer.visibility = View.GONE
            }
        }
    }

    private fun transitionOrderStatus(nextStatus: String) {
        lifecycleScope.launch {
            try {
                binding.btnPrimaryAction.isEnabled = false
                val apiService = ApiClient.getApiService(requireContext())
                val response = apiService.updateOrderStatus(orderId, nextStatus)

                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(requireContext(), "स्थिति सफलतापूर्वक अद्यतित हुई!", Toast.LENGTH_SHORT).show()
                    loadOrderDetails()
                } else {
                    Toast.makeText(requireContext(), "स्थिति बदलने में विफलता", Toast.LENGTH_SHORT).show()
                    binding.btnPrimaryAction.isEnabled = true
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "त्रुटि: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                binding.btnPrimaryAction.isEnabled = true
            }
        }
    }

    private fun setupDisputeButton() {
        binding.btnRaiseDispute.setOnClickListener {
            CreateDisputeDialogFragment.newInstance(orderId) { reason, desc, _ ->
                transitionOrderStatus("DISPUTED")
                Toast.makeText(requireContext(), "विवाद दर्ज हुआ: $reason", Toast.LENGTH_LONG).show()
            }.show(childFragmentManager, "CreateDispute")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
