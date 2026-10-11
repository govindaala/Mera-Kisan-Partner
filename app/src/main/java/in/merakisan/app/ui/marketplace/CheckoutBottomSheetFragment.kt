// app/src/main/java/in/merakisan/app/ui/marketplace/CheckoutBottomSheetFragment.kt
package `in`.merakisan.app.ui.marketplace

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import `in`.merakisan.app.core.config.FeatureManager
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.network.model.CreateOrderRequest
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
            // मूल्य को केवल Integer Paise (Long) में प्राप्त करना
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
        setupPaymentGatewayOption()
        setupListeners()
    }

    private fun setupProductInfo() {
        findTextView("tvProductName", "tvTitle")?.text = productName
        val priceRupees = pricePaise / 100.0
        findTextView("tvProductPrice", "tvPrice")?.text =
            String.format(Locale.getDefault(), "₹%.0f / %s", priceRupees, unit)
        findTextView("tvAvailableStock", "tvStock")?.text =
            "उपलब्ध स्टॉक: $availableStock$unit"

        updateCalculations(1.0)
    }

    private fun setupPaymentGatewayOption() {
        // मास्टर स्पेक: जब तक पेमेंट गेटवे लाइव न हो, केवल COD / सीधा संपर्क ही मान्य होगा
        val isPaymentEnabled = FeatureManager.isEnabled(requireContext(), "payment")
        val rbOnline = findView("rbPaymentOnline", "rbOnline")
        val rbCod = findView("rbPaymentCod", "rbCod")
        val tvNote = findTextView("tvPaymentNote", "tvNote")

        if (!isPaymentEnabled) {
            rbOnline?.visibility = View.GONE
            rbCod?.let {
                if (it is android.widget.RadioButton) it.isChecked = true
            }
            tvNote?.text = "वर्तमान में केवल डिलीवरी पर नकद भुगतान (COD) या किसान से सीधा संपर्क मान्य है।"
            tvNote?.visibility = View.VISIBLE
        } else {
            rbOnline?.visibility = View.VISIBLE
        }
    }

    private fun setupListeners() {
        val etQty = findEditText("etQuantity", "etOrderQuantity")
        etQty?.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val qty = s?.toString()?.toDoubleOrNull() ?: 0.0
                updateCalculations(qty)
            }
        })

        findView("btnPlaceOrder", "btnConfirmOrder")?.setOnClickListener {
            validateAndSubmitOrder()
        }
    }

    private fun updateCalculations(quantity: Double) {
        val totalPaise = (quantity * pricePaise).toLong()
        val totalRupees = totalPaise / 100.0
        findTextView("tvOrderTotal", "tvTotalAmount")?.text =
            String.format(Locale.getDefault(), "कुल राशि: ₹%.0f", totalRupees)
    }

    private fun validateAndSubmitOrder() {
        val sessionManager = SessionManager(requireContext())
        val user = sessionManager.getUserSession()

        if (user == null || user.uid.isBlank()) {
            Toast.makeText(context, "ऑर्डर करने के लिए कृपया पहले लॉगिन करें।", Toast.LENGTH_LONG).show()
            dismiss()
            return
        }

        val etQty = findEditText("etQuantity", "etOrderQuantity")
        val etAddress = findEditText("etDeliveryAddress", "etAddress")

        val quantity = etQty?.text?.toString()?.toDoubleOrNull() ?: 0.0
        val address = etAddress?.text?.toString()?.trim().orEmpty()

        if (quantity <= 0.0) {
            Toast.makeText(context, "कृपया मान्य मात्रा दर्ज करें।", Toast.LENGTH_SHORT).show()
            return
        }

        if (quantity > availableStock) {
            Toast.makeText(context, "मात्रा उपलब्ध स्टॉक ($availableStock$unit) से अधिक नहीं हो सकती।", Toast.LENGTH_SHORT).show()
            return
        }

        if (address.isBlank()) {
            Toast.makeText(context, "कृपया डिलीवरी का पता या गाँव दर्ज करें।", Toast.LENGTH_SHORT).show()
            return
        }

        val totalAmountPaise = (quantity * pricePaise).toLong()
        val idempotencyKey = UUID.randomUUID().toString()

        val rbOnline = findView("rbPaymentOnline", "rbOnline")
        val isOnlineChecked = if (rbOnline is android.widget.RadioButton) rbOnline.isChecked else false
        val paymentMethod = if (isOnlineChecked && FeatureManager.isEnabled(requireContext(), "payment")) "online" else "cod"

        val orderRequest = CreateOrderRequest(
            productId = productId,
            sellerUid = sellerUid,
            buyerUid = user.uid,
            quantity = quantity,
            unit = unit,
            totalAmountPaise = totalAmountPaise,
            paymentMethod = paymentMethod,
            deliveryAddress = address,
            buyerPhone = user.phone.orEmpty()
        )

        submitOrderToBackend(idempotencyKey, orderRequest)
    }

    private fun submitOrderToBackend(idempotencyKey: String, request: CreateOrderRequest) {
        findView("progressBar")?.visibility = View.VISIBLE
        findView("btnPlaceOrder", "btnConfirmOrder")?.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiService = ApiClient.getApiService(requireContext())
                val response = apiService.createOrder(idempotencyKey, request)

                withContext(Dispatchers.Main) {
                    findView("progressBar")?.visibility = View.GONE
                    findView("btnPlaceOrder", "btnConfirmOrder")?.isEnabled = true

                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(context, "ऑर्डर सफलतापूर्वक दर्ज कर लिया गया!", Toast.LENGTH_LONG).show()
                        dismiss()
                    } else {
                        val errMsg = response.errorBody()?.string() ?: "ऑर्डर दर्ज करने में विफलता।"
                        Toast.makeText(context, "त्रुटि: $errMsg", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    findView("progressBar")?.visibility = View.GONE
                    findView("btnPlaceOrder", "btnConfirmOrder")?.isEnabled = true
                    Toast.makeText(context, "नेटवर्क त्रुटि: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
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

    private fun findEditText(vararg names: String): android.widget.EditText? {
        val root = binding.root
        for (name in names) {
            val id = root.resources.getIdentifier(name, "id", root.context.packageName)
            if (id != 0) {
                val v = root.findViewById<View>(id)
                if (v is android.widget.EditText) return v
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
