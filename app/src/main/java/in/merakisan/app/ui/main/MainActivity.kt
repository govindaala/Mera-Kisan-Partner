package `in`.merakisan.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import `in`.merakisan.app.BuildConfig
import `in`.merakisan.app.R
import `in`.merakisan.app.data.remote.ApiClient
import `in`.merakisan.app.data.remote.models.PublicConfigData
import `in`.merakisan.app.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        setupBottomNavigation(binding.bottomNavigation)
        applyRoleBasedNavigationMenu()
        checkRemoteConfigAndMaintenance()
        registerFcmTokenIfAuthenticated()
        handleIncomingDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIncomingDeepLink(intent)
    }

    /**
     * रोल (FARMER / BUYER / BOTH) के अनुसार बॉटम नेविगेशन के टाइटल सेट करना
     */
    fun applyRoleBasedNavigationMenu() {
        val prefs = getSharedPreferences("mera_kisan_prefs", Context.MODE_PRIVATE)
        val currentRole = prefs.getString("user_role", "BOTH") ?: "BOTH"
        val menu = binding.bottomNavigation.menu

        when (currentRole) {
            "BUYER" -> {
                menu.findItem(R.id.navigation_home)?.title = "होम"
                menu.findItem(R.id.navigation_demand)?.title = "मुझे चाहिए"
                menu.findItem(R.id.navigation_add_product)?.title = "पसंदीदा"
                menu.findItem(R.id.navigation_orders)?.title = "ऑर्डर्स"
                menu.findItem(R.id.navigation_profile)?.title = "प्रोफ़ाइल"
            }
            "FARMER" -> {
                menu.findItem(R.id.navigation_home)?.title = "होम"
                menu.findItem(R.id.navigation_demand)?.title = "मेरी फसलें"
                menu.findItem(R.id.navigation_add_product)?.title = "फसल जोड़ें"
                menu.findItem(R.id.navigation_orders)?.title = "ऑर्डर्स"
                menu.findItem(R.id.navigation_profile)?.title = "प्रोफ़ाइल"
            }
            else -> { // BOTH (एकीकृत मोड)
                menu.findItem(R.id.navigation_home)?.title = "बाज़ार (Home)"
                menu.findItem(R.id.navigation_demand)?.title = "मांग बोर्ड"
                menu.findItem(R.id.navigation_add_product)?.title = "बेचें (+)"
                menu.findItem(R.id.navigation_orders)?.title = "ऑर्डर्स"
                menu.findItem(R.id.navigation_profile)?.title = "प्रोफ़ाइल"
            }
        }
    }

    /**
     * बग फिक्स: 5वां प्रोफाइल टैब सही जगह खुले और चाइल्ड स्क्रीन (Add Crop आदि) बैक-स्टैक से साफ़ हों
     */
    private fun setupBottomNavigation(bottomNav: BottomNavigationView) {
        bottomNav.setOnItemSelectedListener { item ->
            val navOptions = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(false)
                .setEnterAnim(android.R.anim.fade_in)
                .setExitAnim(android.R.anim.fade_out)
                .setPopUpTo(
                    navController.graph.findStartDestination().id,
                    inclusive = false,
                    saveState = false
                )
                .build()

            when (item.itemId) {
                R.id.navigation_home -> {
                    navController.popBackStack(R.id.navigation_home, false)
                    navController.navigate(R.id.navigation_home, null, navOptions)
                    true
                }
                R.id.navigation_demand -> {
                    navController.navigate(R.id.navigation_demand, null, navOptions)
                    true
                }
                R.id.navigation_add_product -> {
                    val prefs = getSharedPreferences("mera_kisan_prefs", Context.MODE_PRIVATE)
                    val role = prefs.getString("user_role", "BOTH")
                    if (role == "BUYER") {
                        navController.navigate(R.id.navigation_favorites, null, navOptions)
                    } else {
                        navController.navigate(R.id.navigation_add_product, null, navOptions)
                    }
                    true
                }
                R.id.navigation_orders -> {
                    navController.navigate(R.id.navigation_orders, null, navOptions)
                    true
                }
                R.id.navigation_profile -> {
                    // 5वां टैब सीधे ProfileFragment खोलेगा (सर्च स्क्रीन नहीं)
                    navController.navigate(R.id.navigation_profile, null, navOptions)
                    true
                }
                else -> false
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment,
                R.id.roleSelectionFragment -> {
                    binding.bottomNavigation.visibility = View.GONE
                }
                else -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
            }
        }
    }

    /**
     * रिमोट कॉन्फ़िग: मेंटेनेंस मोड, फ़ोर्स अपडेट और AdMob (Default OFF) का नियंत्रण
     */
    private fun checkRemoteConfigAndMaintenance() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getPublicConfig()
                if (response.isSuccessful && response.body()?.success == true) {
                    val config = response.body()?.data ?: return@launch
                    saveConfigLocally(config)

                    // 1. मेंटेनेंस मोड जांच
                    if (config.maintenanceMode == true) {
                        showMaintenanceDialog(
                            config.maintenanceMessage
                                ?: "Mera Kisan पर रखरखाव कार्य चल रहा है। कृपया कुछ समय बाद प्रयास करें।"
                        )
                        return@launch
                    }

                    // 2. फ़ोर्स अपडेट (Minimum App Version) जांच
                    val minVersionCode = config.minAppVersionCode ?: 1
                    if (BuildConfig.VERSION_CODE < minVersionCode && config.forceUpdate == true) {
                        showForceUpdateDialog()
                        return@launch
                    }

                    // 3. AdMob नियंत्रण (डिफ़ॉल्ट रूप से OFF)
                    val isAdMobEnabled = config.features?.get("admob") == true &&
                                         config.admobKillSwitch != true
                    setupAdMobBanner(isAdMobEnabled)
                }
            } catch (e: Exception) {
                // ऑफलाइन या नेटवर्क विफलता पर ऐप क्रैश नहीं होगा
                setupAdMobBanner(false)
            }
        }
    }

    private fun saveConfigLocally(config: PublicConfigData) {
        val prefs = getSharedPreferences("mera_kisan_config", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("payment_enabled", config.features?.get("payment") == true)
            putBoolean("boost_enabled", config.features?.get("crop_boost") == true || config.features?.get("boost") == true)
            putBoolean("mandi_enabled", config.features?.get("mandi_prices") == true || config.features?.get("mandi_price") == true)
            putBoolean("demand_board_enabled", config.features?.get("buyer_request") == true || config.features?.get("demand_board") == true)
            putBoolean("offers_enabled", config.features?.get("offers") == true || config.features?.get("make_offer") == true)
            putBoolean("favorites_enabled", config.features?.get("favorites") == true)
            putBoolean("ai_poster_enabled", config.features?.get("ai_poster") != false)
            putBoolean("chat_enabled", config.features?.get("chat") == true)
            putBoolean("farm_box_enabled", config.features?.get("farm_box") == true)
            putBoolean("oil_market_enabled", config.features?.get("oil_marketplace") == true)
            putInt("boost_price", config.boostPrice ?: 99)
            putInt("boost_days", config.boostDays ?: 3)
            putString("support_whatsapp", config.supportWhatsapp ?: "")
            putString("home_banner_text", config.homeBannerText ?: "")
            apply()
        }
    }

    private fun setupAdMobBanner(enabled: Boolean) {
        // AdMob डिफ़ॉल्ट रूप से OFF रहेगा जब तक एडमिन पैनल से ON न हो
        binding.adViewContainer?.visibility = if (enabled) View.VISIBLE else View.GONE
    }

    private fun showMaintenanceDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("सिस्टम मेंटेनेंस")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("बंद करें") { _, _ -> finish() }
            .show()
    }

    private fun showForceUpdateDialog() {
        AlertDialog.Builder(this)
            .setTitle("नया अपडेट आवश्यक है")
            .setMessage("बेहतर सुरक्षा और नए फीचर्स के लिए कृपया Mera Kisan ऐप को अपडेट करें।")
            .setCancelable(false)
            .setPositiveButton("अपडेट करें") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                startActivity(intent)
                finish()
            }
            .show()
    }

    /**
     * पुश नोटिफिकेशन (FCM) टोकन सिंक
     */
    private fun registerFcmTokenIfAuthenticated() {
        val currentUser = auth.currentUser ?: return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
            currentUser.getIdToken(false).addOnSuccessListener { tokenResult ->
                val bearer = "Bearer ${tokenResult.token}"
                lifecycleScope.launch {
                    try {
                        ApiClient.apiService.registerNotificationToken(bearer, mapOf("fcmToken" to fcmToken))
                    } catch (_: Exception) {}
                }
            }
        }
    }

    /**
     * शेयर किए गए प्रोडक्ट लिंक (Deep Link: merakisan.in/product/ID) को सीधे ऐप में खोलना
     */
    private fun handleIncomingDeepLink(intent: Intent?) {
        val data: Uri = intent?.data ?: return
        val pathSegments = data.pathSegments
        if (pathSegments.size >= 2 && pathSegments[0] == "product") {
            val productId = pathSegments[1]
            val bundle = bundleOf("productId" to productId)
            navController.navigate(R.id.productDetailFragment, bundle)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
