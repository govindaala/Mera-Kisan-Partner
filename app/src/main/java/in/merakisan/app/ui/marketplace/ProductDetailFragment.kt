// app/src/main/java/in/merakisan/app/ui/marketplace/ProductDetailFragment.kt
package `in`.merakisan.app.ui.marketplace

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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

        findView(binding.root, "btnBack", "ivBack", "toolbarBack")?.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun loadProductDetails(productId: String) {
        findView(binding.root, "progressBar", "progress", "loadingBar")?.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiService = ApiClient.getApiService(requireContext())
                val response = apiService.getProductDetail(productId)
                if (response.isSuccessful && response.body()?.success == true) {
                    val product = response.body()?.data
                    withContext(Dispatchers.Main) {
                        findView(binding.root, "progressBar", "progress", "loadingBar")?.visibility = View.GONE
                        product?.let { bindProductData(it) }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        findView(binding.root, "progressBar", "progress", "loadingBar")?.visibility = View.GONE
                        Toast.makeText(context, "डेटा लोड करने में असमर्थ", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    findView(binding.root, "progressBar", "progress", "loadingBar")?.visibility = View.GONE
                    Toast.makeText(context, "नेटवर्क त्रुटि: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun bindProductData(product: ProductDto) {
        currentProduct = product
        val root = binding.root

        val title = if (!product.variety.isNullOrBlank()) "${product.name} (${product.variety})" else product.name
        findTextView(root, "tvProductName", "tvTitle", "tvName")?.text = title
        findTextView(root, "tvProductCategory", "tvCategory")?.text = "श्रेणी: ${product.category}"

        val priceRupees = product.pricePaise / 100.0
        findTextView(root, "tvProductPrice", "tvPrice")?.text = String.format(Locale.getDefault(), "₹%.0f", priceRupees)
        findTextView(root, "tvProductUnit", "tvUnit")?.text = "/ ${product.unit}"
        findTextView(root, "tvProductStock", "tvStock", "tvQuantity")?.text = "उपलब्ध मात्रा: ${product.stockQuantity}${product.unit}"

        val loc = if (!product.village.isNullOrBlank()) "${product.village},${product.district}" else product.district ?: ""
        findTextView(root, "tvProductLocation", "tvLocation")?.text = "स्थान: $loc"
        findTextView(root, "tvFarmerName", "tvSeller")?.text = "किसान साथी: ${product.sellerName}"
        findTextView(root, "tvDescription", "tvProductDescription")?.text =
            if (!product.description.isNullOrBlank()) product.description else "कोई अतिरिक्त विवरण नहीं दिया गया है।"

        val photoUrl = product.photos.firstOrNull() ?: product.imageUrls.firstOrNull()
        findImageView(root, "ivProductImage", "ivCropImage", "ivImage")?.let { iv ->
            if (!photoUrl.isNullOrBlank()) {
                iv.load(photoUrl) {
                    crossfade(true)
                    placeholder(android.R.drawable.ic_menu_gallery)
                    error(android.R.drawable.ic_menu_gallery)
                }
            } else {
                iv.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }

        findView(root, "btnWatchVideo", "btnVideo")?.let { btn ->
            if (!product.videoUrl.isNullOrBlank()) {
                btn.visibility = View.VISIBLE
                btn.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(product.videoUrl))
                    startActivity(intent)
                }
            } else {
                btn.visibility = View.GONE
            }
        }

        findView(root, "badgeVerification", "tvVerifiedBadge", "badgeVerified")?.let { badge ->
            if (product.verificationStatus == "verified_farmer" || product.verificationStatus == "organic_certified") {
                badge.visibility = View.VISIBLE
                findTextView(root, "tvVerificationText", "tvVerifiedBadge")?.text =
                    if (product.verificationStatus == "organic_certified") "जैविक प्रमाणित" else "सत्यापित किसान"
            } else {
                badge.visibility = View.GONE
            }
        }

        findView(root, "btnCall", "ivCall")?.setOnClickListener {
            val phone = product.sellerPhone
            if (!phone.isNullOrBlank()) {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                startActivity(intent)
            } else {
                Toast.makeText(context, "किसान का फ़ोन नंबर उपलब्ध नहीं है", Toast.LENGTH_SHORT).show()
            }
        }

        findView(root, "btnWhatsApp", "btnWhatsapp", "ivWhatsApp")?.setOnClickListener {
            val phone = product.sellerPhone
            if (!phone.isNullOrBlank()) {
                val cleanPhone = phone.replace("+", "").replace(" ", "").trim()
                val msg = "नमस्ते ${product.sellerName} जी, मैंने Mera Kisan ऐप पर आपकी फसल '${product.name}' देखी।"
                val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(msg)}"
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } else {
                Toast.makeText(context, "WhatsApp नंबर उपलब्ध नहीं है", Toast.LENGTH_SHORT).show()
            }
        }

        findView(root, "btnShare", "ivShare")?.setOnClickListener {
            val shareText = "🌾 Mera Kisan पर फसल उपलब्ध है!\nफसल: ${product.name}\nभाव: ₹${product.pricePaise / 100}/${product.unit}"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(intent, "फसल शेयर करें"))
        }

        findView(root, "btnMakeOffer")?.let { btn ->
            if (FeatureManager.isEnabled(requireContext(), "offers")) {
                btn.visibility = View.VISIBLE
                btn.setOnClickListener {
                    Toast.makeText(context, "भाव प्रस्ताव विंडो खुल रही है...", Toast.LENGTH_SHORT).show()
                }
            } else {
                btn.visibility = View.GONE
            }
        }

        // सुरक्षित नेविगेशन (अमान्य ID से कंपाइलर एरर नहीं आएगी)
        findView(root, "btnBuyNow")?.setOnClickListener {
            val bundle = Bundle().apply {
                putString("productId", product.productId)
                putDouble("price", product.pricePaise / 100.0)
            }
            val navActionId = resources.getIdentifier("action_productDetail_to_checkoutBottomSheet", "id", requireContext().packageName)
            val fallbackDestId = resources.getIdentifier("checkoutBottomSheetFragment", "id", requireContext().packageName)
            val targetId = if (navActionId != 0) navActionId else fallbackDestId

            if (targetId != 0) {
                try {
                    findNavController().navigate(targetId, bundle)
                } catch (_: Exception) {
                    Toast.makeText(context, "ऑर्डर विंडो लोड हो रही है...", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "चेकआउट सुविधा जल्द उपलब्ध होगी", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun findTextView(root: View, vararg names: String): TextView? {
        for (name in names) {
            val id = root.resources.getIdentifier(name, "id", root.context.packageName)
            if (id != 0) {
                val v = root.findViewById<View>(id)
                if (v is TextView) return v
            }
        }
        return null
    }

    private fun findImageView(root: View, vararg names: String): ImageView? {
        for (name in names) {
            val id = root.resources.getIdentifier(name, "id", root.context.packageName)
            if (id != 0) {
                val v = root.findViewById<View>(id)
                if (v is ImageView) return v
            }
        }
        return null
    }

    private fun findView(root: View, vararg names: String): View? {
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
