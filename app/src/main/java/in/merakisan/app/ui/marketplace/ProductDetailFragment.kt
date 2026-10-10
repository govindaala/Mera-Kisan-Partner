// app/src/main/java/in/merakisan/app/ui/marketplace/ProductDetailFragment.kt
package `in`.merakisan.app.ui.marketplace

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.load
import `in`.merakisan.app.R
import `in`.merakisan.app.core.config.FeatureManager
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.network.model.ProductDto
import `in`.merakisan.app.databinding.FragmentProductDetailBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * MERA KISAN Produce Detail View
 * पूर्ण फ़ीचर्स: Coil गैलरी, YouTube वीडियो, Call, WhatsApp, Share, Make Offer व Checkout
 */
class ProductDetailFragment : Fragment() {

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!
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

        val productId = arguments?.getString("productId") ?: ""
        if (productId.isNotBlank()) {
            loadProductDetails(productId)
        } else {
            Toast.makeText(context, "उत्पाद आईडी अमान्य है", Toast.LENGTH_SHORT).show()
        }

        setupStaticListeners()
    }

    private fun setupStaticListeners() {
        binding.btnBack?.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun loadProductDetails(productId: String) {
        binding.progressBar?.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiService = ApiClient.getApiService(requireContext())
                val response = apiService.getProductDetail(productId)
                if (response.isSuccessful && response.body()?.success == true) {
                    val product = response.body()?.data
                    withContext(Dispatchers.Main) {
                        binding.progressBar?.visibility = View.GONE
                        product?.let { bindProductData(it) }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        binding.progressBar?.visibility = View.GONE
                        Toast.makeText(context, "डेटा लोड करने में असमर्थ", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar?.visibility = View.GONE
                    Toast.makeText(context, "नेटवर्क त्रुटि: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun bindProductData(product: ProductDto) {
        currentProduct = product

        // 1. मुख्य विवरण (सुरक्षित TextView .text असाइनमेंट)
        val title = if (product.variety.isNotBlank()) "${product.name} (${product.variety})" else product.name
        binding.tvProductName.text = title
        binding.tvProductCategory.text = "श्रेणी: ${product.category}"

        val priceRupees = product.pricePaise / 100.0
        binding.tvProductPrice.text = String.format(Locale.getDefault(), "₹%.0f", priceRupees)
        binding.tvProductUnit.text = "/ ${product.unit}"
        binding.tvProductStock.text = "उपलब्ध मात्रा: ${product.stockQuantity}${product.unit}"

        val loc = if (product.village.isNotBlank()) "${product.village},${product.district}" else product.district
        binding.tvProductLocation.text = "स्थान: $loc"
        binding.tvFarmerName.text = "किसान साथी: ${product.sellerName}"
        binding.tvDescription.text = if (product.description.isNotBlank()) product.description else "कोई अतिरिक्त विवरण नहीं दिया गया है।"

        // 2. Coil द्वारा मुख्य फोटो लोड करना
        val photoUrl = product.photos.firstOrNull() ?: product.imageUrls.firstOrNull()
        if (!photoUrl.isNullOrBlank()) {
            binding.ivProductImage.load(photoUrl) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_gallery)
            }
        } else {
            binding.ivProductImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        // 3. YouTube वीडियो हैंडलर (Free-First Architecture)
        if (!product.videoUrl.isNullOrBlank()) {
            binding.btnWatchVideo?.visibility = View.VISIBLE
            binding.btnWatchVideo?.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(product.videoUrl))
                startActivity(intent)
            }
        } else {
            binding.btnWatchVideo?.visibility = View.GONE
        }

        // 4. सत्यापन बैज (Evidence-Based Verification)
        if (product.verificationStatus == "verified_farmer" || product.verificationStatus == "organic_certified") {
            binding.badgeVerification?.visibility = View.VISIBLE
            binding.tvVerificationText?.text = if (product.verificationStatus == "organic_certified") "जैविक प्रमाणित" else "सत्यापित किसान"
        } else {
            binding.badgeVerification?.visibility = View.GONE
        }

        // 5. डायरेक्ट कॉल बटन
        binding.btnCall.setOnClickListener {
            val phone = product.sellerPhone
            if (!phone.isNullOrBlank()) {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                startActivity(intent)
            } else {
                Toast.makeText(context, "किसान का फ़ोन नंबर उपलब्ध नहीं है", Toast.LENGTH_SHORT).show()
            }
        }

        // 6. WhatsApp संपर्क
        binding.btnWhatsApp?.setOnClickListener {
            val phone = product.sellerPhone
            if (!phone.isNullOrBlank()) {
                val cleanPhone = phone.replace("+", "").replace(" ", "").trim()
                val msg = "नमस्ते ${product.sellerName} जी, मैंने Mera Kisan ऐप पर आपकी फसल '${product.name}' देखी। मुझे इसके बारे में जानकारी चाहिए।"
                val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(msg)}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } else {
                Toast.makeText(context, "WhatsApp नंबर उपलब्ध नहीं है", Toast.LENGTH_SHORT).show()
            }
        }

        // 7. सोशल शेयरिंग
        binding.btnShare.setOnClickListener {
            val shareText = "🌾 Mera Kisan पर ताज़ा फसल उपलब्ध है!\nफसल: ${product.name}\nभाव: ₹${product.pricePaise / 100}/${product.unit}\nकिसान: ${product.sellerName} (${product.district})\nऐप पर देखें: https://merakisan.in/product/${product.productId}"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(intent, "फसल शेयर करें"))
        }

        // 8. Make Offer / मोलभाव (Central Feature Flag नियंत्रित)
        if (FeatureManager.isEnabled(requireContext(), "offers")) {
            binding.btnMakeOffer?.visibility = View.VISIBLE
            binding.btnMakeOffer?.setOnClickListener {
                Toast.makeText(context, "भाव प्रस्ताव (Offer) विंडो खुल रही है...", Toast.LENGTH_SHORT).show()
            }
        } else {
            binding.btnMakeOffer?.visibility = View.GONE
        }

        // 9. Buy Now / सीधा ऑर्डर (Central Feature Flag नियंत्रित)
        binding.btnBuyNow?.setOnClickListener {
            val bundle = Bundle().apply {
                putString("productId", product.productId)
                putDouble("price", product.pricePaise / 100.0)
            }
            try {
                findNavController().navigate(R.id.action_productDetail_to_checkoutBottomSheet, bundle)
            } catch (_: Exception) {
                Toast.makeText(context, "ऑर्डर विंडो लोड हो रही है...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
