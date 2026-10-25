// app/src/main/java/in/merakisan/app/ui/home/HomeFragment.kt
package `in`.merakisan.app.ui.home

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import `in`.merakisan.app.core.config.FeatureManager
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.security.SessionManager
import `in`.merakisan.app.ui.main.MainActivity
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * MERA KISAN — UNIFIED ROLE-SEPARATED HOME SCREEN
 *
 * Strict Role Separation (Master Spec Sec 5 & merged.txt Final Spec Sec 11, 63, 64):
 * - When FARMER mode is active: Renders ONLY the Farmer Seller Dashboard (Active products count,
 *   Orders count, Buyer Requests matching farmer, Add Crop, Crop Boost, Mandi AI Advisor, Share).
 * - When BUYER mode is active: Renders ONLY the Buyer Direct-from-Farm Marketplace (Search,
 *   Small Quantity 1-25kg, Fresh Today, Desi/Traditional categories, Nearby products, Buyer Request).
 * - Uses ONLY ApiClient (Single Point of Server Connection) and 0% Dummy Data.
 */
class HomeFragment : Fragment() {

    private lateinit var rootContentLayout: LinearLayout
    private lateinit var loadingBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val scroll = ScrollView(ctx).apply {
            isFillViewport = true
            setBackgroundColor(Color.parseColor("#F2F6F2"))
        }

