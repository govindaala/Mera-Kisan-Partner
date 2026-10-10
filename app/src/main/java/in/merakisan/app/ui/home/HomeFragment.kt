// app/src/main/java/in/merakisan/app/ui/home/HomeFragment.kt
package in.merakisan.app.ui.home

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
import in.merakisan.app.R
import in.merakisan.app.databinding.FragmentHomeBinding
import in.merakisan.app.ui.marketplace.ProductAdapter
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

    private fun setupRecyclerView() {
        freshTodayAdapter = ProductAdapter { product ->
            val bundle = Bundle().apply {
                putString("productId", product.productId)
            }
            findNavController().navigate(R.id.action_home_to_productDetail, bundle)
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
        // सर्च ट्रिगर ➔ बाज़ार फ़ीड नेविगेशन
        binding.cardSearchTrigger.setOnClickListener {
            findNavController().navigate(R.id.nav_marketplace)
        }

        // छोटी मात्रा (1-25 kg) फ़िल्टर ट्रिगर
        binding.cardSmallQtyHero.setOnClickListener {
            findNavController().navigate(R.id.nav_marketplace)
        }

        // "मुझे चाहिए" मांग बोर्ड शॉर्टकट
        binding.cardDemandBoardCallout.setOnClickListener {
            findNavController().navigate(R.id.nav_buyer_requests)
        }

        // ताज़ा फसलें 'सभी देखें'
        binding.tvViewAllMarketplace.setOnClickListener {
            findNavController().navigate(R.id.nav_marketplace)
        }

        // किसान त्वरित फसल जोड़ें बटन
        binding.btnQuickAddProduce.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_addProduct)
        }

        // किसान ऑर्डर्स शॉर्टकट
        binding.cardFarmerOrders.setOnClickListener {
            findNavController().navigate(R.id.nav_orders)
        }

        // किसान फसलें शॉर्टकट
        binding.cardFarmerListings.setOnClickListener {
            findNavController().navigate(R.id.nav_marketplace)
        }

        // रोल चिप पर क्लिक करने पर प्रोफ़ाइल (रोल स्विचर) खोलना
        binding.chipActiveRole.setOnClickListener {
            findNavController().navigate(R.id.nav_profile)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                binding.swipeRefreshHome.isRefreshing = (state is HomeUiState.Loading)

                when (state) {
                    is HomeUiState.Loading -> {
                        binding.pbHomeLoading.visibility = View.VISIBLE
                        binding.tvEmptyHomeProducts.visibility = View.GONE
                    }
                    is HomeUiState.Success -> {
                        binding.pbHomeLoading.visibility = View.GONE

                        // 1. उपयोगकर्ता विवरण व प्राइवेसी-सम्मत लोकेशन
                        binding.tvHomeGreeting.text = "नमस्ते, ${state.userName}"
                        binding.tvHomeLocation.text = state.locationText

                        // 2. सक्रिय भूमिका अनुकूलन (Role Adaptation)
                        when (state.activeRole) {
                            "FARMER" -> {
                                binding.chipActiveRole.text = "🌾 किसान मोड"
                                binding.llFarmerSection.visibility = View.VISIBLE
                                binding.llBuyerSection.visibility = View.GONE
                            }
                            "BUYER" -> {
                                binding.chipActiveRole.text = "🛒 खरीदार मोड"
                                binding.llFarmerSection.visibility = View.GONE
                                binding.llBuyerSection.visibility = View.VISIBLE
                            }
                            else -> { // BOTH
                                binding.chipActiveRole.text = "🤝 संयुक्त मोड (Both)"
                                binding.llFarmerSection.visibility = View.VISIBLE
                                binding.llBuyerSection.visibility = View.VISIBLE
                            }
                        }

                        // 3. किसान आँकड़े
                        binding.tvActiveProduceCount.text = state.activeProduceCount.toString()
                        binding.tvPendingOrdersCount.text = state.pendingOrdersCount.toString()

                        // 4. रिमोट घोषणा बैनर
                        if (!state.announcementBanner.isNullOrBlank()) {
                            binding.cardRemoteBanner.visibility = View.VISIBLE
                            binding.tvRemoteBannerText.text = state.announcementBanner
                        } else {
                            binding.cardRemoteBanner.visibility = View.GONE
                        }

                        // 5. ताज़ा फसलें प्रीव्यू
                        if (state.freshProducts.isNotEmpty()) {
                            binding.tvEmptyHomeProducts.visibility = View.GONE
                            binding.rvFreshToday.visibility = View.VISIBLE
                            freshTodayAdapter.submitList(state.freshProducts)
                        } else {
                            binding.rvFreshToday.visibility = View.GONE
                            binding.tvEmptyHomeProducts.visibility = View.VISIBLE
                        }
                    }
                    is HomeUiState.Error -> {
                        binding.pbHomeLoading.visibility = View.GONE
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
