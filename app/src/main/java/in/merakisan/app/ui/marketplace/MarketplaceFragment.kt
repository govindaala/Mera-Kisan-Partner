// app/src/main/java/in/merakisan/app/ui/marketplace/MarketplaceFragment.kt
package in.merakisan.app.ui.marketplace

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import in.merakisan.app.R
import in.merakisan.app.core.network.model.ProductDto
import in.merakisan.app.databinding.FragmentMarketplaceBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.net.URLEncoder

class MarketplaceFragment : Fragment() {

    private var _binding: FragmentMarketplaceBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MarketplaceViewModel by viewModels()
    private lateinit var productAdapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMarketplaceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFilters()
        setupSearch()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(
            onProductClick = { product ->
                findNavController().navigate(
                    R.id.action_marketplace_to_productDetail,
                    bundleOf("productId" to product.productId)
                )
            },
            onCallClick = { product ->
                initiatePhoneCall(product.sellerPhone)
            },
            onWhatsAppClick = { product ->
                initiateWhatsAppContact(product)
            }
        )

        binding.rvProducts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productAdapter
            setHasFixedSize(true)
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadMarketplaceProducts()
        }
    }

    private fun setupFilters() {
        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            when {
                checkedIds.contains(R.id.chipAll) -> {
                    viewModel.setCategoryFilter(null)
                    viewModel.setSmallQuantityFilter(false)
                    viewModel.setOrganicFilter(false)
                }
                checkedIds.contains(R.id.chipSmallQuantity) -> {
                    viewModel.setSmallQuantityFilter(true)
                }
                checkedIds.contains(R.id.chipOrganic) -> {
                    viewModel.setOrganicFilter(true)
                }
                checkedIds.contains(R.id.chipGrains) -> {
                    viewModel.setCategoryFilter("अनाज व दालें")
                }
                checkedIds.contains(R.id.chipVegetables) -> {
                    viewModel.setCategoryFilter("सब्जियां")
                }
                checkedIds.contains(R.id.chipOil) -> {
                    viewModel.setCategoryFilter("कच्ची घानी तेल")
                }
            }
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.searchProducts(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                binding.swipeRefresh.isRefreshing = (state is MarketplaceUiState.Loading)

                when (state) {
                    is MarketplaceUiState.Loading -> {
                        binding.llEmptyState.visibility = View.GONE
                    }
                    is MarketplaceUiState.Success -> {
                        binding.llEmptyState.visibility = View.GONE
                        binding.rvProducts.visibility = View.VISIBLE
                        productAdapter.submitList(state.products)
                    }
                    is MarketplaceUiState.Empty -> {
                        binding.rvProducts.visibility = View.GONE
                        binding.llEmptyState.visibility = View.VISIBLE
                        binding.tvEmptyTitle.text = "कोई फसल नहीं मिली"
                    }
                    is MarketplaceUiState.Error -> {
                        binding.rvProducts.visibility = View.GONE
                        binding.llEmptyState.visibility = View.VISIBLE
                        binding.tvEmptyTitle.text = "लोड करने में समस्या"
                        binding.tvEmptySubtitle.text = state.message
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun initiatePhoneCall(phone: String?) {
        if (phone.isNullOrBlank()) {
            Toast.makeText(requireContext(), "किसान का नंबर उपलब्ध नहीं है", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phone")
        }
        startActivity(intent)
    }

    private fun initiateWhatsAppContact(product: ProductDto) {
        val phone = product.sellerPhone
        if (phone.isNullOrBlank()) {
            Toast.makeText(requireContext(), "व्हाट्सएप नंबर उपलब्ध नहीं है", Toast.LENGTH_SHORT).show()
            return
        }

        val message = "नमस्ते ${product.sellerName} जी, मैंने MERA KISAN ऐप पर आपकी फसल '${product.name}' (दर: ₹${product.pricePaise / 100}/kg) देखी है। मुझे यह खरीदनी है।"
        val url = "https://api.whatsapp.com/send?phone=+91$phone&text=${URLEncoder.encode(message, "UTF-8")}"

        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "WhatsApp इंस्टॉल नहीं है", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
