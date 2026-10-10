// app/src/main/java/in/merakisan/app/ui/requests/BuyerRequestsFragment.kt
package in.merakisan.app.ui.requests

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import in.merakisan.app.core.network.model.BuyerRequestDto
import in.merakisan.app.core.network.model.CreateBuyerRequestPayload
import in.merakisan.app.databinding.DialogCreateBuyerRequestBinding
import in.merakisan.app.databinding.FragmentBuyerRequestsBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.net.URLEncoder

class BuyerRequestsFragment : Fragment() {

    private var _binding: FragmentBuyerRequestsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BuyerRequestsViewModel by viewModels()
    private lateinit var requestAdapter: BuyerRequestAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBuyerRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFab()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        requestAdapter = BuyerRequestAdapter(
            onSendOfferClick = { request ->
                showFarmerOfferDialog(request)
            },
            onCallClick = { request ->
                initiatePhoneCall(request.buyerPhone)
            },
            onWhatsAppClick = { request ->
                initiateWhatsAppContact(request)
            }
        )

        binding.rvBuyerRequests.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = requestAdapter
            setHasFixedSize(true)
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadBuyerRequests()
        }
    }

    private fun setupFab() {
        binding.fabPostRequest.setOnClickListener {
            showCreateRequestDialog()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                binding.swipeRefresh.isRefreshing = (state is BuyerRequestsUiState.Loading)

                when (state) {
                    is BuyerRequestsUiState.Loading -> {
                        binding.llEmptyState.visibility = View.GONE
                    }
                    is BuyerRequestsUiState.Success -> {
                        binding.llEmptyState.visibility = View.GONE
                        binding.rvBuyerRequests.visibility = View.VISIBLE
                        requestAdapter.submitList(state.requests)
                    }
                    is BuyerRequestsUiState.Empty -> {
                        binding.rvBuyerRequests.visibility = View.GONE
                        binding.llEmptyState.visibility = View.VISIBLE
                    }
                    is BuyerRequestsUiState.Error -> {
                        binding.rvBuyerRequests.visibility = View.GONE
                        binding.llEmptyState.visibility = View.VISIBLE
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showCreateRequestDialog() {
        val dialogBinding = DialogCreateBuyerRequestBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancelReq.setOnClickListener { dialog.dismiss() }

        dialogBinding.btnSubmitReq.setOnClickListener {
            val name = dialogBinding.etReqProductName.text.toString().trim()
            val variety = dialogBinding.etReqVariety.text.toString().trim()
            val qtyStr = dialogBinding.etReqQuantity.text.toString().trim()
            val unit = dialogBinding.etReqUnit.text.toString().trim()
            val priceStr = dialogBinding.etReqTargetPrice.text.toString().trim()
            val district = dialogBinding.etReqDistrict.text.toString().trim()
            val notes = dialogBinding.etReqNotes.text.toString().trim()

            if (name.isBlank()) {
                Toast.makeText(requireContext(), "फसल का नाम दर्ज करें", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val qty = qtyStr.toDoubleOrNull()
            if (qty == null || qty <= 0.0) {
                Toast.makeText(requireContext(), "मान्य मात्रा दर्ज करें", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val pricePaise = priceStr.toDoubleOrNull()?.let { (it * 100).toLong() }

            val payload = CreateBuyerRequestPayload(
                productName = name,
                category = "सामान्य",
                variety = if (variety.isNotBlank()) variety else null,
                quantity = qty,
                unit = if (unit.isNotBlank()) unit else "kg",
                targetPricePaise = pricePaise,
                district = if (district.isNotBlank()) district else "मंदसौर",
                notes = if (notes.isNotBlank()) notes else null
            )

            viewModel.postNewRequest(payload)
            dialog.dismiss()
            Toast.makeText(requireContext(), "आपकी मांग सफलतापूर्वक पोस्ट हो गई!", Toast.LENGTH_LONG).show()
        }

        dialog.show()
    }

    private fun showFarmerOfferDialog(request: BuyerRequestDto) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("किसान प्रस्ताव प्रेषित करें")
        builder.setMessage("क्या आप '${request.productName}' के लिए खरीदार ${request.buyerName} को अपनी उपलब्ध दर का प्रस्ताव भेजना चाहते हैं?")
        builder.setPositiveButton("हाँ, प्रस्ताव भेजें") { d, _ ->
            d.dismiss()
            Toast.makeText(requireContext(), "प्रस्ताव खरीदार को भेज दिया गया है।", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("रद्द करें") { d, _ -> d.dismiss() }
        builder.show()
    }

    private fun initiatePhoneCall(phone: String?) {
        if (phone.isNullOrBlank()) {
            Toast.makeText(requireContext(), "खरीदार का नंबर उपलब्ध नहीं है", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phone")
        }
        startActivity(intent)
    }

    private fun initiateWhatsAppContact(request: BuyerRequestDto) {
        val phone = request.buyerPhone
        if (phone.isNullOrBlank()) {
            Toast.makeText(requireContext(), "व्हाट्सएप नंबर उपलब्ध नहीं है", Toast.LENGTH_SHORT).show()
            return
        }

        val message = "नमस्ते ${request.buyerName} जी, MERA KISAN ऐप पर आपकी मांग '${request.productName} (${request.quantity} ${request.unit})' देखी। मेरे पास यह फसल उपलब्ध है।"
        val url = "https://api.whatsapp.com/send?phone=+91$phone&text=${URLEncoder.encode(message, "UTF-8")}"

        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "WhatsApp इंस्टॉल नहीं है", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
