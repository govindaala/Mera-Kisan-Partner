// app/src/main/java/in/merakisan/app/ui/marketplace/CheckoutBottomSheetFragment.kt
package `in`.merakisan.app.ui.marketplace

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import `in`.merakisan.app.core.config.FeatureManager
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.network.CreateOrderRequest
import `in`.merakisan.app.core.security.SessionManager
import `in`.merakisan.app.databinding.BottomSheetCheckoutBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

class CheckoutBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCheckoutBinding? = null
    private val binding get() = _binding!!

    private var productId: String = ""
    private var productName: String = ""
    private var pricePaise: Long = 0L
    private var unit: String = "kg"
    private var availableStock: Double = 0.0
    private var sellerUid: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            productId = it.getString("productId", "")
            productName = it.getString("productName", "कृषि उपज")
            pricePaise = it.getLong("pricePaise", (it.getDouble("price", 0.0) * 100).toLong())
            unit = it.getString("unit", "kg")
            availableStock = it.getDouble("stockQuantity", 10.0)
            sellerUid = it.getString("sellerUid", "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetCheckoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupProductInfo()
        setupListeners()
    }

    private fun setupProductInfo() {
        val priceRupees = pricePaise / 100.0
        findTextView("tvProductName")?.text = productName
        findTextView("tvProductPrice")?.text = String.format(Locale.getDefault(), "₹%.0f / %s", priceRupees, unit)
        findTextView("tvAvailableStock")?.text = "उपलब्ध: $availableStock$unit"
        updateCalculations(1.0)
    }

    private fun setupListeners() {
        findView("btnPlaceOrder")?.setOnClickListener {
            validateAndSubmitOrder()
        }
    }

    private fun updateCalculations(qty: Double) {
        val totalRupees = (qty * pricePaise) / 100.0
        findTextView("tvOrderTotal")?.text = String.format(Locale.getDefault(), "कुल राशि: ₹%.0f", totalRupees)
    }

    private fun validateAndSubmitOrder() {
        val sessionManager = SessionManager(requireContext())
        val user = sessionManager.getUserSession()
        if (user.uid.isBlank()) {
            Toast.makeText(context, "ऑर्डर करने के लिए लॉगिन करें।", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        val quantity = findEditText("etQuantity")?.text?.toString()?.toDoubleOrNull() ?: 1.0
        val address = findEditText("etDeliveryAddress")?.text?.toString()?.trim().orEmpty()

        val idempotencyKey = UUID.randomUUID().toString()
        val totalAmountPaise = (quantity * pricePaise).toLong()

        val request = CreateOrderRequest(
            productId = productId,
            sellerUid = sellerUid,
            buyerUid = user.uid,
            quantity = quantity,
            unit = unit,
            totalAmountPaise = totalAmountPaise,
            paymentMethod = "cod",
            deliveryAddress = address,
            buyerPhone = user.phone
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val api = ApiClient.getApiService(requireContext())
                val resp = api.createOrder(idempotencyKey, request)
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful && resp.body()?.success == true) {
                        Toast.makeText(context, "ऑर्डर सफलतापूर्वक दर्ज हुआ!", Toast.LENGTH_LONG).show()
                        dismiss()
                    } else {
                        Toast.makeText(context, "ऑर्डर विफल रहा।", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "त्रुटि: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun findTextView(name: String): android.widget.TextView? {
        val id = resources.getIdentifier(name, "id", requireContext().packageName)
        return if (id != 0) binding.root.findViewById(id) else null
    }

    private fun findEditText(name: String): android.widget.EditText? {
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
