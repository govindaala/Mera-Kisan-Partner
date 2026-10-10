// app/src/main/java/in/merakisan/app/ui/reviews/AddReviewDialogFragment.kt
package in.merakisan.app.ui.reviews

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import in.merakisan.app.core.network.model.CreateReviewRequest
import in.merakisan.app.databinding.DialogAddReviewBinding

class AddReviewDialogFragment : BottomSheetDialogFragment() {

    private var _binding: DialogAddReviewBinding? = null
    private val binding get() = _binding!!

    private var orderId: String = ""
    private var productId: String = ""
    private var targetUid: String = ""
    private var onReviewSubmitted: ((CreateReviewRequest) -> Unit)? = null

    companion object {
        fun newInstance(
            orderId: String,
            productId: String,
            targetUid: String,
            onSubmit: (CreateReviewRequest) -> Unit
        ): AddReviewDialogFragment {
            return AddReviewDialogFragment().apply {
                this.orderId = orderId
                this.productId = productId
                this.targetUid = targetUid
                this.onReviewSubmitted = onSubmit
            }
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

        binding.btnCancelReview.setOnClickListener { dismiss() }

        binding.btnSubmitReview.setOnClickListener {
            val overall = binding.rbOverallRating.rating
            val quality = binding.rbQualityRating.rating
            val delivery = binding.rbDeliveryRating.rating
            val comment = binding.etReviewComment.text?.toString()?.trim()

            if (overall < 1.0f) {
                Toast.makeText(requireContext(), "कृपया कम से कम 1 स्टार रेटिंग दें", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = CreateReviewRequest(
                orderId = orderId,
                productId = productId,
                targetUid = targetUid,
                rating = overall,
                qualityRating = quality,
                deliveryRating = delivery,
                comment = if (!comment.isNullOrBlank()) comment else null
            )

            onReviewSubmitted?.invoke(request)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
