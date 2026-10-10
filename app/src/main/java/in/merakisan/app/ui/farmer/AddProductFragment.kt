// app/src/main/java/in/merakisan/app/ui/farmer/AddProductFragment.kt
package in.merakisan.app.ui.farmer

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import in.merakisan.app.core.utils.ImageUtils
import in.merakisan.app.databinding.FragmentAddProductBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class AddProductFragment : Fragment() {

    private var _binding: FragmentAddProductBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddProductViewModel by viewModels()
    private var selectedImageUri: Uri? = null

    // Photo Picker
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            binding.ivCropPreview.setImageURI(uri)
            binding.ivCropPreview.visibility = View.VISIBLE
            binding.llPhotoPlaceholder.visibility = View.GONE

            // बैकग्राउंड में स्थानीय इमेज कम्प्रेशन का परीक्षण
            lifecycleScope.launch {
                val result = ImageUtils.compressImage(requireContext(), uri)
                if (result.isSuccess) {
                    val file = result.getOrNull()
                    if (file != null) {
                        Toast.makeText(requireContext(), "इमेज अनुकूलित: ${file.length() / 1024} KB", Toast.LENGTH_SHORT).show()
                    }
                }
            }
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

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        setupDropdowns()
        setupDatePicker()
        setupPhotoPicker()
        setupSubmitButton()
        observeViewModel()
    }

    private fun setupDropdowns() {
        val categories = arrayOf("अनाज व दालें", "ताज़ी सब्जियां", "फल व बागवानी", "कच्ची घानी तेल", "देशी मसाले व गुड़", "अन्य देशी उत्पाद")
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.actvCategory.setAdapter(categoryAdapter)
        binding.actvCategory.setText(categories[0], false)

        val units = arrayOf("kg", "क्विंटल", "लीटर", "ग्राम", "नग/पीस")
        val unitAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, units)
        binding.actvUnit.setAdapter(unitAdapter)
        binding.actvUnit.setText(units[0], false)

        // Factual Farming Verification Labels (No unsupported health/purity claims)
        val farmingMethods = arrayOf("farmer_declared", "organic_certified", "unverified")
        val methodLabels = arrayOf("किसान द्वारा स्व-घोषित (Factual)", "जैविक प्रमाणित (प्रमाणपत्र उपलब्ध)", "सामान्य उत्पादन")
        val methodAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, methodLabels)
        binding.actvFarmingMethod.setAdapter(methodAdapter)
        binding.actvFarmingMethod.setText(methodLabels[0], false)
    }

    private fun setupDatePicker() {
        binding.etHarvestDate.setOnClickListener {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            val dpd = DatePickerDialog(requireContext(), { _, y, m, d ->
                val dateStr = String.format(Locale.getDefault(), "%02d/%02d/%d", d, m + 1, y)
                binding.etHarvestDate.setText(dateStr)
            }, year, month, day)

            dpd.show()
        }
    }

    private fun setupPhotoPicker() {
        binding.cardPickPhoto.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    private fun setupSubmitButton() {
        binding.btnPublishCrop.setOnClickListener {
            val name = binding.etProductName.text.toString().trim()
            val category = binding.actvCategory.text.toString().trim()
            val variety = binding.etVariety.text.toString().trim()
            val priceStr = binding.etPrice.text.toString().trim()
            val stockStr = binding.etStock.text.toString().trim()
            val unit = binding.actvUnit.text.toString().trim()
            val minOrderStr = binding.etMinOrder.text.toString().trim()
            val harvestDate = binding.etHarvestDate.text.toString().trim()
            val farmingMethodLabel = binding.actvFarmingMethod.text.toString().trim()
            val village = binding.etVillage.text.toString().trim()
            val district = binding.etDistrict.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()

            val price = priceStr.toDoubleOrNull() ?: 0.0
            val stock = stockStr.toDoubleOrNull() ?: 0.0
            val minOrder = minOrderStr.toDoubleOrNull() ?: 1.0

            val verificationStatus = when {
                farmingMethodLabel.contains("जैविक") -> "organic_certified"
                farmingMethodLabel.contains("स्व-घोषित") -> "farmer_declared"
                else -> "unverified"
            }

            viewModel.publishProduct(
                name = name,
                category = category,
                variety = variety,
                priceRupees = price,
                stock = stock,
                unit = unit,
                minOrder = minOrder,
                harvestDate = harvestDate,
                farmingMethod = verificationStatus,
                village = village,
                district = district,
                description = description,
                photoUrl = selectedImageUri?.toString()
            )
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is AddProductUiState.Idle -> {
                        binding.btnPublishCrop.isEnabled = true
                        binding.pbSubmitting.visibility = View.GONE
                    }
                    is AddProductUiState.Loading -> {
                        binding.btnPublishCrop.isEnabled = false
                        binding.pbSubmitting.visibility = View.VISIBLE
                    }
                    is AddProductUiState.Success -> {
                        binding.btnPublishCrop.isEnabled = true
                        binding.pbSubmitting.visibility = View.GONE
                        Toast.makeText(requireContext(), "फसल सफलतापूर्वक प्रकाशित हो गई!", Toast.LENGTH_LONG).show()
                        viewModel.resetState()
                        findNavController().navigateUp()
                    }
                    is AddProductUiState.Error -> {
                        binding.btnPublishCrop.isEnabled = true
                        binding.pbSubmitting.visibility = View.GONE
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
