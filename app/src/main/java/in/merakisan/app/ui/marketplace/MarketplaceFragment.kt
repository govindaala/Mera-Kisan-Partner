// app/src/main/java/in/merakisan/app/ui/marketplace/MarketplaceFragment.kt
package `in`.merakisan.app.ui.marketplace

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import `in`.merakisan.app.R
import `in`.merakisan.app.core.config.FeatureManager
import `in`.merakisan.app.core.network.model.ProductDto
import `in`.merakisan.app.databinding.FragmentMarketplaceBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * MERA KISAN — Marketplace Fragment
 * स्वच्छ MVVM, स्टेट-फ्लो कलेक्ट और एग्जॉस्टिव UI स्टेट मशीन
 */
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
        setupSearchAndFilters()
        setupSwipeRefresh()
        observeUiState()

        // प्रारंभिक डेटा लोड
        viewModel.loadMarketplaceProducts()
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(
            onProductClick = { product ->
                navigateToProductDetail(product.productId)
            }
        )

        binding.rvProducts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupSearchAndFilters() {
        // सर्च बार लिसनर
        binding.etSearch?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                viewModel.searchProducts(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // श्रेणी फ़िल्टर चिप्स
        binding.chipGroupCategories?.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) {
                viewModel.setCategoryFilter(null)
                return@setOnCheckedStateChangeListener
            }
            val checkedChip = group.findViewById<Chip>(checkedIds.first())
            val category = checkedChip?.text?.toString()
            if (category.equals("सभी", ignoreCase = true) || category.equals("All", ignoreCase = true)) {
                viewModel.setCategoryFilter(null)
            } else {
                viewModel.setCategoryFilter(category)
            }
        }

        // छोटी मात्रा (Small Quantity) फ़िल्टर
        binding.chipSmallQuantity?.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setSmallQuantityFilter(isChecked)
        }

        // जैविक / प्राकृतिक (Organic) फ़िल्टर
        binding.chipOrganic?.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setOrganicFilter(isChecked)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadMarketplaceProducts()
        }
    }

    private fun observeUiState() {
        // 1. Kotlin Coroutine StateFlow Observer (100% Exhaustive when block)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    when (state) {
                        is MarketplaceUiState.Loading -> {
                            renderLoading(true)
                        }
                        is MarketplaceUiState.Success -> {
                            renderLoading(false)
                            val list = if (state.products.isNotEmpty()) state.products else state.data
                            renderProductList(list)
                        }
                        is MarketplaceUiState.Empty -> {
                            renderLoading(false)
                            renderEmpty()
                        }
                        is MarketplaceUiState.Error -> {
                            renderLoading(false)
                            val msg = state.message.ifBlank { state.error }
                            renderError(msg)
                        }
                        else -> {
                            // किसी भी अतिरिक्त स्थिति (Idle, Offline आदि) के लिए सुरक्षात्मक फॉलबैक
                            renderLoading(false)
                        }
                    }
                }
            }
        }

        // 2. बैकवर्ड कम्पैटिबिलिटी LiveData ऑब्ज़र्वर
        viewModel.products.observe(viewLifecycleOwner) { products ->
            if (!products.isNullOrEmpty()) {
                productAdapter.submitList(products)
                binding.tvEmptyState?.isVisible = false
                binding.rvProducts.isVisible = true
            }
        }
    }

    private fun renderLoading(isLoading: Boolean) {
        binding.swipeRefresh.isRefreshing = isLoading
        binding.progressBar?.isVisible = isLoading && productAdapter.itemCount == 0
    }

    private fun renderProductList(products: List<ProductDto>) {
        if (products.isEmpty()) {
            renderEmpty()
        } else {
            binding.tvEmptyState?.isVisible = false
            binding.rvProducts.isVisible = true
            productAdapter.submitList(products)
        }
    }

    private fun renderEmpty() {
        productAdapter.submitList(emptyList())
        binding.rvProducts.isVisible = false
        binding.tvEmptyState?.apply {
            text = "वर्तमान में कोई कृषि उपज उपलब्ध नहीं है।"
            isVisible = true
        }
    }

    private fun renderError(errorMessage: String) {
        binding.swipeRefresh.isRefreshing = false
        if (productAdapter.itemCount == 0) {
            binding.rvProducts.isVisible = false
            binding.tvEmptyState?.apply {
                text = if (errorMessage.isNotBlank()) errorMessage else "डेटा लोड करने में समस्या आई। पुनः प्रयास करें।"
                isVisible = true
            }
        } else {
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToProductDetail(productId: String) {
        val bundle = Bundle().apply {
            putString("productId", productId)
        }
        try {
            findNavController().navigate(R.id.productDetailFragment, bundle)
        } catch (_: Exception) {
            // यदि NavGraph आईडी भिन्न हो तो सुरक्षित फॉलबैक
            Toast.makeText(requireContext(), "उत्पाद विवरण खोला जा रहा है...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
