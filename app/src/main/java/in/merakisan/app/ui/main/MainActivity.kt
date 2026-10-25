// app/src/main/java/in/merakisan/app/ui/main/MainActivity.kt
package `in`.merakisan.app.ui.main

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import `in`.merakisan.app.core.config.FeatureManager
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.security.SessionManager
import `in`.merakisan.app.ui.agri.AgriInputsSeedsFragment
import `in`.merakisan.app.ui.auth.LoginFragment
import `in`.merakisan.app.ui.auth.SplashFragment
import `in`.merakisan.app.ui.crops.AddProductFragment
import `in`.merakisan.app.ui.crops.MarketplaceFragment
import `in`.merakisan.app.ui.crops.ProductDetailFragment
import `in`.merakisan.app.ui.home.HomeFragment
import `in`.merakisan.app.ui.mandi.MandiPricesFragment
import `in`.merakisan.app.ui.modules.BuyerRequestFragment
import `in`.merakisan.app.ui.modules.FavoritesFollowFragment
import `in`.merakisan.app.ui.orders.OrdersFragment
import `in`.merakisan.app.ui.profile.ProfileFragment
import kotlinx.coroutines.launch

/**
 * MERA KISAN — UNIFIED MAIN ACTIVITY
 *
 * Implements:
 * - Strict Farmer vs Buyer Bottom Navigation (Master Spec Sec 5 & merged.txt Sec 76).
 * - 1-Tap Role Switcher synced with Backend `/auth/role`.
 * - Remote Feature Flag Tab Visibility (`FeatureManager.isEnabled`).
 * - Deep Link Routing (`/product/{productId}` and `merakisan://product/{productId}`).
 * - AdMob Architectural Slot (Default OFF / View.GONE unless enabled by Admin).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var topGovernanceBar: LinearLayout
    private lateinit var activeModeBadgeText: TextView
    private lateinit var btnSwitchMode: Button
    private lateinit var btnQuickMandi: Button
    private lateinit var btnQuickAgriInputs: Button

    private lateinit var fragmentContainer: FrameLayout
    private lateinit var adMobBannerSlot: FrameLayout
    private lateinit var bottomNavContainer: LinearLayout

    private var pendingDeepLinkProductId: String? = null
    private var currentSelectedTabKey: String = "HOME"

    private val fragmentContainerId = View.generateViewId()

    data class NavTabItem(
        val key: String,
        val labelHi: String,
        val featureFlagKey: String? = null
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FeatureManager.initFromLocalCache(this)

        buildUnifiedActivityLayout()
        extractDeepLinkProductId(intent)

        if (savedInstanceState == null) {
            showFullscreenAuthFragment(SplashFragment())
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractDeepLinkProductId(intent)
        if (SessionManager.isLoggedInAndOnboarded(this)) {
            consumePendingDeepLinkIfAny()
        }
    }

    private fun buildUnifiedActivityLayout() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F2F6F2"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // 1. Top Role & Quick Utility Bar (Visible after Onboarding)
        topGovernanceBar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1B5E20"))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            visibility = View.GONE
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        activeModeBadgeText = TextView(this).apply {
            text = "👨‍🌾 किसान मोड"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        btnSwitchMode = Button(this).apply {
            text = "🛒 खरीदार मोड में बदलें"
            isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = roundedDrawable("#BF360C", dp(8))
            setPadding(dp(10), dp(6), dp(10), dp(6))
            minHeight = dp(36)
            setOnClickListener { toggleFarmerBuyerMode() }
        }

        topRow.addView(activeModeBadgeText)
        topRow.addView(btnSwitchMode)

        // Quick Action Chips Row (Live Mandi Prices + Seeds & Agri-Inputs)
        val quickRowScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(0, dp(8), 0, 0)
        }
        val quickRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        btnQuickMandi = Button(this).apply {
            text = "📈 लाइव मंडी भाव व AI सलाह"
            isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(Color.parseColor("#1B5E20"))
            background = roundedDrawable("#E8F5E9", dp(8))
            setPadding(dp(10), dp(4), dp(10), dp(4))
            minHeight = dp(34)
            setOnClickListener { openMandiPricesScreen() }
        }

        btnQuickAgriInputs = Button(this).apply {
            text = "🧪 बीज, खाद व कृषि दवाई (Pickup)"
            isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(Color.parseColor("#1B5E20"))
            background = roundedDrawable("#FFF8E1", dp(8))
            setPadding(dp(10), dp(4), dp(10), dp(4))
            minHeight = dp(34)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.leftMargin = dp(8)
            layoutParams = lp
            setOnClickListener { openAgriInputsAndSeedsScreen() }
        }

        quickRow.addView(btnQuickMandi)
        quickRow.addView(btnQuickAgriInputs)
        quickRowScroll.addView(quickRow)

        topGovernanceBar.addView(topRow)
        topGovernanceBar.addView(quickRowScroll)
        root.addView(topGovernanceBar)

        // 2. Main Fragment Container
        fragmentContainer = FrameLayout(this).apply {
            id = fragmentContainerId
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        root.addView(fragmentContainer)

        // 3. AdMob Architectural Slot (Default OFF / View.GONE — Master Spec Sec 17)
        adMobBannerSlot = FrameLayout(this).apply {
            visibility = View.GONE
            setBackgroundColor(Color.parseColor("#ECEFF1"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        root.addView(adMobBannerSlot)

        // 4. Role-Specific Bottom Navigation Bar
        bottomNavContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            elevation = dp(8).toFloat()
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(60)
            )
        }
        root.addView(bottomNavContainer)

        setContentView(root)
    }

    private fun extractDeepLinkProductId(incomingIntent: Intent?) {
        val dataUri: Uri = incomingIntent?.data ?: return
        val pathSegments = dataUri.pathSegments
        if (pathSegments != null && pathSegments.size >= 2 && pathSegments[0].equals("product", ignoreCase = true)) {
            pendingDeepLinkProductId = pathSegments[1]
        } else if (dataUri.host.equals("product", ignoreCase = true) && !dataUri.lastPathSegment.isNullOrBlank()) {
            pendingDeepLinkProductId = dataUri.lastPathSegment
        }
    }

    private fun consumePendingDeepLinkIfAny(): Boolean {
        val targetProductId = pendingDeepLinkProductId ?: return false
        pendingDeepLinkProductId = null
        openProductDetailScreen(targetProductId)
        return true
    }

    /**
     * SplashFragment या LoginFragment में प्रमाणीकरण व ऑनबोर्डिंग पूर्ण होने पर कॉल होता है।
     */
    fun onUserAuthenticatedAndOnboarded() {
        topGovernanceBar.visibility = View.VISIBLE
        bottomNavContainer.visibility = View.VISIBLE
        refreshAdMobSlotVisibility()
        refreshRoleNavigation(defaultTabKey = "HOME")
        consumePendingDeepLinkIfAny()
    }

    fun openLoginAndOnboardingScreen() {
        topGovernanceBar.visibility = View.GONE
        bottomNavContainer.visibility = View.GONE
        adMobBannerSlot.visibility = View.GONE
        showFullscreenAuthFragment(LoginFragment())
    }

    private fun showFullscreenAuthFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(fragmentContainerId, fragment)
            .commitAllowingStateLoss()
    }

    /**
     * किसान मोड <-> खरीदार मोड स्विच करना (सर्वर के `/auth/role` के साथ लाइव सिंक)
     */
    fun toggleFarmerBuyerMode() {
        val currentlyFarmer = SessionManager.isFarmerModeActive(this)
        val nextMode = if (currentlyFarmer) "BUYER" else "FARMER"

        SessionManager.setActiveMode(this, nextMode)
        refreshRoleNavigation(defaultTabKey = "HOME")

        val modeTitleHi = if (nextMode == "FARMER") "👨‍🌾 किसान विक्रेता मोड" else "🛒 खरीदार मोड"
        Toast.makeText(this, "अब आप '$modeTitleHi' में हैं।", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            ApiClient.switchActiveRole(this@MainActivity, nextMode.lowercase())
        }
    }

    /**
     * वर्तमान भूमिका (Farmer vs Buyer) और FeatureManager फ्लैग्स के अनुसार बॉटम टैब्स रेंडर करता है।
     */
    fun refreshRoleNavigation(defaultTabKey: String = "HOME") {
        val isFarmer = SessionManager.isFarmerModeActive(this)
        val userName = SessionManager.getUserName(this)

        if (isFarmer) {
            activeModeBadgeText.text = "👨‍🌾 किसान मोड ${if (userName.isNotBlank()) "• $userName" else ""}"
            btnSwitchMode.text = "🛒 खरीदार मोड में बदलें"
        } else {
            activeModeBadgeText.text = "🛒 खरीदार मोड ${if (userName.isNotBlank()) "• $userName" else ""}"
            btnSwitchMode.text = "👨‍🌾 किसान मोड में बदलें"
        }

        btnQuickMandi.visibility = if (FeatureManager.isEnabled("mandi_price", this)) View.VISIBLE else View.GONE
        btnQuickAgriInputs.visibility = if (
            FeatureManager.isEnabled("agri_inputs_marketplace", this) ||
            FeatureManager.isEnabled("seeds_marketplace", this)
        ) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // Master Spec Sec 5 & merged.txt Sec 76 Role-Specific Navigation Tabs
        val rawTabs = if (isFarmer) {
            listOf(
                NavTabItem("HOME", "🏠 होम", "farmer_mode"),
                NavTabItem("MY_CROPS", "🌾 मेरी फसलें", "product_listing"),
                NavTabItem("ADD_PRODUCT", "➕ फसल डालें", "product_listing"),
                NavTabItem("ORDERS", "📦 ऑर्डर्स", "orders"),
                NavTabItem("PROFILE", "👤 प्रोफ़ाइल", null)
            )
        } else {
            listOf(
                NavTabItem("HOME", "🏠 होम", "buyer_mode"),
                NavTabItem("SEARCH", "🔍 खोजें", "product_search"),
                NavTabItem("BUYER_REQUEST", "📢 मुझे चाहिए", "buyer_request"),
                NavTabItem("FAVORITES", "❤️ पसंदीदा", "favorites"),
                NavTabItem("PROFILE", "👤 प्रोफ़ाइल", null)
            )
        }

        val activeTabs = rawTabs.filter { tab ->
            tab.featureFlagKey == null || FeatureManager.isEnabled(tab.featureFlagKey, this)
        }

        bottomNavContainer.removeAllViews()
        for (tab in activeTabs) {
            val tabView = TextView(this).apply {
                text = tab.labelHi
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(dp(4), dp(8), dp(4), dp(8))
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1f
                )
                setOnClickListener {
                    selectBottomTab(tab.key, activeTabs)
                }
            }
            bottomNavContainer.addView(tabView)
        }

        selectBottomTab(defaultTabKey, activeTabs)
    }

    private fun selectBottomTab(tabKey: String, tabs: List<NavTabItem>) {
        currentSelectedTabKey = tabKey
        for (i in 0 until bottomNavContainer.childCount) {
            val child = bottomNavContainer.getChildAt(i) as? TextView ?: continue
            val item = tabs.getOrNull(i) ?: continue
            if (item.key == tabKey) {
                child.setTextColor(Color.parseColor("#1B5E20"))
                child.setTypeface(child.typeface, Typeface.BOLD)
                child.setBackgroundColor(Color.parseColor("#E8F5E9"))
            } else {
                child.setTextColor(Color.parseColor("#556655"))
                child.setTypeface(null, Typeface.NORMAL)
                child.setBackgroundColor(Color.WHITE)
            }
        }

        val targetFragment: Fragment = when (tabKey) {
            "HOME" -> HomeFragment()
            "MY_CROPS" -> MarketplaceFragment().apply {
                arguments = Bundle().apply { putString("mode", "FARMER_MY_CROPS") }
            }
            "ADD_PRODUCT" -> AddProductFragment()
            "SEARCH" -> MarketplaceFragment().apply {
                arguments = Bundle().apply { putString("mode", "BUYER_DISCOVERY") }
            }
            "BUYER_REQUEST" -> BuyerRequestFragment()
            "FAVORITES" -> FavoritesFollowFragment()
            "ORDERS" -> OrdersFragment()
            "PROFILE" -> ProfileFragment()
            else -> HomeFragment()
        }

        supportFragmentManager.beginTransaction()
            .replace(fragmentContainerId, targetFragment)
            .commitAllowingStateLoss()
    }

    // अन्य स्क्रीन खोलने के नेविगेशन हेल्पर्स
    fun openProductDetailScreen(productId: String) {
        val frag = ProductDetailFragment().apply {
            arguments = Bundle().apply { putString("product_id", productId) }
        }
        supportFragmentManager.beginTransaction()
            .replace(fragmentContainerId, frag)
            .addToBackStack("product_detail")
            .commitAllowingStateLoss()
    }

    fun openMandiPricesScreen() {
        if (!FeatureManager.isEnabled("mandi_price", this)) {
            Toast.makeText(this, "मंडी भाव सुविधा अभी बंद है।", Toast.LENGTH_SHORT).show()
            return
        }
        supportFragmentManager.beginTransaction()
            .replace(fragmentContainerId, MandiPricesFragment())
            .addToBackStack("mandi_prices")
            .commitAllowingStateLoss()
    }

    fun openAgriInputsAndSeedsScreen(initialCategory: String? = null) {
        val frag = AgriInputsSeedsFragment().apply {
            if (!initialCategory.isNullOrBlank()) {
                arguments = Bundle().apply { putString("initial_category", initialCategory) }
            }
        }
        supportFragmentManager.beginTransaction()
            .replace(fragmentContainerId, frag)
            .addToBackStack("agri_inputs_seeds")
            .commitAllowingStateLoss()
    }

    fun openAddProductScreen(editProductId: String? = null) {
        val frag = AddProductFragment().apply {
            if (!editProductId.isNullOrBlank()) {
                arguments = Bundle().apply { putString("edit_product_id", editProductId) }
            }
        }
        supportFragmentManager.beginTransaction()
            .replace(fragmentContainerId, frag)
            .addToBackStack("add_edit_product")
            .commitAllowingStateLoss()
    }

    fun openBuyerRequestsScreen() {
        supportFragmentManager.beginTransaction()
            .replace(fragmentContainerId, BuyerRequestFragment())
            .addToBackStack("buyer_requests")
            .commitAllowingStateLoss()
    }

    fun openOrdersScreen() {
        supportFragmentManager.beginTransaction()
            .replace(fragmentContainerId, OrdersFragment())
            .addToBackStack("orders_screen")
            .commitAllowingStateLoss()
    }

    private fun refreshAdMobSlotVisibility() {
        val adConfig = FeatureManager.getAdMobConfig(this)
        if (adConfig.enabled && adConfig.bannerUnitId.isNotBlank()) {
            adMobBannerSlot.visibility = View.VISIBLE
        } else {
            adMobBannerSlot.visibility = View.GONE
        }
    }

    private fun roundedDrawable(hexColor: String, radiusPx: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx.toFloat()
            setColor(Color.parseColor(hexColor))
        }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
}
