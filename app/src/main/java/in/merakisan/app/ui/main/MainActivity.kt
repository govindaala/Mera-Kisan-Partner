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
import `in`.merakisan.app.ui.auth.LoginFragment
import `in`.merakisan.app.ui.auth.SplashFragment
import `in`.merakisan.app.ui.farmer.AddProductFragment
import `in`.merakisan.app.ui.farmer.FarmerPublicProfileFragment
import `in`.merakisan.app.ui.farmer.MyCropsFragment
import `in`.merakisan.app.ui.favorites.FavoritesFragment
import `in`.merakisan.app.ui.home.HomeFragment
import `in`.merakisan.app.ui.mandi.MandiPricesFragment
import `in`.merakisan.app.ui.marketing.AiPosterStudioFragment
import `in`.merakisan.app.ui.marketplace.MarketplaceFragment
import `in`.merakisan.app.ui.marketplace.ProductDetailFragment
import `in`.merakisan.app.ui.modules.BatchQrTraceabilityFragment
import `in`.merakisan.app.ui.modules.FairPriceCalculatorFragment
import `in`.merakisan.app.ui.modules.FarmBoxGroupBuyFragment
import `in`.merakisan.app.ui.modules.OilMillMarketplaceFragment
import `in`.merakisan.app.ui.offers.NegotiationHistoryFragment
import `in`.merakisan.app.ui.orders.OrdersFragment
import `in`.merakisan.app.ui.profile.ProfileFragment
import `in`.merakisan.app.ui.requests.BuyerRequestsFragment
import kotlinx.coroutines.launch

/**
 * MERA KISAN — UNIFIED MAIN ACTIVITY
 * Strictly aligned with actual repository tree:
 * - ui/farmer/MyCropsFragment.kt
 * - ui/farmer/AddProductFragment.kt
 * - ui/marketplace/MarketplaceFragment.kt
 * - ui/marketplace/ProductDetailFragment.kt
 * - ui/requests/BuyerRequestsFragment.kt
 * - ui/favorites/FavoritesFragment.kt
 * - ui/orders/OrdersFragment.kt
 * - ui/mandi/MandiPricesFragment.kt
 * - ui/marketing/AiPosterStudioFragment.kt
 * - ui/offers/NegotiationHistoryFragment.kt
 * - ui/modules/*
 * - ui/profile/ProfileFragment.kt
 */
class MainActivity : AppCompatActivity() {

    private lateinit var topGovernanceBar: LinearLayout
    private lateinit var activeModeBadgeText: TextView
    private lateinit var btnSwitchMode: Button
    private lateinit var btnQuickMandi: Button
    private lateinit var btnQuickSeedsAgri: Button
    private lateinit var btnQuickOffers: Button
    private lateinit var btnQuickFairPrice: Button

    private lateinit var fragmentContainer: FrameLayout
    private lateinit var adMobBannerSlot: FrameLayout
    private lateinit var bottomNavContainer: LinearLayout

    private var pendingDeepLinkProductId: String? = null
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

        // 1. Top Role Switcher & Quick Feature Bar
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

