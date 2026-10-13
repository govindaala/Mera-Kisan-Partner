// app/src/main/java/in/merakisan/app/ui/reviews/AddReviewDialogFragment.kt
package `in`.merakisan.app.ui.reviews

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.network.model.ReviewDto
import `in`.merakisan.app.core.security.SessionManager
import `in`.merakisan.app.databinding.DialogAddReviewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * MERA KISAN — ऑर्डर समीक्षा एवं 5-स्टार रेटिंग डायलॉग
 * Master Spec v1.1 Section 9 & 17: Post-Delivery Verified Buyer Reviews
 */
class AddReviewDialogFragment : BottomSheetDialogFragment() {

    private var _binding: DialogAddReviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReviewsViewModel by viewModels()

    private var orderId: String = ""
    private var productId: String = ""
    private var sellerUid: String = ""
    private var productName: String = ""
    private var onReviewSubmittedCallback: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            orderId = it.getString(ARG_ORDER_ID, "").orEmpty()
            productId = it.getString(ARG_PRODUCT_ID, "").orEmpty()
            sellerUid = it.getString(ARG_SELLER_UID, "").orEmpty()
            productName = it.getString(ARG_PRODUCT_NAME, "कृषि उपज").orEmpty()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupHeaderDetails()
        setupListeners()
    }

    private fun setupHeaderDetails() {
        findTextView("tvProductName", "tvReviewProductName", "tvTitle")?.text = productName
        findTextView("tvOrderId", "tvReviewOrderId")?.text = if (orderId.isNotBlank()) {
            "ऑर्डर ID: #${orderId.takeLast(8)}"
        } else {
            "सत्यापित डिलीवरी समीक्षा"
        }
    }

    private fun setupListeners() {
        findView("btnSubmitReview", "btnSubmit", "btnConfirmReview")?.setOnClickListener {
            validateAndSubmitReview()
        }

        findView("btnCancelReview", "btnCancel", "ivClose")?.setOnClickListener {
            dismiss()
        }
    }

    private fun validateAndSubmitReview() {
        val sessionManager = SessionManager(requireContext())
        val user = sessionManager.getUserSession()

        if (user.uid.isBlank()) {
            Toast.makeText(requireContext(), "समीक्षा दर्ज करने के लिए कृपया पहले लॉगिन करें", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        val ratingBar = findRatingBar("rbRating", "ratingBar", "rbQualityRating")
        val ratingValue = ratingBar?.rating?.toDouble() ?: 5.0

        if (ratingValue <= 0.0) {
            Toast.makeText(requireContext(), "कृपया कम से कम 1 स्टार रेटिंग अवश्य चुनें", Toast.LENGTH_SHORT).show()
            return
        }

        val commentText = findEditText("etReviewComment", "etComment", "etFeedback", "etReview")?.text?.toString()?.trim().orEmpty()

        if (commentText.isBlank()) {
            Toast.makeText(requireContext(), "कृपया उपज की गुणवत्ता पर अपना संक्षिप्त अनुभव लिखें", Toast.LENGTH_SHORT).show()
            return
        }

        val newReviewDto = ReviewDto(
            reviewId = "REV_${UUID.randomUUID().toString().take(8).uppercase()}",
            orderId = orderId,
            productId = productId,
            buyerUid = user.uid,
            buyerName = user.name.ifBlank { "सत्यापित खरीदार" },
            sellerUid = sellerUid,
            rating = ratingValue,
            comment = commentText,
            createdAt = System.currentTimeMillis().toString()
        )

        setLoading(true)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // ViewModel के माध्यम से स्थानीय व नेटवर्क सबमिशन
                viewModel.submitReview(newReviewDto)

                withContext(Dispatchers.Main) {
                    setLoading(false)
                    Toast.makeText(requireContext(), "आपकी समीक्षा सफलतापूर्वक दर्ज हो गई! धन्यवाद।", Toast.LENGTH_LONG).show()
                    onReviewSubmittedCallback?.invoke()
                    dismiss()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setLoading(false)
                    Toast.makeText(requireContext(), "समीक्षा स्थानीय रूप से सहेजी गई", Toast.LENGTH_SHORT).show()
                    onReviewSubmittedCallback?.invoke()
                    dismiss()
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        findView("pbReviewLoading", "progressBar", "pbLoading")?.isVisible = isLoading
        findView("btnSubmitReview", "btnSubmit", "btnConfirmReview")?.isEnabled = !isLoading
    }

    private fun findRatingBar(vararg candidateNames: String): RatingBar? {
        for (name in candidateNames) {
            val id = resources.getIdentifier(name, "id", requireContext().packageName)
            if (id != 0) {
                val v = binding.root.findViewById<View>(id)
                if (v is RatingBar) return v
            }
        }
        return null
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
        private const val ARG_PRODUCT_ID = "arg_product_id"
        private const val ARG_SELLER_UID = "arg_seller_uid"
        private const val ARG_PRODUCT_NAME = "arg_product_name"

        @JvmStatic
        @JvmOverloads
        fun newInstance(
            orderId: String,
            productId: String = "",
            sellerUid: String = "",
            productName: String = "",
            onSubmitted: (() -> Unit)? = null
        ): AddReviewDialogFragment {
            return AddReviewDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ORDER_ID, orderId)
                    putString(ARG_PRODUCT_ID, productId)
                    putString(ARG_SELLER_UID, sellerUid)
                    putString(ARG_PRODUCT_NAME, productName)
                }
                this.onReviewSubmittedCallback = onSubmitted
            }
        }
    }
}
