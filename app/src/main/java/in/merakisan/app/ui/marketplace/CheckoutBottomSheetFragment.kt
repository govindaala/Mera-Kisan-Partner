// app/src/main/java/in/merakisan/app/ui/marketplace/CheckoutBottomSheetFragment.kt
package in.merakisan.app.ui.marketplace

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import in.merakisan.app.R
import in.merakisan.app.core.config.FeatureManager
import in.merakisan.app.core.network.ApiClient
import in.merakisan.app.core.network.model.CreateOrderRequest
import in.merakisan.app.core.network.model.ProductDto
import in.merakisan.app.databinding.BottomSheetCheckoutBinding
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

class CheckoutBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCheckoutBinding? = null
    private val binding get() = _binding!!

    private var product: ProductDto? = null
    private var onOrderPlacedCallback: (() -> Unit)? = null

    companion object {
        fun newInstance(
            product: ProductDto,
            onOrderPlaced: () -> Unit
        ): CheckoutBottomSheetFragment {
            return CheckoutBottomSheetFragment().apply {
                this.product = product
                this.onOrderPlacedCallback = onOrderPlaced
            }
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
        val prod = product ?: run {
            dismiss()
            return
        }

        bindProductDetails(prod)
        setupPriceCalculation(prod)
        setupFeatureGuards()
        setupConfirmButton(prod)
    }

    private fun bindProductDetails(prod: ProductDto) {
        val priceRupees = prod.pricePaise / 100.0
        binding.tvCheckoutProductName.text = "${prod.name} • ₹$priceRupees / ${prod.unit}"
        binding.tvCheckoutAvailableStock.text =
            "उपलब्ध स्टॉक: ${prod.stockQuantity} ${prod.unit} • न्यूनतम ऑर्डर: ${prod.minOrderQuantity ?: 1.0} ${prod.unit}"

        // डिफ़ॉल्ट न्यूनतम ऑर्डर मात्रा सेट करना
        val defaultQty = prod.minOrderQuantity ?: 1.0
        binding.etOrderQuantity.setText(defaultQty.toString())
        updateTotal(prod, defaultQty)
    }

    private fun setupPriceCalculation(prod: ProductDto) {
        binding.etOrderQuantity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val qty = s?.toString()?.toDoubleOrNull() ?: 0.0
                updateTotal(prod, qty)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updateTotal(prod: ProductDto, quantity: Double) {
        val unitPriceRupees = prod.pricePaise / 100.0
        val total = quantity * unitPriceRupees
        binding.tvCalculatedTotal.text = String.format(Locale.getDefault(), "₹%.2f", total)
    }

    private fun setupFeatureGuards() {
        // रिमोट फ़ीचर गार्ड: यदि एडमिन ने पेमेंट चालू की है तो ही ऑनलाइन भुगतान विकल्प दिखे
        val isPaymentEnabled = FeatureManager.isEnabled(requireContext(), "payment") ||
                FeatureManager.isEnabled(requireContext(), "token_payment")
        binding.rbPaymentOnline.visibility = if (isPaymentEnabled) View.VISIBLE else View.GONE
    }

    private fun setupConfirmButton(prod: ProductDto) {
        binding.btnConfirmOrder.setOnClickListener {
            val qtyStr = binding.etOrderQuantity.text.toString().trim()
            val quantity = qtyStr.toDoubleOrNull()

            if (quantity == null || quantity <= 0.0) {
                Toast.makeText(requireContext(), "मान्य मात्रा दर्ज करें", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val minOrder = prod.minOrderQuantity ?: 1.0
            if (quantity < minOrder) {
                Toast.makeText(requireContext(), "न्यूनतम ऑर्डर मात्रा $minOrder ${prod.unit} है", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (quantity > prod.stockQuantity) {
                Toast.makeText(requireContext(), "उपलब्ध स्टॉक केवल ${prod.stockQuantity} ${prod.unit} है", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val address = binding.etDeliveryAddress.text.toString().trim()
            if (binding.rbFarmerDelivery.isChecked && address.isBlank()) {
                Toast.makeText(requireContext(), "डिलीवरी का पता दर्ज करें", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val deliveryType = if (binding.rbFarmerDelivery.isChecked) "farmer_delivery" else "buyer_pickup"
            val paymentMethod = if (binding.rbPaymentOnline.isChecked) "online" else "cod"

            // Idempotency Key: बैकएंड पर दोहरे चार्ज व डुप्लिकेट ऑर्डर रोकने के लिए UUID
            val idempotencyKey = UUID.randomUUID().toString()

            executeOrderCreation(
                idempotencyKey = idempotencyKey,
                productId = prod.productId,
                quantity = quantity,
                deliveryType = deliveryType,
                paymentMethod = paymentMethod,
                address = address
            )
        }
    }

    private fun executeOrderCreation(
        idempotencyKey: String,
        productId: String,
        quantity: Double,
        deliveryType: String,
        paymentMethod: String,
        address: String
    ) {
        binding.btnConfirmOrder.isEnabled = false
        binding.pbPlacingOrder.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val apiService = ApiClient.getApiService(requireContext())
                val request = CreateOrderRequest(
                    productId = productId,
                    quantity = quantity,
                    deliveryType = deliveryType,
                    paymentMethod = paymentMethod,
                    deliveryAddress = address,
                    notes = null
                )

                val response = apiService.createOrder(idempotencyKey, request)
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(requireContext(), "ऑर्डर सफलतापूर्वक दर्ज हो गया!", Toast.LENGTH_LONG).show()
                    onOrderPlacedCallback?.invoke()
                    dismiss()
                } else {
                    val errMsg = response.body()?.error?.message ?: "ऑर्डर प्रोसेस करने में विफलता (HTTP ${response.code()})"
                    Toast.makeText(requireContext(), errMsg, Toast.LENGTH_LONG).show()
                    binding.btnConfirmOrder.isEnabled = true
                    binding.pbPlacingOrder.visibility = View.GONE
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "नेटवर्क संपर्क त्रुटि: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                binding.btnConfirmOrder.isEnabled = true
                binding.pbPlacingOrder.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