        val quickRowScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(0, dp(8), 0, 0)
        }
        val quickRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        btnQuickMandi = createQuickChipButton("📈 लाइव मंडी व AI सलाह", "#E8F5E9", "#1B5E20") {
            openMandiPricesScreen()
        }

        btnQuickSeedsAgri = createQuickChipButton("🌱 बीज, खाद व कृषि दवाई", "#FFF8E1", "#1B5E20") {
            openAgriInputsAndSeedsScreen("seeds")
        }

        btnQuickOffers = createQuickChipButton("🤝 मोलभाव (Offers)", "#E3F2FD", "#0D47A1") {
            openNegotiationOffersScreen()
        }

        btnQuickFairPrice = createQuickChipButton("🧮 उचित दाम कैलकुलेटर", "#F3E5F5", "#4A148C") {
            openFairPriceCalculatorScreen()
        }

        quickRow.addView(btnQuickMandi)
        quickRow.addView(btnQuickSeedsAgri)
        quickRow.addView(btnQuickOffers)
        quickRow.addView(btnQuickFairPrice)
        quickRowScroll.addView(quickRow)

        topGovernanceBar.addView(topRow)
        topGovernanceBar.addView(quickRowScroll)
        root.addView(topGovernanceBar)

        // 2. Main Fragment Host
        fragmentContainer = FrameLayout(this).apply {
            id = fragmentContainerId
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        root.addView(fragmentContainer)

        // 3. AdMob Banner Slot (Default OFF / View.GONE — Master Spec Sec 17)
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

    private fun createQuickChipButton(
        label: String,
        bgHex: String,
        textHex: String,
        onClick: () -> Unit
    ): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(Color.parseColor(textHex))
            background = roundedDrawable(bgHex, dp(8))
            setPadding(dp(10), dp(4), dp(10), dp(4))
            minHeight = dp(34)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.rightMargin = dp(8)
            layoutParams = lp
            setOnClickListener { onClick() }
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
        btnQuickOffers.visibility = if (FeatureManager.isEnabled("make_offer", this)) View.VISIBLE else View.GONE
        btnQuickFairPrice.visibility = if (FeatureManager.isEnabled("fair_price", this)) View.VISIBLE else View.GONE

        // Master Spec Sec 5 & merged.txt Sec 76 Role-Specific Bottom Navigation
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

        // Routes strictly to the exact Fragment classes in your repository tree
        val targetFragment: Fragment = when (tabKey) {
            "HOME" -> HomeFragment()
            "MY_CROPS" -> MyCropsFragment()
            "ADD_PRODUCT" -> AddProductFragment()
            "SEARCH" -> MarketplaceFragment()
            "BUYER_REQUEST" -> BuyerRequestsFragment()
            "FAVORITES" -> FavoritesFragment()
            "ORDERS" -> OrdersFragment()
            "PROFILE" -> ProfileFragment()
            else -> HomeFragment()
        }

        supportFragmentManager.beginTransaction()
            .replace(fragmentContainerId, targetFragment)
            .commitAllowingStateLoss()
    }

    // ========================================================================
    // NAVIGATION HELPERS MATCHING EXACT REPOSITORY FRAGMENTS
    // ========================================================================

    fun openProductDetailScreen(productId: String) {
        val frag = ProductDetailFragment().apply {
            arguments = Bundle().apply { putString("product_id", productId) }
        }
        supportFragmentManager.beginTransaction()
            .replace(fragmentContainerId, frag)
            .addToBackStack("product_detail")
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

    fun openMyCropsScreen() {
        supportFragmentManager.beginTransaction()
            .replace(fragmentContainerId, MyCropsFragment())
            .addToBackStack("my_crops")
            .commitAllowingStateLoss()
    }

    fun openFarmerPublicProfileScreen(farmerUid: String) {
        val frag = FarmerPublicProfileFragment().apply {
            arguments = Bundle().apply { putString("farmer_uid", farmerUid) }
        }
        supportFragmentManager.beginTransaction()
            .replace(fragmentContainerId, frag)
            .addToBackStack("farmer_public_profile")
            .commitAllowingStateLoss()
    }

    fun openAiPosterStudioScreen(productId: String? = null) {
        val frag = AiPosterStudioFragment().apply {
            if (!productId.isNullOrBlank()) {
                arguments = Bundle().apply { putString("product_id", productId) }
            }
        }
        supportFragmentManager.beginTransaction()
            .replace(fragmentContainerId, frag)
            .addToBackStack("ai_poster_studio")
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
        val frag = MarketplaceFragment().apply {
            arguments = Bundle().apply {
                putString("mode", "AGRI_INPUTS_AND_SEEDS")
                if (!initialCategory.isNullOrBlank()) {
                    putString("initial_category", initialCategory)
                }
            }
        }
        supportFragmentManager.beginTransaction()
            .replace(fragmentContainerId, frag)
            .addToBackStack("agri_inputs_seeds")
            .commitAllowingStateLoss()
    }

    fun openBuyerRequestsScreen() {
        supportFragmentManager.beginTransaction()
            .replace(fragmentContainerId, BuyerRequestsFragment())
            .addToBackStack("buyer_requests")
            .commitAllowingStateLoss()
    }

    fun openNegotiationOffersScreen() {
        supportFragmentManager.beginTransaction()
            .replace(fragmentContainerId, NegotiationHistoryFragment())
            .addToBackStack("negotiation_offers")
            .commitAllowingStateLoss()
    }

    fun openOrdersScreen() {
        supportFragmentManager.beginTransaction()
            .replace(fragmentContainerId, OrdersFragment())
            .addToBackStack("orders_screen")
            .commitAllowingStateLoss()
    }

    fun openFairPriceCalculatorScreen() {
        supportFragmentManager.beginTransaction()
            .replace(fragmentContainerId, FairPriceCalculatorFragment())
            .addToBackStack("fair_price_calculator")
            .commitAllowingStateLoss()
    }

    fun openBatchQrTraceabilityScreen(batchId: String? = null) {
        val frag = BatchQrTraceabilityFragment().apply {
            if (!batchId.isNullOrBlank()) {
                arguments = Bundle().apply { putString("batch_id", batchId) }
            }
        }
        supportFragmentManager.beginTransaction()
            .replace(fragmentContainerId, frag)
            .addToBackStack("batch_qr_traceability")
            .commitAllowingStateLoss()
    }

    fun openFarmBoxGroupBuyScreen() {
        supportFragmentManager.beginTransaction()
            .replace(fragmentContainerId, FarmBoxGroupBuyFragment())
            .addToBackStack("farm_box_group_buy")
            .commitAllowingStateLoss()
    }

    fun openOilMillMarketplaceScreen() {
        supportFragmentManager.beginTransaction()
            .replace(fragmentContainerId, OilMillMarketplaceFragment())
            .addToBackStack("oil_mill_marketplace")
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
