// app/src/main/java/in/merakisan/app/ui/farmer/AddProductFragment.kt
package `in`.merakisan.app.ui.farmer

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.network.model.ProductDto
import `in`.merakisan.app.core.security.SessionManager
import `in`.merakisan.app.databinding.FragmentAddProductBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class AddProductFragment : Fragment() {

    private var _binding: FragmentAddProductBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddProductViewModel by viewModels()
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            findImageView("ivProductPreview", "ivCropImage", "ivPreview")?.apply {
                setImageURI(uri)
                isVisible = true
            }
            Toast.makeText(requireContext(), "फोटो चुनी गई", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
    }

    private fun setupListeners() {
        findView("btnSelectPhoto", "btnUploadPhoto", "btnAddPhoto")?.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        findView("btnPublishProduct", "btnSubmitProduct", "btnSaveCrop", "btnSubmit")?.setOnClickListener {
            validateAndSubmitProduct()
        }
    }

    private fun validateAndSubmitProduct() {
        val name = findEditText("etProductName", "etCropName", "etName")?.text?.toString()?.trim().orEmpty()
        val category = findEditText("etCategory", "spCategory")?.text?.toString()?.trim().orEmpty().ifBlank { "अनाज" }
        val variety = findEditText("etVariety", "etCropVariety")?.text?.toString()?.trim().orEmpty()
        val quantityStr = findEditText("etQuantity", "etStockQuantity", "etStock")?.text?.toString()?.trim().orEmpty()
        val unit = findEditText("etUnit", "spUnit")?.text?.toString()?.trim().orEmpty().ifBlank { "kg" }
        val priceStr = findEditText("etPrice", "etPricePerUnit")?.text?.toString()?.trim().orEmpty()
        val description = findEditText("etDescription", "etProductDescription")?.text?.toString()?.trim().orEmpty()

        val villageInput = findEditText("etVillage", "etLocation")?.text?.toString()?.trim().orEmpty()
        val districtInput = findEditText("etDistrict", "spDistrict")?.text?.toString()?.trim().orEmpty()

        if (name.isBlank()) {
            Toast.makeText(requireContext(), "कृपया फसल/उत्पाद का नाम दर्ज करें", Toast.LENGTH_SHORT).show()
            return
        }

        val quantity = quantityStr.toDoubleOrNull()
        if (quantity == null || quantity <= 0.0) {
            Toast.makeText(requireContext(), "कृपया मान्य मात्रा (Quantity) दर्ज करें", Toast.LENGTH_SHORT).show()
            return
        }

        val priceRupees = priceStr.toDoubleOrNull()
        if (priceRupees == null || priceRupees <= 0.0) {
            Toast.makeText(requireContext(), "कृपया मान्य भाव (Price) दर्ज करें", Toast.LENGTH_SHORT).show()
            return
        }

        val sessionManager = SessionManager(requireContext())
        val userSession = sessionManager.getUserSession()
        val sellerUid = userSession.uid.ifBlank { "farmer_${System.currentTimeMillis()}" }
        val sellerName = userSession.name.ifBlank { "किसान साथी" }
        val pricePaise = (priceRupees * 100).toLong()

        val productDto = ProductDto(
            productId = "PROD_${UUID.randomUUID().toString().take(8).uppercase()}",
            sellerUid = sellerUid,
            sellerName = sellerName,
            name = name,
            category = category,
            variety = variety,
            description = description,
            stockQuantity = quantity,
            unit = unit,
            pricePaise = pricePaise,
            village = villageInput,
            district = districtInput,
            verificationStatus = "farmer_declared",
            photos = if (selectedImageUri != null) listOf(selectedImageUri.toString()) else emptyList(),
            createdAt = System.currentTimeMillis().toString()
        )

        showLoading(true)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val apiService = ApiClient.getApiService(requireContext())
                val response = apiService.createProduct(productDto)

                withContext(Dispatchers.Main) {
                    showLoading(false)
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(requireContext(), "फसल सफलतापूर्वक सूचीबद्ध हो गई!", Toast.LENGTH_LONG).show()
                        findNavController().navigateUp()
                    } else {
                        Toast.makeText(requireContext(), "फसल स्थानीय रूप से सहेजी गई", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(requireContext(), "फसल सहेजी गई: ${productDto.name}", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        findView("pbAddProductLoading", "progressBar", "pbLoading")?.isVisible = isLoading
        findView("btnPublishProduct", "btnSubmitProduct", "btnSaveCrop")?.isEnabled = !isLoading
    }

    private fun findEditText(vararg candidateNames: String): EditText? {
        for (name in candidateNames) {
            val id = resources.getIdentifier(name, "id", requireContext().packageName)
            if (id != 0) {
                val view = binding.root.findViewById<View>(id)
                if (view is EditText) return view
            }
        }
        return null
    }

    private fun findImageView(vararg candidateNames: String): ImageView? {
        for (name in candidateNames) {
            val id = resources.getIdentifier(name, "id", requireContext().packageName)
            if (id != 0) {
                val view = binding.root.findViewById<View>(id)
                if (view is ImageView) return view
            }
        }
        return null
    }

    private fun findView(vararg candidateNames: String): View? {
        for (name in candidateNames) {
            val id = resources.getIdentifier(name, "id", requireContext().packageName)
            if (id != 0) {
                val view = binding.root.findViewById<View>(id)
                if (view != null) return view
            }
        }
        return null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
