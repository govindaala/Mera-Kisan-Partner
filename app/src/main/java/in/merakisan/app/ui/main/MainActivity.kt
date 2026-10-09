// app/src/main/java/in/merakisan/app/ui/main/MainActivity.kt
package in.merakisan.app.ui.main

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
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

        binding.bottomNavigation.setupWithNavController(navController)

        // स्प्लैश और रोल चयन पर बॉटम नेविगेशन बार को छिपाना
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.splashFragment, R.id.roleSelectionFragment -> {
                    binding.bottomNavigation.visibility = View.GONE
                }
                else -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                    adaptNavigationForRole()
                }
            }
        }
    }

    /**
     * किसान/खरीदार रोल के अनुसार मेन्यू शीर्षकों को अनुकूलित करना
     */
    private fun adaptNavigationForRole() {
        val role = SessionManager.getUserRole(this)
        val menu = binding.bottomNavigation.menu

        val marketplaceItem = menu.findItem(R.id.nav_marketplace)
        val requestsItem = menu.findItem(R.id.nav_buyer_requests)

        when (role) {
            "FARMER" -> {
                marketplaceItem.title = "मेरी फसलें"
                requestsItem.title = "मांग बोर्ड"
            }
            "BUYER" -> {
                marketplaceItem.title = "फसलें खोजें"
                requestsItem.title = "मुझे चाहिए"
            }
            else -> { // BOTH
                marketplaceItem.title = "बाज़ार"
                requestsItem.title = "मांग / जरूरत"
            }
        }
    }

    /**
     * Vercel बैकएंड से रिमोट फ़ीचर्स और मेंटेनेंस मोड को बैकग्राउंड में सिंक करना
     */
    private fun syncRemoteConfiguration() {
        lifecycleScope.launch {
            FeatureManager.syncRemoteConfig(this@MainActivity)
            checkMaintenanceState()
        }
    }

    private fun checkMaintenanceState() {
        if (FeatureManager.isMaintenanceMode(this)) {
            binding.tvMaintenanceBanner.apply {
                visibility = View.VISIBLE
                text = FeatureManager.getMaintenanceMessage(this@MainActivity)
            }
        } else {
            binding.tvMaintenanceBanner.visibility = View.GONE
        }
    }
}
