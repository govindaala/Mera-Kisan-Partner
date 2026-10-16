package `in`.merakisan.app.ui.main

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import `in`.merakisan.app.R
import `in`.merakisan.app.core.config.FeatureManager
import `in`.merakisan.app.core.state.RoleStateManager
import `in`.merakisan.app.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. RoleStateManager ko initialize karna
        RoleStateManager.init(this)

        setupNavigation()
        observeRoleStateChanges()
        syncRemoteConfiguration()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        // NavOptions: Tab change par purani sub-screens (jaise AddCrop) ko stack se saaf karna
        val navOptions = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setRestoreState(false)
            .setPopUpTo(R.id.homeFragment, inclusive = false, saveState = false)
            .build()

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val targetDestId = when (item.itemId) {
                R.id.nav_home, R.id.homeFragment -> R.id.homeFragment
                R.id.nav_marketplace, R.id.marketplaceFragment -> R.id.marketplaceFragment
                R.id.nav_buyer_requests, R.id.buyerRequestsFragment -> R.id.buyerRequestsFragment
                R.id.nav_orders, R.id.ordersFragment -> R.id.ordersFragment
                R.id.nav_profile, R.id.profileFragment -> R.id.profileFragment
                else -> null
            }

            if (targetDestId != null) {
                if (targetDestId == R.id.homeFragment) {
                    try {
                        navController.popBackStack(R.id.homeFragment, false)
                    } catch (_: Throwable) {}
                }

                if (navController.currentDestination?.id != targetDestId) {
                    try {
                        navController.navigate(targetDestId, null, navOptions)
                    } catch (_: Throwable) {
                        try {
                            navController.navigate(targetDestId)
                        } catch (_: Throwable) {}
                    }
                }
                true
            } else {
                false
            }
        }

        binding.bottomNavigation.setOnItemReselectedListener { item ->
            val targetDestId = when (item.itemId) {
                R.id.nav_home, R.id.homeFragment -> R.id.homeFragment
                R.id.nav_marketplace, R.id.marketplaceFragment -> R.id.marketplaceFragment
                R.id.nav_buyer_requests, R.id.buyerRequestsFragment -> R.id.buyerRequestsFragment
                R.id.nav_orders, R.id.ordersFragment -> R.id.ordersFragment
                R.id.nav_profile, R.id.profileFragment -> R.id.profileFragment
                else -> null
            }
            if (targetDestId != null) {
                try {
                    navController.popBackStack(targetDestId, false)
                } catch (_: Throwable) {}
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.splashFragment, R.id.loginFragment, R.id.roleSelectionFragment -> {
                    binding.bottomNavigation.visibility = View.GONE
                }
                else -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                    adaptNavigationForRole(RoleStateManager.getCurrentRole())

                    val menuId = when (destination.id) {
                        R.id.homeFragment -> R.id.nav_home
                        R.id.marketplaceFragment -> R.id.nav_marketplace
                        R.id.buyerRequestsFragment -> R.id.nav_buyer_requests
                        R.id.ordersFragment -> R.id.nav_orders
                        R.id.profileFragment -> R.id.nav_profile
                        else -> null
                    }
                    if (menuId != null && binding.bottomNavigation.selectedItemId != menuId) {
                        binding.bottomNavigation.menu.findItem(menuId)?.isChecked = true
                    }
                }
            }
        }
    }

    /**
     * Master Spec Sec 5: Profile me role switch karte hi bina app restart kiye
     * Bottom Navigation tabs instant update honge
     */
    private fun observeRoleStateChanges() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                RoleStateManager.roleFlow.collect { activeRole ->
                    adaptNavigationForRole(activeRole)
                }
            }
        }
    }

    private fun adaptNavigationForRole(role: String) {
        val menu = binding.bottomNavigation.menu
        val marketplaceItem = menu.findItem(R.id.nav_marketplace)
        val requestsItem = menu.findItem(R.id.nav_buyer_requests)
        val ordersItem = menu.findItem(R.id.nav_orders)

        when (role.trim().uppercase()) {
            RoleStateManager.ROLE_FARMER -> {
                marketplaceItem?.title = "मेरी फसलें"
                requestsItem?.title = "मांग बोर्ड"
                ordersItem?.title = "बिक्री ऑर्डर्स"
            }
            RoleStateManager.ROLE_BUYER -> {
                marketplaceItem?.title = "फसलें खोजें"
                requestsItem?.title = "मुझे चाहिए"
                ordersItem?.title = "खरीद ऑर्डर्स"
            }
            else -> {
                // BOTH Mode — Options Double
                marketplaceItem?.title = "बाज़ार / फसलें"
                requestsItem?.title = "मांग / ज़रूरत"
                ordersItem?.title = "ऑर्डर्स लेन-देन"
            }
        }
    }

    private fun syncRemoteConfiguration() {
        lifecycleScope.launch {
            try {
                FeatureManager.syncRemoteConfig(this@MainActivity)
                checkMaintenanceState()
            } catch (_: Throwable) {}
        }
    }

    private fun checkMaintenanceState() {
        try {
            if (FeatureManager.isMaintenanceMode(this)) {
                binding.tvMaintenanceBanner.apply {
                    visibility = View.VISIBLE
                    text = FeatureManager.getMaintenanceMessage(this@MainActivity)
                }
            } else {
                binding.tvMaintenanceBanner.visibility = View.GONE
            }
        } catch (_: Throwable) {
            binding.tvMaintenanceBanner.visibility = View.GONE
        }
    }
}
