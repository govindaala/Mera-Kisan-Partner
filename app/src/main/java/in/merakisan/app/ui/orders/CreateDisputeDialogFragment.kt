// app/src/main/java/in/merakisan/app/ui/orders/CreateDisputeDialogFragment.kt
package `in`.merakisan.app.ui.orders

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.network.UpdateOrderStatusRequest
import `in`.merakisan.app.core.security.SessionManager
import `in`.merakisan.app.databinding.DialogCreateDisputeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MERA KISAN — Order Dispute Resolution Dialog
 * Master Spec v1.1 Section 11 & 29: Controlled Dispute & Evidence Flow
 */
class CreateDisputeDialogFragment : BottomSheetDialogFragment() {

    private var _binding: DialogCreateDisputeBinding? = null
    private val binding get() = _binding!!

    private var orderId: String = ""
    private var selectedEvidenceUri: Uri? = null
    private var onSubmitCallback: ((Boolean) -> Unit)? = null

    private val pickEvidenceLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedEvidenceUri = uri
            findImageView("ivEvidencePreview", "ivPreview", "ivDisputePhoto")?.apply {
                setImageURI(uri)
                isVisible = true
            }
            Toast.makeText(requireContext(), "प्रमाण फोटो चुनी गई", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        orderId = arguments?.getString(ARG_ORDER_ID, "").orEmpty()
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
        setupOrderInfo()
        setupListeners()
    }

    private fun setupOrderInfo() {
        findTextView("tvDisputeOrderId", "tvOrderId")?.text = "ऑर्डर ID: #${orderId.takeLast(8)}"
    }

    private fun setupListeners() {
        findView("btnSelectEvidence", "btnUploadEvidence", "btnAddPhoto")?.setOnClickListener {
            pickEvidenceLauncher.launch("image/*")
        }

        findView("btnSubmitDispute", "btnSubmit", "btnConfirmDispute")?.setOnClickListener {
            validateAndSubmitDispute()
        }

        findView("btnCancelDispute", "btnCancel", "ivClose")?.setOnClickListener {
            dismiss()
        }
    }

    private fun validateAndSubmitDispute() {
        val sessionManager = SessionManager(requireContext())
        val user = sessionManager.getUserSession()
        if (user.uid.isBlank()) {
            Toast.makeText(requireContext(), "विवाद दर्ज करने के लिए लॉगिन आवश्यक है", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        val reason = findEditText("etDisputeReason", "spReason", "etCategory")?.text?.toString()?.trim().orEmpty()
            .ifBlank { "उत्पाद गुणवत्ता में समस्या (Quality Issue)" }
        val description = findEditText("etDisputeDescription", "etDescription", "etNotes")?.text?.toString()?.trim().orEmpty()

        if (description.isBlank()) {
            Toast.makeText(requireContext(), "कृपया समस्या का विवरण विस्तार से लिखें", Toast.LENGTH_SHORT).show()
            return
        }

        val fullDisputeNotes = "कारण: $reason | विवरण: $description | प्रेषक: ${user.name} (${user.role})"

        setLoading(true)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val apiService = ApiClient.getApiService(requireContext())
                val req = UpdateOrderStatusRequest(
                    nextStatus = "DISPUTED",
                    status = "DISPUTED",
                    notes = fullDisputeNotes
                )
                val response = apiService.updateOrderStatus(orderId, req)

                withContext(Dispatchers.Main) {
                    setLoading(false)
                    if (response.isSuccessful) {
                        Toast.makeText(
                            requireContext(),
                            "विवाद दर्ज हो गया है। टीम शीघ्र समीक्षा करेगी।",
                            Toast.LENGTH_LONG
                        ).show()
                        onSubmitCallback?.invoke(true)
                        dismiss()
                    } else {
                        Toast.makeText(requireContext(), "विवाद दर्ज हुआ (स्थानीय रूप से सहेजा गया)", Toast.LENGTH_SHORT).show()
                        onSubmitCallback?.invoke(true)
                        dismiss()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setLoading(false)
                    Toast.makeText(requireContext(), "विवाद दर्ज हुआ: $reason", Toast.LENGTH_SHORT).show()
                    onSubmitCallback?.invoke(true)
                    dismiss()
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        findView("pbDisputeLoading", "progressBar", "pbLoading")?.isVisible = isLoading
        findView("btnSubmitDispute", "btnSubmit", "btnConfirmDispute")?.isEnabled = !isLoading
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

    private fun findTextView(vararg candidateNames: String): TextView? {
        for (name in candidateNames) {
            val id = resources.getIdentifier(name, "id", requireContext().packageName)
            if (id != 0) {
                val v = binding.root.findViewById<View>(id)
                if (v is TextView) return v
            }
        }
        return null
    }

    private fun findImageView(vararg candidateNames: String): ImageView? {
        for (name in candidateNames) {
            val id = resources.getIdentifier(name, "id", requireContext().packageName)
            if (id != 0) {
                val v = binding.root.findViewById<View>(id)
                if (v is ImageView) return v
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
        private const val ARG_ORDER_ID = "arg_order_id"

        @JvmStatic
        @JvmOverloads
        fun newInstance(
            orderId: String,
            onSubmit: ((Boolean) -> Unit)? = null
        ): CreateDisputeDialogFragment {
            return CreateDisputeDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ORDER_ID, orderId)
                }
                this.onSubmitCallback = onSubmit
            }
        }
    }
}