        val wrapper = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(28))
        }

        loadingBar = ProgressBar(ctx).apply {
            visibility = View.GONE
        }

        rootContentLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }

        wrapper.addView(loadingBar)
        wrapper.addView(rootContentLayout)
        scroll.addView(wrapper)
        return scroll
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        renderRoleSpecificHome()
    }

    private fun renderRoleSpecificHome() {
        val ctx = context ?: return
        rootContentLayout.removeAllViews()

        // 1. Remote Config Announcement / Home Banner
        val bannerText = FeatureManager.getHomeBannerText()
        val announcement = FeatureManager.getAnnouncementText()

        val bannerCard = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#1B5E20", dp(14))
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        bannerCard.addView(
            TextView(ctx).apply {
                text = "🌾 $bannerText"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setTypeface(typeface, Typeface.BOLD)
            }
        )
        if (announcement.isNotBlank()) {
            bannerCard.addView(
                TextView(ctx).apply {
                    text = "📢 $announcement"
                    setTextColor(Color.parseColor("#FFF59D"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setPadding(0, dp(4), 0, 0)
                }
            )
        }
        rootContentLayout.addView(bannerCard)

        // 2. Strict Mode Routing: Farmer vs Buyer
        if (SessionManager.isFarmerModeActive(ctx)) {
            buildFarmerModeDashboard()
        } else {
            buildBuyerModeHome()
        }
    }

    // ========================================================================
    // A. FARMER MODE HOME (merged.txt Sec 64 — केवल किसान विक्रेता के विकल्प)
    // ========================================================================
    private fun buildFarmerModeDashboard() {
        val ctx = context ?: return
        val farmerUid = SessionManager.getUserUid(ctx) ?: ""
        val farmerName = SessionManager.getUserName(ctx).ifBlank { "किसान" }
        val district = SessionManager.getUserDistrict(ctx)
        val verifBadge = formatVerificationBadgeHi(SessionManager.getVerificationStatus(ctx))

        // Farmer Identity & Verification Summary Card
        val welcomeCard = createCard().apply {
            addView(
                TextView(ctx).apply {
                    text = "👨‍🌾 नमस्ते, $farmerName"
                    setTextColor(Color.parseColor("#124116"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    setTypeface(typeface, Typeface.BOLD)
                }
            )
            addView(
                TextView(ctx).apply {
                    text = "स्थान: ${district.ifBlank { "अपना ज़िला प्रोफ़ाइल में जोड़ें" }} • $verifBadge"
                    setTextColor(Color.parseColor("#2E7D32"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setPadding(0, dp(4), 0, dp(10))
                }
            )
        }

        // Live Stats Row (Active Products | Orders | Buyer Demands)
        val statsRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 3f
        }
        val statActiveCropsVal = TextView(ctx)
        val statOrdersVal = TextView(ctx)
        val statDemandsVal = TextView(ctx)

        statsRow.addView(createStatBox("सक्रिय फसलें", statActiveCropsVal))
        statsRow.addView(createStatBox("प्राप्त ऑर्डर्स", statOrdersVal))
        statsRow.addView(createStatBox("ग्राहक मांगें", statDemandsVal))
        welcomeCard.addView(statsRow)
        rootContentLayout.addView(welcomeCard)

        // Farmer Primary Action Buttons
        val actionsCard = createCard()
        actionsCard.addView(sectionTitle("⚡ त्वरित किसान कार्य (Quick Seller Actions)"))

        val btnAddCrop = createActionButton("➕ नई फसल / बीज / उत्पाद बेचने के लिए डालें", "#1B5E20") {
            (activity as? MainActivity)?.openAddProductScreen(null)
        }
        actionsCard.addView(btnAddCrop)

        if (FeatureManager.isEnabled("mandi_price", ctx)) {
            actionsCard.addView(space(dp(8)))
            actionsCard.addView(
                createActionButton("📈 लाइव सरकारी मंडी भाव, 1-2 साल ग्राफ व AI विक्रय सलाह", "#0D47A1") {
                    (activity as? MainActivity)?.openMandiPricesScreen()
                }
            )
        }

        if (FeatureManager.isEnabled("agri_inputs_marketplace", ctx) || FeatureManager.isEnabled("seeds_marketplace", ctx)) {
            actionsCard.addView(space(dp(8)))
            actionsCard.addView(
                createActionButton("🧪 प्रमाणित बीज, खाद व कृषि दवाइयां (शहर पिकअप पॉइंट)", "#3E2723") {
                    (activity as? MainActivity)?.openAgriInputsAndSeedsScreen()
                }
            )
        }

        if (FeatureManager.isEnabled("buyer_request", ctx)) {
            actionsCard.addView(space(dp(8)))
            actionsCard.addView(
                createActionButton("📢 ग्राहकों की 'मुझे यह चाहिए' मांग देखें और भाव भेजें", "#BF360C") {
                    (activity as? MainActivity)?.openBuyerRequestsScreen()
                }
            )
        }

        rootContentLayout.addView(actionsCard)

        // Container for Farmer's Active Products & Nearby Buyer Demands
        val myProductsContainer = createCard().apply {
            addView(sectionTitle("🌾 मेरी हालिया सूचीबद्ध फसलें"))
        }
        val buyerDemandsContainer = createCard().apply {
            addView(sectionTitle("📢 ग्राहकों की ताज़ा मांगें (सीधे भाव प्रस्ताव दें)"))
        }

        rootContentLayout.addView(myProductsContainer)
        rootContentLayout.addView(buyerDemandsContainer)

        // Load Live Farmer Data via ApiClient (Single Connection Point)
        loadingBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val myCropsRes = ApiClient.getProducts(ctx, farmerUid = farmerUid)
            val ordersRes = ApiClient.getOrders(ctx, viewRole = "farmer")
            val demandsRes = ApiClient.getBuyerRequests(ctx)

            if (!isAdded) return@launch
            loadingBar.visibility = View.GONE

            val myCropsArr = myCropsRes.dataArray ?: JSONArray()
            val ordersArr = ordersRes.dataArray ?: JSONArray()
            val demandsArr = demandsRes.dataArray ?: JSONArray()

            statActiveCropsVal.text = myCropsArr.length().toString()
            statOrdersVal.text = ordersArr.length().toString()
            statDemandsVal.text = demandsArr.length().toString()

            // Render Farmer's Own Crops
            if (myCropsArr.length() == 0) {
                myProductsContainer.addView(
                    emptyNotice("आपने अभी कोई फसल सूचीबद्ध नहीं की है। ऊपर 'नई फसल डालें' दबाकर शुरुआत करें।")
                )
            } else {
                val count = minOf(myCropsArr.length(), 5)
                for (i in 0 until count) {
                    val item = myCropsArr.optJSONObject(i) ?: continue
                    myProductsContainer.addView(buildFarmerProductRow(item))
                }
            }

            // Render Open Buyer Requests for Farmer to Respond
            if (demandsArr.length() == 0) {
                buyerDemandsContainer.addView(
                    emptyNotice("अभी किसी ग्राहक की खुली मांग लंबित नहीं है।")
                )
            } else {
                val count = minOf(demandsArr.length(), 5)
                for (i in 0 until count) {
                    val reqObj = demandsArr.optJSONObject(i) ?: continue
                    buyerDemandsContainer.addView(buildBuyerDemandRowForFarmer(reqObj))
                }
            }
        }
    }

    // ========================================================================
    // B. BUYER MODE HOME (merged.txt Sec 11 & 63 — केवल खरीदार के विकल्प)
    // ========================================================================
    private fun buildBuyerModeHome() {
        val ctx = context ?: return

        // 1. Search & Discovery Card
        val searchCard = createCard()
        searchCard.addView(sectionTitle("🔍 आप क्या खरीदना चाहते हैं?"))

        val searchRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val searchInput = EditText(ctx).apply {
            hint = "उदा. देशी गेहूं, लहसुन, कच्ची घानी तेल, गुड़..."
            background = roundedBorder("#FAFAFA", "#B8CBB8", dp(8))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f)
        }
        val btnSearch = Button(ctx).apply {
            text = "खोजें"
            background = roundedBg("#1B5E20", dp(8))
            setTextColor(Color.WHITE)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(46)
            )
            lp.leftMargin = dp(8)
            layoutParams = lp
        }
        searchRow.addView(searchInput)
        searchRow.addView(btnSearch)
        searchCard.addView(searchRow)

        // Quick Discovery Filter Chips (Master Spec Sec 6, 9, 11, 12)
        val filterScroll = HorizontalScrollView(ctx).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(0, dp(10), 0, 0)
        }
        val filterRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val dynamicProductsContainer = createCard()
        val productsSectionHeader = sectionTitle("🌾 किसानों के खेत से सीधे उपलब्ध उत्पाद")
        dynamicProductsContainer.addView(productsSectionHeader)

        fun loadBuyerProducts(
            category: String? = null,
            smallQty: Boolean = false,
            freshToday: Boolean = false,
            districtOnly: String? = null,
            query: String? = null,
            titleLabel: String = "🌾 किसानों के खेत से सीधे उपलब्ध उत्पाद"
        ) {
            productsSectionHeader.text = titleLabel
            // Keep header at index 0, clear old rows
            while (dynamicProductsContainer.childCount > 1) {
                dynamicProductsContainer.removeViewAt(1)
            }
            loadingBar.visibility = View.VISIBLE

            viewLifecycleOwner.lifecycleScope.launch {
                val res = ApiClient.getProducts(
                    context = ctx,
                    category = category,
                    district = districtOnly,
                    smallQuantityOnly = smallQty,
                    freshTodayOnly = freshToday,
                    searchQuery = query
                )
                if (!isAdded) return@launch
                loadingBar.visibility = View.GONE

                val arr = res.dataArray ?: JSONArray()
                if (!res.isSuccess) {
                    dynamicProductsContainer.addView(
                        emptyNotice(res.errorMessage ?: "उत्पाद सूची लोड करने में समस्या आई।")
                    )
                    return@launch
                }
                if (arr.length() == 0) {
                    dynamicProductsContainer.addView(
                        emptyNotice("इस श्रेणी में अभी कोई उत्पाद उपलब्ध नहीं है। आप 'मुझे यह चाहिए' में अपनी मांग पोस्ट कर सकते हैं!")
                    )
                    return@launch
                }

                for (i in 0 until arr.length()) {
                    val prod = arr.optJSONObject(i) ?: continue
                    dynamicProductsContainer.addView(buildBuyerProductCard(prod))
                }
            }
        }

        btnSearch.setOnClickListener {
            val q = searchInput.text?.toString()?.trim() ?: ""
            loadBuyerProducts(query = q, titleLabel = "🔍 खोज परिणाम: ${q.ifBlank { "सभी उत्पाद" }}")
        }

        val buyerDistrict = SessionManager.getUserDistrict(ctx)
        val quickFilters = listOf(
            Triple("🌟 सभी उत्पाद", { loadBuyerProducts(titleLabel = "🌟 सभी उपलब्ध उत्पाद") }, true),
            Triple("📍 मेरे आसपास (${buyerDistrict.ifBlank { "ज़िला" }})", {
                loadBuyerProducts(
                    districtOnly = buyerDistrict.ifBlank { null },
                    titleLabel = "📍 मेरे आसपास के किसानों के उत्पाद"
                )
            }, FeatureManager.isEnabled("nearby_products", ctx)),
            Triple("🥬 आज खेत से ताज़ा", {
                loadBuyerProducts(freshToday = true, titleLabel = "🥬 आज की ताज़ा फसल (Fresh Today)")
            }, FeatureManager.isEnabled("fresh_today", ctx)),
            Triple("⚖️ छोटी मात्रा (1–25 kg)", {
                loadBuyerProducts(smallQty = true, titleLabel = "⚖️ छोटी मात्रा (1–25 kg) बाज़ार")
            }, FeatureManager.isEnabled("small_quantity", ctx)),
            Triple("🌾 देशी अनाज", {
                loadBuyerProducts(category = "grains", titleLabel = "🌾 देशी अनाज")
            }, true),
            Triple("🥬 ताज़ा सब्जियां", {
                loadBuyerProducts(category = "vegetables", titleLabel = "🥬 ताज़ा सब्जियां")
            }, true),
            Triple("🍎 देशी फल", {
                loadBuyerProducts(category = "fruits", titleLabel = "🍎 देशी फल")
            }, true),
            Triple("🥜 दाल व तिलहन", {
                loadBuyerProducts(category = "pulses_oilseeds", titleLabel = "🥜 देशी दाल व तिलहन")
            }, true),
            Triple("🫙 कच्ची घानी तेल", {
                loadBuyerProducts(category = "cold_pressed_oil", titleLabel = "🫙 पारंपरिक कच्ची घानी तेल")
            }, true),
            Triple("🍯 गुड़ व मसाले", {
                loadBuyerProducts(category = "spices_jaggery", titleLabel = "🍯 देशी गुड़ व मसाले")
            }, true),
            Triple("🌱 देशी व उन्नत बीज", {
                loadBuyerProducts(category = "seeds", titleLabel = "🌱 देशी व प्रमाणित बीज")
            }, FeatureManager.isEnabled("seeds_marketplace", ctx)),
            Triple("🏺 दुर्लभ/पारंपरिक किस्में", {
                loadBuyerProducts(category = "traditional", titleLabel = "🏺 दुर्लभ व पारंपरिक किस्में")
            }, true)
        )

        for ((label, action, enabled) in quickFilters) {
            if (!enabled) continue
            val chip = Button(ctx).apply {
                text = label
                isAllCaps = false
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTextColor(Color.parseColor("#1B5E20"))
                background = roundedBorder("#E8F5E9", "#81C784", dp(18))
                setPadding(dp(12), dp(6), dp(12), dp(6))
                minHeight = dp(36)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.rightMargin = dp(8)
                layoutParams = lp
                setOnClickListener { action() }
            }
            filterRow.addView(chip)
        }
        filterScroll.addView(filterRow)
        searchCard.addView(filterScroll)
        rootContentLayout.addView(searchCard)

        // 2. Buyer Reverse Marketplace ("मुझे यह चाहिए") & Live Mandi Shortcut
        val buyerToolsCard = createCard()
        if (FeatureManager.isEnabled("buyer_request", ctx)) {
            buyerToolsCard.addView(
                createActionButton("📢 मनचाही फसल नहीं मिली? 'मुझे यह चाहिए' में मांग पोस्ट करें", "#BF360C") {
                    (activity as? MainActivity)?.openBuyerRequestsScreen()
                }
            )
        }
        if (FeatureManager.isEnabled("mandi_price", ctx)) {
            buyerToolsCard.addView(space(dp(8)))
            buyerToolsCard.addView(
                createActionButton("📈 आज के सरकारी मंडी भाव व 1-2 साल का भाव ट्रेंड देखें", "#0D47A1") {
                    (activity as? MainActivity)?.openMandiPricesScreen()
                }
            )
        }
        if (FeatureManager.isEnabled("farm_box", ctx)) {
            buyerToolsCard.addView(space(dp(8)))
            buyerToolsCard.addView(
                createActionButton("🧺 ग्राम ताज़ा फार्म बॉक्स (Village Fresh Box)", "#2E7D32") {
                    Toast.makeText(ctx, "फार्म बॉक्स सूची लोड हो रही है...", Toast.LENGTH_SHORT).show()
                }
            )
        }
        rootContentLayout.addView(buyerToolsCard)

        // 3. Live Products Container
        rootContentLayout.addView(dynamicProductsContainer)
        loadBuyerProducts()
    }

    private fun buildFarmerProductRow(prod: JSONObject): View {
        val ctx = requireContext()
        val id = prod.optString("id", "")
        val name = prod.optString("name", "फसल")
        val variety = prod.optString("variety", "")
        val priceRupees = prod.optDouble("price_rupees", prod.optDouble("price", 0.0))
        val unit = prod.optString("unit", "kg")
        val stock = prod.optDouble("stock_quantity", 0.0)
        val status = prod.optString("status", "ACTIVE")

        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBorder("#FAFDF9", "#D4E2D4", dp(10))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(8)
            layoutParams = lp

            addView(
                TextView(ctx).apply {
                    text = "$name ${if (variety.isNotBlank()) "($variety)" else ""} • ₹$priceRupees/$unit"
                    setTextColor(Color.parseColor("#124116"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                    setTypeface(typeface, Typeface.BOLD)
                }
            )
            addView(
                TextView(ctx).apply {
                    text = "उपलब्ध स्टॉक: $stock $unit • स्थिति: $status"
                    setTextColor(Color.parseColor("#4A5A4A"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setPadding(0, dp(2), 0, dp(6))
                }
            )

            val btnRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            btnRow.addView(
                Button(ctx).apply {
                    text = "✏️ एडिट / पोस्टर"
                    isAllCaps = false
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    setOnClickListener {
                        (activity as? MainActivity)?.openProductDetailScreen(id)
                    }
                }
            )
            addView(btnRow)
        }
    }

    private fun buildBuyerDemandRowForFarmer(reqObj: JSONObject): View {
        val ctx = requireContext()
        val prodName = reqObj.optString("product_name", "कृषि उत्पाद")
        val qty = reqObj.optDouble("quantity", 0.0)
        val unit = reqObj.optString("unit", "kg")
        val targetRupees = reqObj.optDouble("target_price_rupees", 0.0)
        val dist = reqObj.optString("district", "")
        val buyerName = reqObj.optString("buyer_name", "खरीदार")

        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBorder("#FFF8E1", "#FFE082", dp(10))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(8)
            layoutParams = lp

            addView(
                TextView(ctx).apply {
                    text = "📢 मांग: $prodName ($qty $unit) • बजट: ₹$targetRupees/$unit"
                    setTextColor(Color.parseColor("#BF360C"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    setTypeface(typeface, Typeface.BOLD)
                }
            )
            addView(
                TextView(ctx).apply {
                    text = "ग्राहक: $buyerName ($dist)"
                    setTextColor(Color.parseColor("#4A5A4A"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    setPadding(0, dp(2), 0, dp(6))
                }
            )
            addView(
                Button(ctx).apply {
                    text = "✋ मेरे पास उपलब्ध है — भाव प्रस्ताव भेजें"
                    isAllCaps = false
                    background = roundedBg("#1B5E20", dp(8))
                    setTextColor(Color.WHITE)
                    minHeight = dp(38)
                    setOnClickListener {
                        (activity as? MainActivity)?.openBuyerRequestsScreen()
                    }
                }
            )
        }
    }

    private fun buildBuyerProductCard(prod: JSONObject): View {
        val ctx = requireContext()
        val id = prod.optString("id", "")
        val name = prod.optString("name", "कृषि उत्पाद")
        val variety = prod.optString("variety", "")
        val priceRupees = prod.optDouble("price_rupees", prod.optDouble("price", 0.0))
        val unit = prod.optString("unit", "kg")
        val stock = prod.optDouble("stock_quantity", 0.0)
        val minOrder = prod.optDouble("min_order_quantity", 1.0)
        val farmerName = prod.optString("farmer_name", "किसान")
        val farmerPhone = prod.optString("farmer_phone", "")
        val village = prod.optString("village", "")
        val district = prod.optString("district", "")
        val harvestDate = prod.optString("harvest_date", "")
        val isFreshToday = prod.optBoolean("is_fresh_today", false)
        val verifBadge = formatVerificationBadgeHi(prod.optString("verification_status", "farmer_declared"))
        val shareUrl = prod.optString("share_url", "${ApiClient.SERVER_ORIGIN}/product/$id")

        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBorder("#FFFFFF", "#C8E6C9", dp(12))
            setPadding(dp(14), dp(12), dp(14), dp(12))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(10)
            layoutParams = lp

            // Verification & Freshness Badges Row
            addView(
                TextView(ctx).apply {
                    text = "$verifBadge ${if (isFreshToday) "• 🥬 आज खेत से ताज़ा" else ""} ${if (minOrder <= 25) "• ⚖️ छोटी मात्रा उपलब्ध" else ""}"
                    setTextColor(Color.parseColor("#1B5E20"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    setTypeface(typeface, Typeface.BOLD)
                }
            )

            addView(
                TextView(ctx).apply {
                    text = "$name ${if (variety.isNotBlank()) "($variety)" else ""}"
                    setTextColor(Color.parseColor("#1A241A"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                    setTypeface(typeface, Typeface.BOLD)
                    setPadding(0, dp(4), 0, dp(2))
                }
            )

            addView(
                TextView(ctx).apply {
                    text = "₹$priceRupees / $unit  •  उपलब्ध: $stock $unit (न्यूनतम: $minOrder $unit)"
                    setTextColor(Color.parseColor("#BF360C"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                    setTypeface(typeface, Typeface.BOLD)
                }
            )

            addView(
                TextView(ctx).apply {
                    val locText = listOf(village, district).filter { it.isNotBlank() && it != "null" }.joinToString(", ")
                    text = "👨‍🌾 किसान: $farmerName ${if (locText.isNotBlank()) "($locText)" else ""}" +
                        if (harvestDate.isNotBlank() && harvestDate != "null") " • कटाई: $harvestDate" else ""
                    setTextColor(Color.parseColor("#4A5A4A"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setPadding(0, dp(4), 0, dp(10))
                }
            )

            // Action Buttons: [विवरण व खरीदें] [WhatsApp] [Share]
            val actions = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val btnBuy = Button(ctx).apply {
                text = "🛒 खरीदें / भाव प्रस्ताव"
                isAllCaps = false
                background = roundedBg("#1B5E20", dp(8))
                setTextColor(Color.WHITE)
                minHeight = dp(40)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.3f)
                setOnClickListener {
                    (activity as? MainActivity)?.openProductDetailScreen(id)
                }
            }
            actions.addView(btnBuy)

            if (FeatureManager.isEnabled("whatsapp", ctx) && farmerPhone.length >= 10) {
                val btnWa = Button(ctx).apply {
                    text = "🟢 WhatsApp"
                    isAllCaps = false
                    background = roundedBorder("#E8F5E9", "#2E7D32", dp(8))
                    setTextColor(Color.parseColor("#1B5E20"))
                    minHeight = dp(40)
                    val blp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9f)
                    blp.leftMargin = dp(6)
                    layoutParams = blp
                    setOnClickListener {
                        val waMsg = "नमस्ते $farmerName जी, मैंने 'मेरा किसान' ऐप पर आपकी फसल *$name* (₹$priceRupees/$unit) देखी। मुझे यह खरीदनी है।\n$shareUrl"
                        val url = "https://wa.me/91${farmerPhone.takeLast(10)}?text=${Uri.encode(waMsg)}"
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                }
                actions.addView(btnWa)
            }

            if (FeatureManager.isEnabled("share", ctx)) {
                val btnShare = Button(ctx).apply {
                    text = "📤 शेयर"
                    isAllCaps = false
                    background = roundedBorder("#FFF8E1", "#FFB300", dp(8))
                    setTextColor(Color.parseColor("#BF360C"))
                    minHeight = dp(40)
                    val slp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.7f)
                    slp.leftMargin = dp(6)
                    layoutParams = slp
                    setOnClickListener {
                        val msg = "🌾 *मेरा किसान*\n*$name* — ₹$priceRupees/$unit\n📦 $stock $unit उपलब्ध\n👨‍🌾 किसान: $farmerName ($district)\n$verifBadge\n\nदेखें व खरीदें:\n$shareUrl"
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, msg)
                        }
                        startActivity(Intent.createChooser(sendIntent, "फसल शेयर करें"))
                    }
                }
                actions.addView(btnShare)
            }

            addView(actions)
        }
    }

    private fun formatVerificationBadgeHi(status: String): String {
        return when (status.trim().lowercase()) {
            "verified_farmer", "document_verified" -> "✔ मेरा किसान सत्यापित"
            "organic_certified" -> "🌿 प्रमाणित जैविक (Certified Organic)"
            "lab_tested" -> "🧪 लैब परीक्षित (Lab Tested)"
            else -> "👨‍🌾 किसान स्व-घोषित"
        }
    }

    private fun createStatBox(label: String, valueView: TextView): LinearLayout {
        val ctx = requireContext()
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = roundedBorder("#F8FBF8", "#C8E6C9", dp(10))
            setPadding(dp(8), dp(10), dp(8), dp(10))
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            lp.rightMargin = dp(6)
            layoutParams = lp

            valueView.apply {
                text = "..."
                setTextColor(Color.parseColor("#1B5E20"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
            }
            addView(valueView)
            addView(
                TextView(ctx).apply {
                    text = label
                    setTextColor(Color.parseColor("#4A5A4A"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    gravity = Gravity.CENTER
                }
            )
        }
    }

    private fun createCard(): LinearLayout {
        val ctx = requireContext()
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#FFFFFF", dp(14))
            setPadding(dp(14), dp(14), dp(14), dp(14))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(12)
            layoutParams = lp
        }
    }

    private fun sectionTitle(textStr: String): TextView =
        TextView(requireContext()).apply {
            text = textStr
            setTextColor(Color.parseColor("#124116"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(8))
        }

    private fun createActionButton(title: String, colorHex: String, onClick: () -> Unit): Button =
        Button(requireContext()).apply {
            text = title
            isAllCaps = false
            background = roundedBg(colorHex, dp(10))
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            minHeight = dp(46)
            setOnClickListener { onClick() }
        }

    private fun emptyNotice(msg: String): TextView =
        TextView(requireContext()).apply {
            text = msg
            setTextColor(Color.parseColor("#667766"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(8), 0, dp(4))
        }

    private fun roundedBg(hex: String, radiusPx: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx.toFloat()
            setColor(Color.parseColor(hex))
        }

    private fun roundedBorder(fillHex: String, borderHex: String, radiusPx: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx.toFloat()
            setColor(Color.parseColor(fillHex))
            setStroke(dp(1), Color.parseColor(borderHex))
        }

    private fun space(h: Int): View =
        View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, h)
        }

    private fun dp(v: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()
}
