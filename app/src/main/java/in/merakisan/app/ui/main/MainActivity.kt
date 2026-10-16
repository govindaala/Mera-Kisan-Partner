package in.merakisan.app.ui.main

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import in.merakisan.app.R
import in.merakisan.app.core.config.FeatureManager
import in.merakisan.app.core.security.SessionManager
import in.merakisan.app.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        syncRemoteConfiguration()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        // NavOptions: टैब बदलने पर सब-स्क्रीन्स (जैसे AddCrop) को स्टैक में न फँसाए
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
                // यदि यूज़र होम पर क्लिक करे, तो सब-स्क्रीन्स को पॉप करके सीधे होम पर लाएँ
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

        // उसी टैब को दोबारा दबाने पर उस टैब की मुख्य स्क्रीन पर वापस आना
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
                    adaptNavigationForRole()

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

    private fun adaptNavigationForRole() {
        val role = try {
            SessionManager.getUserRole(this)
        } catch (_: Throwable) {
            "BOTH"
        }
        val menu = binding.bottomNavigation.menu

        val marketplaceItem = menu.findItem(R.id.nav_marketplace)
        val requestsItem = menu.findItem(R.id.nav_buyer_requests)

        when (role.uppercase()) {
            "FARMER" -> {
                marketplaceItem?.title = "मेरी फसलें"
                requestsItem?.title = "मांग बोर्ड"
            }
            "BUYER" -> {
                marketplaceItem?.title = "फसलें खोजें"
                requestsItem?.title = "मुझे चाहिए"
            }
            else -> {
                marketplaceItem?.title = "बाज़ार"
                requestsItem?.title = "मांग / जरूरत"
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
