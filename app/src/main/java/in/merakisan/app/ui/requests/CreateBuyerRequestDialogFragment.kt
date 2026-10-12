// app/src/main/java/in/merakisan/app/ui/requests/CreateBuyerRequestDialogFragment.kt
package `in`.merakisan.app.ui.requests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.network.model.BuyerRequestDto
import `in`.merakisan.app.core.network.model.CreateBuyerRequestPayload
import `in`.merakisan.app.core.security.SessionManager
import `in`.merakisan.app.databinding.DialogCreateBuyerRequestBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * MERA KISAN — ग्राहक मांग बोर्ड डायलॉग ("मुझे यह चाहिए")
 * Master Spec v1.1 Section 7 & 15: Reverse Marketplace Engine
 */
class CreateBuyerRequestDialogFragment : BottomSheetDialogFragment() {

    private var _binding: DialogCreateBuyerRequestBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BuyerRequestsViewModel by activityViewModels()
    private var defaultCategory: String = "अनाज"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            defaultCategory = it.getString(ARG_CATEGORY, "अनाज")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCreateBuyerRequestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInitialData()
        setupListeners()
    }

    private fun setupInitialData() {
        findEditText("etCategory", "spCategory")?.setText(defaultCategory)
    }

    private fun setupListeners() {
        findView("btnSubmitRequest", "btnSubmit", "btnPostRequest")?.setOnClickListener {
            validateAndSubmitRequest()
        }

        findView("btnCancelRequest", "btnCancel", "ivClose")?.setOnClickListener {
            dismiss()
        }
    }

    private fun validateAndSubmitRequest() {
        val sessionManager = SessionManager(requireContext())
        val user = sessionManager.getUserSession()

        if (user.uid.isBlank()) {
            Toast.makeText(requireContext(), "मांग पोस्ट करने के लिए कृपया पहले लॉगिन करें", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        val productName = findEditText("etProductName", "etCropName", "etName")?.text?.toString()?.trim().orEmpty()
        val category = findEditText("etCategory", "spCategory")?.text?.toString()?.trim().orEmpty().ifBlank { defaultCategory }
        val quantityStr = findEditText("etQuantity", "etTargetQuantity", "etStock")?.text?.toString()?.trim().orEmpty()
        val unit = findEditText("etUnit", "spUnit")?.text?.toString()?.trim().orEmpty().ifBlank { "kg" }
        val targetPriceStr = findEditText("etTargetPrice", "etPrice", "etMaxPrice")?.text?.toString()?.trim().orEmpty()
        val notes = findEditText("etNotes", "etDescription", "etRequirementDetails")?.text?.toString()?.trim().orEmpty()

        if (productName.isBlank()) {
            Toast.makeText(requireContext(), "कृपया उत्पाद/फसल का नाम दर्ज करें", Toast.LENGTH_SHORT).show()
            return
        }

        val quantity = quantityStr.toDoubleOrNull()
        if (quantity == null || quantity <= 0.0) {
            Toast.makeText(requireContext(), "कृपया मान्य मात्रा (Quantity) दर्ज करें", Toast.LENGTH_SHORT).show()
            return
        }

        val targetPriceRupees = targetPriceStr.toDoubleOrNull() ?: 0.0
        // Master Spec v1.1 Rule 14: वित्तीय गणना केवल पैसे (Integer minor units) में
        val targetPricePaise = (targetPriceRupees * 100).toLong()

        val payload = CreateBuyerRequestPayload(
            productName = productName,
            category = category,
            quantity = quantity,
            unit = unit,
            targetPricePaise = targetPricePaise,
            notes = notes
        )

        val fullDto = BuyerRequestDto(
            requestId = "REQ_${UUID.randomUUID().toString().take(8).uppercase()}",
            buyerUid = user.uid,
            buyerName = user.name.ifBlank { "ग्राहक साथी" },
            buyerPhone = user.phone,
            productName = productName,
            category = category,
            quantity = quantity,
            unit = unit,
            targetPricePaise = targetPricePaise,
            district = user.role,
            village = "",
            notes = notes,
            status = "active"
        )

        setLoading(true)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val apiService = ApiClient.getApiService(requireContext())
                val response = apiService.createBuyerRequest(fullDto)

                withContext(Dispatchers.Main) {
                    setLoading(false)
                    if (response.isSuccessful && response.body()?.success == true) {
                        viewModel.loadBuyerRequests()
                        Toast.makeText(requireContext(), "आपकी मांग सफलतापूर्वक पोस्ट हो गई! किसान जल्द संपर्क करेंगे।", Toast.LENGTH_LONG).show()
                        dismiss()
                    } else {
                        // स्थानीय ViewModel फ़ॉलबैक
                        viewModel.postNewRequest(payload)
                        Toast.makeText(requireContext(), "मांग दर्ज हुई (स्थानीय रूप से सहेजी गई)", Toast.LENGTH_SHORT).show()
                        dismiss()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setLoading(false)
                    viewModel.postNewRequest(payload)
                    Toast.makeText(requireContext(), "मांग दर्ज हुई: $productName", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        findView("pbBuyerRequestLoading", "progressBar", "pbLoading")?.isVisible = isLoading
        findView("btnSubmitRequest", "btnSubmit", "btnPostRequest")?.isEnabled = !isLoading
    }

    private fun findEditText(vararg candidateNames: String): EditText? {
        for (name in candidateNames) {
            val id = resources.getIdentifier(name, "id", requireContext().packageName)
            if (id != 0) {
                val v = binding.root.findViewById<View>(id)
                if (v is EditText) return v
            }
        }
        return null
    }

    private fun findView(vararg candidateNames: String): View? {
        for (name in candidateNames) {
            val id = resources.getIdentifier(name, "id", requireContext().packageName)
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

    companion object {
        private const val ARG_CATEGORY = "arg_category"

        fun newInstance(category: String = "अनाज"): CreateBuyerRequestDialogFragment {
            return CreateBuyerRequestDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CATEGORY, category)
                }
            }
        }
    }
}
