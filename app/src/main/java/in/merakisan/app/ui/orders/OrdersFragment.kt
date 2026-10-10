// app/src/main/java/in/merakisan/app/ui/orders/OrdersFragment.kt
package in.merakisan.app.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import in.merakisan.app.core.security.SessionManager
import in.merakisan.app.databinding.FragmentOrdersBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class OrdersFragment : Fragment() {

    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrdersViewModel by viewModels()
    private lateinit var orderAdapter: OrderAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTabs()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupTabs() {
        val userRole = SessionManager.getUserRole(requireContext())
        if (userRole == "FARMER") {
            binding.tabLayoutOrders.getTabAt(1)?.select()
            viewModel.setRoleFilter("farmer")
        } else {
            binding.tabLayoutOrders.getTabAt(0)?.select()
            viewModel.setRoleFilter("buyer")
        }

        binding.tabLayoutOrders.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> viewModel.setRoleFilter("buyer")
                    1 -> viewModel.setRoleFilter("farmer")
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerView() {
        orderAdapter = OrderAdapter(
            onOrderClick = { order ->
                Toast.makeText(
                    requireContext(),
                    "ऑर्डर #${order.orderId.takeLast(6)}: ${order.orderStatus}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        binding.rvOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = orderAdapter
            setHasFixedSize(true)
        }

        binding.swipeRefreshOrders.setOnRefreshListener {
            viewModel.loadOrders()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                binding.swipeRefreshOrders.isRefreshing = (state is OrdersUiState.Loading)

                when (state) {
                    is OrdersUiState.Loading -> {
                        binding.llEmptyOrders.visibility = View.GONE
                    }
                    is OrdersUiState.Success -> {
                        binding.llEmptyOrders.visibility = View.GONE
                        binding.rvOrders.visibility = View.VISIBLE
                        orderAdapter.submitList(state.orders)
                    }
                    is OrdersUiState.Empty -> {
                        binding.rvOrders.visibility = View.GONE
                        binding.llEmptyOrders.visibility = View.VISIBLE
                    }
                    is OrdersUiState.Error -> {
                        binding.rvOrders.visibility = View.GONE
                        binding.llEmptyOrders.visibility = View.VISIBLE
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
