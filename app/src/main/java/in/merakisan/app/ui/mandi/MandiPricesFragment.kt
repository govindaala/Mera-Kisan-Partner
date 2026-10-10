// app/src/main/java/in/merakisan/app/ui/mandi/MandiPricesFragment.kt
package in.merakisan.app.ui.mandi

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import in.merakisan.app.databinding.FragmentMandiPricesBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MandiPricesFragment : Fragment() {

    private var _binding: FragmentMandiPricesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MandiPricesViewModel by viewModels()
    private lateinit var adapter: MandiPriceAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMandiPricesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupSearchFilter()
        setupSwipeRefresh()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        adapter = MandiPriceAdapter { price ->
            Toast.makeText(
                requireContext(),
                "${price.market} में ${price.commodity} का मॉडल भाव ₹${price.modalPricePaise / 100}/क्विंटल है।",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.rvMandiPrices.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MandiPricesFragment.adapter
            setHasFixedSize(true)
        }
    }

    private fun setupSearchFilter() {
        binding.etSearchCommodity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.filterPrices(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshMandi.setOnRefreshListener {
            viewModel.loadMandiPrices(forceRefresh = true)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                binding.swipeRefreshMandi.isRefreshing = (state is MandiUiState.Loading)

                when (state) {
                    is MandiUiState.FeatureDisabled -> {
                        binding.llMandiContent.visibility = View.GONE
                        binding.pbMandiLoading.visibility = View.GONE
                        binding.llEmptyMandi.visibility = View.VISIBLE
                        binding.tvEmptyMandiTitle.text = "मंडी भाव सेवा वर्तमान में बंद है"
                        binding.tvEmptyMandiSubtitle.text = "प्रशासक (Admin) द्वारा यह सुविधा अस्थायी रूप से निष्क्रिय की गई है।"
                    }
                    is MandiUiState.Loading -> {
                        binding.pbMandiLoading.visibility = View.VISIBLE
                        binding.llEmptyMandi.visibility = View.GONE
                    }
                    is MandiUiState.Success -> {
                        binding.pbMandiLoading.visibility = View.GONE
                        binding.llEmptyMandi.visibility = View.GONE
                        binding.llMandiContent.visibility = View.VISIBLE
                        binding.tvMandiLastUpdated.text = state.lastUpdatedText
                        adapter.submitList(state.prices)
                    }
                    is MandiUiState.Empty -> {
                        binding.pbMandiLoading.visibility = View.GONE
                        binding.llEmptyMandi.visibility = View.VISIBLE
                        binding.tvEmptyMandiTitle.text = "कोई मंडी भाव नहीं मिला"
                        binding.tvEmptyMandiSubtitle.text = state.message
                    }
                    is MandiUiState.Error -> {
                        binding.pbMandiLoading.visibility = View.GONE
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
