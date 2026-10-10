// app/src/main/java/in/merakisan/app/ui/marketplace/ProductDetailFragment.kt
package `in`.merakisan.app.ui.marketplace

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
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
        }
    }

    private fun loadProductDetails(productId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiService = ApiClient.getApiService(requireContext())
                val response = apiService.getProductDetail(productId)
                if (response.isSuccessful && response.body()?.success == true) {
                    val product = response.body()?.data
                    withContext(Dispatchers.Main) {
                        product?.let { bindProductData(it) }
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "उत्पाद लोड करने में समस्या आई", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun bindProductData(product: ProductDto) {
        currentProduct = product

        // लेआउट आईडी को सुरक्षित रूप से मैप करना
        setTextSafe("tvProductName", "tvTitle", text = if (product.variety.isNotBlank()) "${product.name} (${product.variety})" else product.name)
        setTextSafe("tvProductCategory", "tvCategory", text = "श्रेणी: ${product.category}")

        val priceRupees = product.pricePaise / 100.0
        setTextSafe("tvProductPrice", "tvPrice", text = String.format(Locale.getDefault(), "₹%.0f", priceRupees))
        setTextSafe("tvProductUnit", "tvUnit", text = "/ ${product.unit}")
        setTextSafe("tvProductStock", "tvStock", text = "उपलब्ध मात्रा: ${product.stockQuantity}${product.unit}")

        val loc = if (product.village.isNotBlank()) "${product.village},${product.district}" else product.district
        setTextSafe("tvProductLocation", "tvLocation", text = "स्थान: $loc")
        setTextSafe("tvFarmerName", "tvSeller", text = "किसान: ${product.sellerName}")
        setTextSafe("tvDescription", "tvProductDescription", text = product.description)

        // कॉल बटन हैंडलर
        findViewSafe("btnCall")?.setOnClickListener {
            product.sellerPhone?.let { phone ->
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                startActivity(intent)
            }
        }

        // शेयर बटन हैंडलर
        findViewSafe("btnShare")?.setOnClickListener {
            val shareText = "🌾 Mera Kisan पर ${product.name} उपलब्ध है!\nमूल्य: ₹${product.pricePaise / 100}/${product.unit}\nकिसान: ${product.sellerName} (${product.district})"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(intent, "फसल शेयर करें"))
        }
    }

    private fun setTextSafe(vararg viewIds: String, text: String) {
        for (idName in viewIds) {
            val id = resources.getIdentifier(idName, "id", requireContext().packageName)
            if (id != 0) {
                binding.root.findViewById<TextView>(id)?.let {
                    it.text = text
                    return
                }
            }
        }
    }

    private fun findViewSafe(vararg viewIds: String): View? {
        for (idName in viewIds) {
            val id = resources.getIdentifier(idName, "id", requireContext().packageName)
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
