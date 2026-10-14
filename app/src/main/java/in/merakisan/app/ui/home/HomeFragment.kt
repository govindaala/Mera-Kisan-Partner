// app/src/main/java/in/merakisan/app/ui/home/HomeFragment.kt
package `in`.merakisan.app.ui.home

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
import `in`.merakisan.app.R
import `in`.merakisan.app.databinding.FragmentHomeBinding
import `in`.merakisan.app.ui.marketplace.ProductAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var freshTodayAdapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun safeNavigate(destinationId: Int, args: Bundle? = null) {
        try {
            val navController = findNavController()
            if (navController.currentDestination?.id != destinationId) {
                navController.navigate(destinationId, args)
            }
        } catch (e: Throwable) {
            Toast.makeText(requireContext(), "स्क्रीन खुल रही है...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        freshTodayAdapter = ProductAdapter { product ->
            val bundle = Bundle().apply {
                putString("productId", product.productId)
            }
            safeNavigate(R.id.productDetailFragment, bundle)
        }

        binding.rvFreshToday.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = freshTodayAdapter
            setHasFixedSize(false)
        }

        binding.swipeRefreshHome.setOnRefreshListener {
            viewModel.loadHomeData()
        }
    }

    private fun setupClickListeners() {
        // 1. सर्च ट्रिगर ➔ बाज़ार फ़ीड नेविगेशन (marketplaceFragment)
        binding.cardSearchTrigger.setOnClickListener {
            safeNavigate(R.id.marketplaceFragment)
        }

        // 2. छोटी मात्रा (1-25 kg) फ़िल्टर ट्रिगर ➔ बाज़ार
        binding.cardSmallQtyHero.setOnClickListener {
            val bundle = Bundle().apply {
                putBoolean("filterSmallQty", true)
            }
            safeNavigate(R.id.marketplaceFragment, bundle)
        }

        // 3. "मुझे चाहिए" मांग बोर्ड शॉर्टकट ➔ buyerRequestsFragment
        binding.cardDemandBoardCallout.setOnClickListener {
            safeNavigate(R.id.buyerRequestsFragment)
        }

        // 4. ताज़ा फसलें 'सभी देखें' ➔ बाज़ार
        binding.tvViewAllMarketplace.setOnClickListener {
            safeNavigate(R.id.marketplaceFragment)
        }

        // 5. किसान त्वरित फसल जोड़ें बटन ➔ addProductFragment
        binding.btnQuickAddProduce.setOnClickListener {
            safeNavigate(R.id.addProductFragment)
        }

        // 6. किसान ऑर्डर्स शॉर्टकट ➔ ordersFragment
        binding.cardFarmerOrders.setOnClickListener {
            safeNavigate(R.id.ordersFragment)
        }

        // 7. किसान फसलें शॉर्टकट ➔ marketplaceFragment
        binding.cardFarmerListings.setOnClickListener {
            safeNavigate(R.id.marketplaceFragment)
        }

        // 8. रोल चिप पर क्लिक करने पर प्रोफ़ाइल (रोल स्विचर) खोलना ➔ profileFragment
        binding.chipActiveRole.setOnClickListener {
            safeNavigate(R.id.profileFragment)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                val b = _binding ?: return@collectLatest
                b.swipeRefreshHome.isRefreshing = (state is HomeUiState.Loading)

                when (state) {
                    is HomeUiState.Loading -> {
                        b.pbHomeLoading.visibility = View.VISIBLE
                        b.tvEmptyHomeProducts.visibility = View.GONE
                    }
                    is HomeUiState.Success -> {
                        b.pbHomeLoading.visibility = View.GONE

                        b.tvHomeGreeting.text = "नमस्ते, ${state.userName}"
                        b.tvHomeLocation.text = state.locationText

                        when (state.activeRole) {
                            "FARMER" -> {
                                b.chipActiveRole.text = "🌾 किसान मोड"
                                b.llFarmerSection.visibility = View.VISIBLE
                                b.llBuyerSection.visibility = View.GONE
                            }
                            "BUYER" -> {
                                b.chipActiveRole.text = "🛒 खरीदार मोड"
                                b.llFarmerSection.visibility = View.GONE
                                b.llBuyerSection.visibility = View.VISIBLE
                            }
                            else -> {
                                b.chipActiveRole.text = "🤝 संयुक्त मोड (Both)"
                                b.llFarmerSection.visibility = View.VISIBLE
                                b.llBuyerSection.visibility = View.VISIBLE
                            }
                        }

                        b.tvActiveProduceCount.text = state.activeProduceCount.toString()
                        b.tvPendingOrdersCount.text = state.pendingOrdersCount.toString()

                        if (!state.announcementBanner.isNullOrBlank()) {
                            b.cardRemoteBanner.visibility = View.VISIBLE
                            b.tvRemoteBannerText.text = state.announcementBanner
                        } else {
                            b.cardRemoteBanner.visibility = View.GONE
                        }

                        if (state.freshProducts.isNotEmpty()) {
                            b.tvEmptyHomeProducts.visibility = View.GONE
                            b.rvFreshToday.visibility = View.VISIBLE
                            freshTodayAdapter.submitList(state.freshProducts)
                        } else {
                            b.rvFreshToday.visibility = View.GONE
                            b.tvEmptyHomeProducts.visibility = View.VISIBLE
                        }
                    }
                    is HomeUiState.Error -> {
                        b.pbHomeLoading.visibility = View.GONE
                        if (state.message.isNotBlank()) {
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }
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
