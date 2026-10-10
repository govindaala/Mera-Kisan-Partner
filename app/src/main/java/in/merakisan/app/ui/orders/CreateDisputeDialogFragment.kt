// app/src/main/java/in/merakisan/app/ui/orders/CreateDisputeDialogFragment.kt
package in.merakisan.app.ui.orders

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import in.merakisan.app.databinding.DialogCreateDisputeBinding

class CreateDisputeDialogFragment : BottomSheetDialogFragment() {

    private var _binding: DialogCreateDisputeBinding? = null
    private val binding get() = _binding!!

    private var orderId: String = ""
    private var onDisputeSubmitted: ((String, String, Uri?) -> Unit)? = null
    private var selectedEvidenceUri: Uri? = null

    private val pickPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedEvidenceUri = uri
            binding.ivEvidencePreview.setImageURI(uri)
            binding.ivEvidencePreview.visibility = View.VISIBLE
            binding.llEvidencePlaceholder.visibility = View.GONE
        }
    }

    companion object {
        fun newInstance(
            orderId: String,
            onSubmit: (reason: String, description: String, evidenceUri: Uri?) -> Unit
        ): CreateDisputeDialogFragment {
            return CreateDisputeDialogFragment().apply {
                this.orderId = orderId
                this.onDisputeSubmitted = onSubmit
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCreateDisputeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDropdown()
        setupListeners()
    }

    private fun setupDropdown() {
        val reasons = arrayOf(
            "खराब गुणवत्ता या सड़ा हुआ माल",
            "मात्रा कम प्राप्त हुई (वज़न की समस्या)",
            "ऑर्डर किया गया उत्पाद नहीं मिला",
            "डिलीवरी में अत्यधिक देरी / माल नहीं पहुँचा",
            "अन्य कारण"
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, reasons)
        binding.actvDisputeReason.setAdapter(adapter)
        binding.actvDisputeReason.setText(reasons[0], false)
    }

    private fun setupListeners() {
        binding.cardPickEvidencePhoto.setOnClickListener {
            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnCancelDispute.setOnClickListener { dismiss() }

        binding.btnSubmitDispute.setOnClickListener {
            val reason = binding.actvDisputeReason.text.toString().trim()
            val description = binding.etDisputeDescription.text.toString().trim()

            if (description.length < 10) {
                Toast.makeText(requireContext(), "कृपया समस्या का कम से कम 10 अक्षरों में विवरण दें", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            onDisputeSubmitted?.invoke(reason, description, selectedEvidenceUri)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
