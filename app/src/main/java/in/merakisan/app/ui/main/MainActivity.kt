// app/src/main/java/in/merakisan/app/ui/main/MainActivity.kt
package `in`.merakisan.app.ui.main

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import `in`.merakisan.app.R
import `in`.merakisan.app.core.config.FeatureManager
import `in`.merakisan.app.core.state.RoleStateManager
import kotlinx.coroutines.launch

/**
 * Fixes Video Bug #1 (00:06, 00:09, 00:24):
 * Prevents top headers from overlapping with the Android status bar clock/icons (17:56)
 * by applying proper systemBars() window insets and dark green status bar color.
 */
class MainActivity : AppCompatActivity() {

    private var navController: NavController? = null
    private var bottomNavView: BottomNavigationView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure content is laid out below the system status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.parseColor("#1B5E20")
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        setContentView(R.layout.activity_main)

        val rootView = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        navController = navHostFragment?.navController

        bottomNavView = findViewById(R.id.bottom_navigation)
        val controller = navController
        val bottomNav = bottomNavView

        if (controller != null && bottomNav != null) {
            bottomNav.setupWithNavController(controller)

            controller.addOnDestinationChangedListener { _, destination, _ ->
                when (destination.id) {
                    R.id.loginFragment,
                    R.id.splashFragment,
                    R.id.roleSelectionFragment -> {
                        bottomNav.visibility = View.GONE
                    }
                    else -> {
                        bottomNav.visibility = View.VISIBLE
                        syncBottomNavigationLabelsWithRole()
                    }
                }
            }
        }

        syncBottomNavigationLabelsWithRole()

        lifecycleScope.launch {
            FeatureManager.syncRemoteConfig(applicationContext)
        }
    }

    /**
     * Master Spec Sec 5 & merged.txt Sec 76:
     * Adapts Bottom Navigation labels dynamically to Farmer / Buyer / Both role.
     */
    fun syncBottomNavigationLabelsWithRole() {
        val bottomNav = bottomNavView ?: return
        val prefs = getSharedPreferences("mera_kisan_role_prefs", Context.MODE_PRIVATE)
        val activeRole = (prefs.getString("active_role", null)
            ?: RoleStateManager.currentRole.value.name).uppercase()

        val menu = bottomNav.menu
        when (activeRole) {
            "BUYER" -> {
                menu.findItem(R.id.homeFragment)?.title = "होम"
                menu.findItem(R.id.marketplaceFragment)?.title = "खरीदें (Search)"
                menu.findItem(R.id.buyerRequestsFragment)?.title = "मुझे चाहिए"
                menu.findItem(R.id.ordersFragment)?.title = "मेरे ऑर्डर्स"
                menu.findItem(R.id.profileFragment)?.title = "प्रोफ़ाइल"
            }
            "FARMER" -> {
                menu.findItem(R.id.homeFragment)?.title = "होम"
                menu.findItem(R.id.marketplaceFragment)?.title = "बाज़ार भाव"
                menu.findItem(R.id.buyerRequestsFragment)?.title = "ग्राहक मांग"
                menu.findItem(R.id.ordersFragment)?.title = "बिक्री ऑर्डर्स"
                menu.findItem(R.id.profileFragment)?.title = "प्रोफ़ाइल"
            }
            else -> {
                menu.findItem(R.id.homeFragment)?.title = "होम"
                menu.findItem(R.id.marketplaceFragment)?.title = "बाज़ार (Buy/Sell)"
                menu.findItem(R.id.buyerRequestsFragment)?.title = "मुझे चाहिए"
                menu.findItem(R.id.ordersFragment)?.title = "ऑर्डर्स"
                menu.findItem(R.id.profileFragment)?.title = "प्रोफ़ाइल"
            }
        }
    }
}
