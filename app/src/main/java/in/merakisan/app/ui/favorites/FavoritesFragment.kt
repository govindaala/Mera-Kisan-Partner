// app/src/main/java/in/merakisan/app/ui/favorites/FavoritesFragment.kt
package in.merakisan.app.ui.favorites

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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import in.merakisan.app.R
import in.merakisan.app.databinding.FragmentFavoritesBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.net.URLEncoder

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FavoritesViewModel by viewModels()

    private lateinit var productAdapter: FavoriteProductAdapter
    private lateinit var farmerAdapter: FollowedFarmerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTabs()
        setupAdapters()
        setupSwipeRefresh()
        observeViewModel()
    }

    private fun setupTabs() {
        binding.tabLayoutFavorites.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                viewModel.selectTab(tab?.position ?: 0)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupAdapters() {
        productAdapter = FavoriteProductAdapter(
            onProductClick = { fav ->
                val bundle = Bundle().apply { putString("productId", fav.productId) }
                findNavController().navigate(R.id.productDetailFragment, bundle)
            },
            onRemoveClick = { fav ->
                viewModel.removeProduct(fav)
                Toast.makeText(requireContext(), "'${fav.name}' पसंदीदा से हटाई गई", Toast.LENGTH_SHORT).show()
            }
        )

        farmerAdapter = FollowedFarmerAdapter(
            onFarmerClick = { farmer ->
                Toast.makeText(requireContext(), "किसान: ${farmer.name}", Toast.LENGTH_SHORT).show()
            },
            onCallClick = { farmer ->
                if (!farmer.phone.isNullOrBlank()) {
                    startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${farmer.phone}")))
                }
            },
            onWhatsAppClick = { farmer ->
                if (!farmer.phone.isNullOrBlank()) {
                    val msg = "नमस्ते ${farmer.name} जी, मैंने MERA KISAN ऐप पर आपकी प्रोफाइल देखी है।"
                    val url = "https://api.whatsapp.com/send?phone=+91${farmer.phone}&text=${URLEncoder.encode(msg, "UTF-8")}"
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "WhatsApp इंस्टॉल नहीं है", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onUnfollowClick = { farmer ->
                viewModel.unfollowFarmer(farmer)
                Toast.makeText(requireContext(), "${farmer.name} को अनफ़ॉलो किया गया", Toast.LENGTH_SHORT).show()
            }
        )

        binding.rvFavoriteProducts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productAdapter
            setHasFixedSize(true)
        }

        binding.rvFollowedFarmers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = farmerAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshFavorites.setOnRefreshListener {
            viewModel.loadInitialData()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                binding.swipeRefreshFavorites.isRefreshing = (state is FavoritesUiState.Loading)

                when (state) {
                    is FavoritesUiState.Loading -> {
                        binding.llEmptyFavorites.visibility = View.GONE
                    }
                    is FavoritesUiState.ProductsLoaded -> {
                        binding.llEmptyFavorites.visibility = View.GONE
                        binding.rvFollowedFarmers.visibility = View.GONE
                        binding.rvFavoriteProducts.visibility = View.VISIBLE
                        productAdapter.submitList(state.products)
                    }
                    is FavoritesUiState.FarmersLoaded -> {
                        binding.llEmptyFavorites.visibility = View.GONE
                        binding.rvFavoriteProducts.visibility = View.GONE
                        binding.rvFollowedFarmers.visibility = View.VISIBLE
                        farmerAdapter.submitList(state.farmers)
                    }
                    is FavoritesUiState.Empty -> {
                        binding.rvFavoriteProducts.visibility = View.GONE
                        binding.rvFollowedFarmers.visibility = View.GONE
                        binding.llEmptyFavorites.visibility = View.VISIBLE

                        val isProductsTab = binding.tabLayoutFavorites.selectedTabPosition == 0
                        binding.tvEmptyFavTitle.text = if (isProductsTab) "कोई सहेजी गई फसल नहीं है" else "कोई फॉलो किया गया किसान नहीं है"
                        binding.tvEmptyFavSubtitle.text = if (isProductsTab) {
                            "मार्केटप्लेस से फसलें सहेजें ताकि आप उन्हें बाद में तुरंत खरीद सकें।"
                        } else {
                            "विश्वसनीय किसानों को फ़ॉलो करें ताकि उनकी नई फसल आते ही आपको तुरंत पता चले।"
                        }
                    }
                    is FavoritesUiState.Error -> {
                        binding.rvFavoriteProducts.visibility = View.GONE
                        binding.rvFollowedFarmers.visibility = View.GONE
                        binding.llEmptyFavorites.visibility = View.VISIBLE
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
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
