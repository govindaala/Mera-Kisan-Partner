// app/src/main/java/in/merakisan/app/ui/marketplace/ProductDetailFragment.kt
package in.merakisan.app.ui.marketplace

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import in.merakisan.app.core.config.FeatureManager
import in.merakisan.app.core.network.model.ProductDto
import in.merakisan.app.databinding.DialogMakeOfferBinding
import in.merakisan.app.databinding.FragmentProductDetailBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.util.Locale

class ProductDetailFragment : Fragment() {

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductDetailViewModel by viewModels()
    private var currentProduct: ProductDto? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        val productId = arguments?.getString("productId") ?: return
        viewModel.loadProduct(productId)

        observeProduct()
    }

    private fun observeProduct() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is ProductDetailUiState.Loading -> {
                        // प्रोग्रेस स्टेट
                    }
                    is ProductDetailUiState.Success -> {
                        currentProduct = state.product
                        bindProductData(state.product)
                        setupActionButtons(state.product)
                    }
                    is ProductDetailUiState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun bindProductData(product: ProductDto) {
        val context = requireContext()

        // 1. शीर्षक व किस्म
        binding.tvTitle.text = if (!product.variety.isNullOrBlank()) {
            "${product.name} (${product.variety})"
        } else {
            product.name
        }

        binding.tvCategory.text = "श्रेणी: ${product.category}"

        // 2. पूर्णांक पैसे (price_paise) से रुपये में रूपांतरण
        val priceRupees = product.pricePaise / 100.0
        binding.tvPrice.text = String.format(Locale.getDefault(), "₹%.0f", priceRupees)
        binding.tvUnit.text = "/ ${product.unit}"
        binding.tvStock.text = "उपलब्ध: ${product.stockQuantity} ${product.unit}"

        // 3. छोटी मात्रा बैज (1–25 kg)
        val isSmallQty = product.stockQuantity <= 25.0 || (product.minOrderQuantity ?: 1.0) <= 25.0
        binding.tvSmallQtyBadge.visibility = if (isSmallQty) View.VISIBLE else View.GONE

        // 4. सत्यापन स्तर
        when (product.verificationStatus) {
            "organic_certified" -> {
                binding.tvVerificationBadge.visibility = View.VISIBLE
                binding.tvVerificationBadge.text = "🌿 Organic Certified"
            }
            "verified_farmer" -> {
                binding.tvVerificationBadge.visibility = View.VISIBLE
                binding.tvVerificationBadge.text = "✔ Mera Kisan Verified"
            }
            else -> {
                binding.tvVerificationBadge.visibility = View.GONE
            }
        }

        // 5. कटाई तिथि व न्यूनतम ऑर्डर
        if (!product.harvestDate.isNullOrBlank()) {
            binding.tvHarvestDate.text = "कटाई तिथि: ${product.harvestDate} (ताज़ा फसल)"
        } else {
            binding.tvHarvestDate.text = "ताज़ा फसल उपलब्ध"
        }
        binding.tvMinOrder.text = "न्यूनतम ऑर्डर: ${product.minOrderQuantity ?: 1.0} ${product.unit}"

        // 6. किसान व लोकेशन प्राइवेसी
        binding.tvFarmerName.text = product.sellerName
        val loc = if (!product.village.isNullOrBlank()) {
            "ग्राम: ${product.village} • ज़िला: ${product.district}"
        } else {
            "ज़िला: ${product.district}"
        }
        binding.tvFarmerLocation.text = loc

        // 7. विवरण
        binding.tvDescription.text = product.description ?: "किसान द्वारा कोई अतिरिक्त विवरण उपलब्ध नहीं है।"

        // 8. इमेज
        if (product.photos.isNotEmpty()) {
            binding.ivProductHero.load(product.photos[0]) {
                crossfade(true)
            }
        }
    }

    private fun setupActionButtons(product: ProductDto) {
        val context = requireContext()

        // रिमोट फ़ीचर गार्ड्स (FeatureManager)
        val isCallEnabled = FeatureManager.isEnabled(context, "call")
        val isWhatsAppEnabled = FeatureManager.isEnabled(context, "whatsapp")
        val isMakeOfferEnabled = FeatureManager.isEnabled(context, "make_offer")
        val isBuyNowEnabled = FeatureManager.isEnabled(context, "buy_now") || FeatureManager.isEnabled(context, "online_order")

        binding.btnCallFarmer.visibility = if (isCallEnabled && !product.sellerPhone.isNullOrBlank()) View.VISIBLE else View.GONE
        binding.btnWhatsAppFarmer.visibility = if (isWhatsAppEnabled && !product.sellerPhone.isNullOrBlank()) View.VISIBLE else View.GONE
        binding.btnMakeOffer.visibility = if (isMakeOfferEnabled) View.VISIBLE else View.GONE
        binding.btnBuyNow.visibility = if (isBuyNowEnabled) View.VISIBLE else View.GONE

        binding.btnCallFarmer.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${product.sellerPhone}")
            }
            startActivity(intent)
        }

        binding.btnWhatsAppFarmer.setOnClickListener {
            val phone = product.sellerPhone ?: return@setOnClickListener
            val msg = "नमस्ते ${product.sellerName} जी, मैंने MERA KISAN ऐप पर आपकी फसल '${product.name}' (दर: ₹${product.pricePaise / 100}/${product.unit}) देखी है।"
            val url = "https://api.whatsapp.com/send?phone=+91$phone&text=${URLEncoder.encode(msg, "UTF-8")}"
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "WhatsApp इंस्टॉल नहीं है", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnMakeOffer.setOnClickListener {
            showMakeOfferDialog(product)
        }

        binding.btnBuyNow.setOnClickListener {
            Toast.makeText(requireContext(), "ऑर्डर चेकआउट प्रवाह प्रारंभ...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMakeOfferDialog(product: ProductDto) {
        val dialogBinding = DialogMakeOfferBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        val pricePerUnit = product.pricePaise / 100.0
        dialogBinding.tvCurrentListedPrice.text = "वर्तमान दर: ₹$pricePerUnit / ${product.unit}"

        fun updateCalculation() {
            val qty = dialogBinding.etQuantity.text.toString().toDoubleOrNull() ?: 0.0
            val offeredRate = dialogBinding.etOfferedPrice.text.toString().toDoubleOrNull() ?: 0.0
            val total = qty * offeredRate
            dialogBinding.tvOfferTotalEstimation.text = String.format(Locale.getDefault(), "कुल प्रस्तावित राशि: ₹%.2f", total)
        }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { updateCalculation() }
            override fun afterTextChanged(s: Editable?) {}
        }

        dialogBinding.etQuantity.addTextChangedListener(watcher)
        dialogBinding.etOfferedPrice.addTextChangedListener(watcher)

        dialogBinding.btnCancelOffer.setOnClickListener { dialog.dismiss() }

        dialogBinding.btnSubmitOffer.setOnClickListener {
            val qty = dialogBinding.etQuantity.text.toString().toDoubleOrNull()
            val offeredRate = dialogBinding.etOfferedPrice.text.toString().toDoubleOrNull()

            if (qty == null || qty <= 0.0) {
                Toast.makeText(requireContext(), "मान्य मात्रा दर्ज करें", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (offeredRate == null || offeredRate <= 0.0) {
                Toast.makeText(requireContext(), "मान्य दर दर्ज करें", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dialog.dismiss()
            Toast.makeText(
                requireContext(),
                "प्रस्ताव किसान को प्रेषित किया गया: $qty ${product.unit} @ ₹$offeredRate/${product.unit}",
                Toast.LENGTH_LONG
            ).show()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
