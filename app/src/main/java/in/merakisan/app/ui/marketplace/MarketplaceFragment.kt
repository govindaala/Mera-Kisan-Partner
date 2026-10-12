// app/src/main/java/in/merakisan/app/ui/marketplace/MarketplaceFragment.kt
package `in`.merakisan.app.ui.marketplace

import android.os.Bundle
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
import `in`.merakisan.app.R
import `in`.merakisan.app.databinding.FragmentMarketplaceBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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

        productAdapter = ProductAdapter(
            onProductClick = { product ->
                val bundle = Bundle().apply { putString("productId", product.productId) }
                try {
                    findNavController().navigate(R.id.productDetailFragment, bundle)
                } catch (_: Exception) {}
            }
        )

        binding.rvProducts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productAdapter
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadMarketplaceProducts()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    when (state) {
                        is MarketplaceUiState.Loading -> {
                            binding.swipeRefresh.isRefreshing = true
                        }
                        is MarketplaceUiState.Success -> {
                            binding.swipeRefresh.isRefreshing = false
                            val list = if (state.products.isNotEmpty()) state.products else state.data
                            productAdapter.submitList(list)
                            binding.rvProducts.isVisible = list.isNotEmpty()
                        }
                        is MarketplaceUiState.Empty -> {
                            binding.swipeRefresh.isRefreshing = false
                            productAdapter.submitList(emptyList())
                            binding.rvProducts.isVisible = false
                        }
                        is MarketplaceUiState.Error -> {
                            binding.swipeRefresh.isRefreshing = false
                            val msg = state.message.ifBlank { state.error }
                            if (msg.isNotBlank()) {
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                        else -> {
                            binding.swipeRefresh.isRefreshing = false
                        }
                    }
                }
            }
        }

        viewModel.loadMarketplaceProducts()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
