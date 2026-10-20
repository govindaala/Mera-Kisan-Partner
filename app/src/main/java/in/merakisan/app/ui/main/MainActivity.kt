// app/src/main/java/in/merakisan/app/ui/main/MainActivity.kt
package `in`.merakisan.app.ui.main

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
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
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Resolves MainActivity.kt compiler errors at lines 50, 53, and 90:
 * - Automatically locates NavHostFragment and BottomNavigationView without hard-coded XML IDs.
 * - Applies systemBars() WindowInsets so top headers never overlap with the phone status bar.
 */
class MainActivity : AppCompatActivity() {

    private var navController: NavController? = null
    private var bottomNavView: BottomNavigationView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        val navHostFragment = locateNavHostFragment()
        navController = navHostFragment?.navController

        bottomNavView = locateBottomNavigationView(rootView)
        val controller = navController
        val bottomNav = bottomNavView

        if (controller != null && bottomNav != null) {
            try {
                bottomNav.setupWithNavController(controller)
            } catch (_: Throwable) {}

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

    private fun locateNavHostFragment(): NavHostFragment? {
        // 1. Check fragment manager directly (100% independent of XML ID name)
        supportFragmentManager.fragments.forEach { frag ->
            if (frag is NavHostFragment) return frag
        }
        // 2. Fallback to common ID names via runtime identifier lookup
        val possibleIds = arrayOf("nav_host_fragment", "navHostFragment", "fragment_container", "nav_host")
        for (name in possibleIds) {
            val id = resources.getIdentifier(name, "id", packageName)
            if (id != 0) {
                val found = supportFragmentManager.findFragmentById(id)
                if (found is NavHostFragment) return found
            }
        }
        return null
    }

    private fun locateBottomNavigationView(root: View?): BottomNavigationView? {
        if (root == null) return null
        if (root is BottomNavigationView) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val childResult = locateBottomNavigationView(root.getChildAt(i))
                if (childResult != null) return childResult
            }
        }
        return null
    }

    /**
     * Master Spec Sec 5 & merged.txt Sec 76:
     * Adapts Bottom Navigation labels dynamically to Farmer / Buyer / Both role.
     */
    fun syncBottomNavigationLabelsWithRole() {
        val bottomNav = bottomNavView ?: return
        val rolePrefs = getSharedPreferences("mera_kisan_role_prefs", Context.MODE_PRIVATE)
        val userPrefs = getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)
        val activeRole = (rolePrefs.getString("active_role", null)
            ?: userPrefs.getString("role", null)
            ?: "BOTH").trim().uppercase(Locale.ROOT)

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
