// app/src/main/java/in/merakisan/app/ui/home/HomeFragment.kt
package `in`.merakisan.app.ui.home

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.InputType
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
 * MERA KISAN — ROLE-SEGREGATED HOME SCREEN
 *
 * Implements:
 * - Farmer Mode Home (merged.txt Sec 64): Active Products, Orders count, Buyer Requests feed
 *   with 1-tap "भाव प्रस्ताव भेजें", Add Crop, Crop Boost, Mandi AI Advisor, and Agri-Inputs Pickup.
 * - Buyer Mode Home (merged.txt Sec 63 & Final Spec Sec 11): Smart Search, Nearby, Fresh Today,
 *   Small Quantity (1-25 kg), Desi/Rare Categories, Seeds, Cold Pressed Oil, "मुझे यह चाहिए",
 *   and Farm Box (when enabled via FeatureManager).
 * - Single Server Connection: All network requests go strictly through ApiClient.
 */
class HomeFragment : Fragment() {

    private lateinit var rootContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var dynamicContentLayout: LinearLayout

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

        rootContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(28))
        }

        // 1. Remote Announcement & Home Banner (Master Spec Sec 16 & merged.txt Sec 37, 71)
        val bannerCard = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#E8F5E9", "#A5D6A7", dp(12))
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }

        val bannerTitle = TextView(ctx).apply {
            text = "🌾 ${FeatureManager.getHomeBannerText()}"
            setTextColor(Color.parseColor("#1B5E20"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, Typeface.BOLD)
        }
        bannerCard.addView(bannerTitle)

        val announcement = FeatureManager.getAnnouncementText()
        if (announcement.isNotBlank()) {
            bannerCard.addView(
                TextView(ctx).apply {
                    text = "📢 $announcement"
                    setTextColor(Color.parseColor("#BF360C"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    setPadding(0, dp(4), 0, 0)
                }
            )
        }
        rootContainer.addView(bannerCard)

        progressBar = ProgressBar(ctx).apply {
            visibility = View.GONE
            setPadding(0, dp(12), 0, dp(12))
        }
        rootContainer.addView(progressBar)

        dynamicContentLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }
        rootContainer.addView(dynamicContentLayout)

        scroll.addView(rootContainer)
        return scroll
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadRoleSpecificHome()
    }

    private fun loadRoleSpecificHome() {
        val ctx = context ?: return
        dynamicContentLayout.removeAllViews()

        if (SessionManager.isFarmerModeActive(ctx)) {
            renderFarmerHomeDashboard()
        } else {
            renderBuyerHomeMarketplace()
        }
    }

    // ========================================================================
    // 1. FARMER MODE HOME DASHBOARD (merged.txt Sec 64)
    // ========================================================================

    private fun renderFarmerHomeDashboard() {
        val ctx = context ?: return
        val farmerUid = SessionManager.getUserUid(ctx) ?: ""
        val farmerName = SessionManager.getUserName(ctx).ifBlank { "किसान" }
        val district = SessionManager.getUserDistrict(ctx)
        val verifBadge = formatVerificationBadgeHi(SessionManager.getVerificationStatus(ctx))

        // A. Farmer Welcome & Verification Card
        val welcomeCard = createCardLayout().apply {
            addView(
                TextView(ctx).apply {
                    text = "नमस्ते, $farmerName जी 👨‍🌾"
                    setTextColor(Color.parseColor("#124116"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
                    setTypeface(typeface, Typeface.BOLD)
                }
            )
            addView(
                TextView(ctx).apply {
                    text = "$verifBadge ${if (district.isNotBlank()) "• 📍 $district" else ""}"
                    setTextColor(Color.parseColor("#2E7D32"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setPadding(0, dp(4), 0, dp(10))
                }
            )

            // Primary Action Buttons Row
            val actionRow1 = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            val btnAddCrop = createActionButton("➕ नई फसल / उत्पाद डालें", "#1B5E20") {
                (activity as? MainActivity)?.openAddProductScreen(null)
            }.apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    rightMargin = dp(6)
                }
            }
            val btnMandiAi = createActionButton("📈 लाइव मंडी व AI सलाह", "#0D47A1") {
                (activity as? MainActivity)?.openMandiPricesScreen()
            }.apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    leftMargin = dp(6)
                }
            }
            actionRow1.addView(btnAddCrop)
            actionRow1.addView(btnMandiAi)
            addView(actionRow1)

            val actionRow2 = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(8), 0, 0)
            }
            val btnAgriInputs = createActionButton("🧪 बीज, खाद व दवाई (Pickup)", "#4E342E") {
                (activity as? MainActivity)?.openAgriInputsAndSeedsScreen(null)
            }.apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    rightMargin = dp(6)
                }
            }
            val btnMyOrders = createActionButton("📦 ग्राहकों के ऑर्डर्स", "#BF360C") {
                (activity as? MainActivity)?.openOrdersScreen()
            }.apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    leftMargin = dp(6)
                }
            }
            actionRow2.addView(btnAgriInputs)
            actionRow2.addView(btnMyOrders)
            addView(actionRow2)
        }
        dynamicContentLayout.addView(welcomeCard)

        // B. Live Stats Strip (Active Products | Orders | Buyer Requests)
        val statsRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(12)
            layoutParams = lp
        }
        val tvMyCropsCount = createStatBox(statsRow, "मेरी सक्रिय फसलें", "...")
        val tvMyOrdersCount = createStatBox(statsRow, "प्राप्त ऑर्डर्स", "...")
        val tvOpenRequestsCount = createStatBox(statsRow, "ग्राहक मांगें", "...")
        dynamicContentLayout.addView(statsRow)

        // C. Buyer Demand Board Section ("मुझे यह चाहिए" — Nearby Customer Requests)
        val demandSection = createCardLayout()
        demandSection.addView(
            createSectionTitle("📢 ग्राहकों की ताज़ा मांगें (“मुझे यह चाहिए” डिमांड बोर्ड)")
        )
        demandSection.addView(
            TextView(ctx).apply {
                text = "ग्राहकों ने सीधे ये उत्पाद माँगे हैं। यदि आपके पास उपलब्ध है, तो तुरंत अपना भाव भेजें:"
                setTextColor(Color.parseColor("#4A5A4A"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(0, 0, 0, dp(8))
            }
        )
        val demandListContainer = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        demandSection.addView(demandListContainer)
        dynamicContentLayout.addView(demandSection)

        // D. Farmer's Own Active Products Section
        val myCropsSection = createCardLayout()
        myCropsSection.addView(createSectionTitle("🌾 मेरी सूचीबद्ध फसलें व उत्पाद"))
        val myCropsListContainer = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        myCropsSection.addView(myCropsListContainer)
        dynamicContentLayout.addView(myCropsSection)

        // Fetch Live Farmer Data via ApiClient
        progressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val myProductsRes = ApiClient.getProducts(
                context = ctx,
                farmerUid = farmerUid.ifBlank { null }
            )
            val ordersRes = ApiClient.getOrders(ctx, viewRole = "farmer")
            val requestsRes = ApiClient.getBuyerRequests(ctx, district = null)

            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            val myProductsArr = myProductsRes.dataArray ?: JSONArray()
            val ordersArr = ordersRes.dataArray ?: JSONArray()
            val requestsArr = requestsRes.dataArray ?: JSONArray()

            tvMyCropsCount.text = myProductsArr.length().toString()
            tvMyOrdersCount.text = ordersArr.length().toString()
            tvOpenRequestsCount.text = requestsArr.length().toString()

            renderFarmerDemandBoardItems(demandListContainer, requestsArr)
            renderFarmerOwnCropsItems(myCropsListContainer, myProductsArr)
        }
    }

    private fun renderFarmerDemandBoardItems(container: LinearLayout, requestsArr: JSONArray) {
        val ctx = context ?: return
        container.removeAllViews()

        if (requestsArr.length() == 0) {
            container.addView(
                TextView(ctx).apply {
                    text = "अभी ग्राहकों की कोई खुली मांग नहीं है।"
                    setTextColor(Color.parseColor("#666666"))
                    setPadding(0, dp(8), 0, dp(8))
                }
            )
            return
        }

        val maxShow = minOf(requestsArr.length(), 5)
        for (i in 0 until maxShow) {
            val reqObj = requestsArr.optJSONObject(i) ?: continue
            val reqId = reqObj.optString("id", "")
            val prodName = reqObj.optString("product_name", "कृषि उत्पाद")
            val qty = reqObj.optDouble("quantity", 1.0)
            val unit = reqObj.optString("unit", "kg")
            val targetRupees = reqObj.optDouble("target_price_rupees", 0.0)
            val buyerName = reqObj.optString("buyer_name", "ग्राहक")
            val reqDistrict = reqObj.optString("district", "")
            val notes = reqObj.optString("notes", "")

            val itemBox = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                background = roundedBg("#FFFDE7", "#FFF59D", dp(10))
                setPadding(dp(12), dp(10), dp(12), dp(10))
                val lp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                lp.topMargin = dp(8)
                layoutParams = lp
            }

            itemBox.addView(
                TextView(ctx).apply {
                    text = "📌 मांग: $prodName — $qty $unit चाहिए"
                    setTextColor(Color.parseColor("#1B5E20"))
                    setTypeface(typeface, Typeface.BOLD)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                }
            )
            itemBox.addView(
                TextView(ctx).apply {
                    text = "💰 ग्राहक बजट: ₹$targetRupees/$unit • 👤 $buyerName ($reqDistrict)"
                    setTextColor(Color.parseColor("#333333"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setPadding(0, dp(3), 0, dp(4))
                }
            )
            if (notes.isNotBlank()) {
                itemBox.addView(
                    TextView(ctx).apply {
                        text = "📝 विवरण: $notes"
                        setTextColor(Color.parseColor("#555555"))
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    }
                )
            }

            val respondBtn = createActionButton("✋ मेरे पास उपलब्ध है — अपना भाव भेजें", "#1B5E20") {
                showFarmerRespondToRequestDialog(reqId, prodName, qty, unit)
            }.apply {
                val lp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                lp.topMargin = dp(8)
                layoutParams = lp
            }

            itemBox.addView(respondBtn)
            container.addView(itemBox)
        }
    }

    private fun showFarmerRespondToRequestDialog(
        requestId: String,
        productName: String,
        requestedQty: Double,
        unit: String
    ) {
        val ctx = context ?: return
        val form = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(8))
        }

        val priceInput = EditText(ctx).apply {
            hint = "आपका प्रस्तावित भाव (₹ प्रति $unit)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val qtyInput = EditText(ctx).apply {
            hint = "आपके पास उपलब्ध मात्रा ($unit में)"
            setText(requestedQty.toString())
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val msgInput = EditText(ctx).apply {
            hint = "संदेश (उदा. ताज़ा देशी फसल मेरे खेत पर उपलब्ध है)"
            setText("मेरे पास ताज़ा '$productName' उपलब्ध है।")
        }

        form.addView(priceInput)
        form.addView(qtyInput)
        form.addView(msgInput)

        AlertDialog.Builder(ctx)
            .setTitle("🌾 '$productName' के लिए अपना भाव भेजें")
            .setView(form)
            .setPositiveButton("प्रस्ताव भेजें") { _, _ ->
                val priceVal = priceInput.text?.toString()?.toDoubleOrNull() ?: 0.0
                val availQty = qtyInput.text?.toString()?.toDoubleOrNull() ?: requestedQty
                val msg = msgInput.text?.toString()?.trim() ?: ""

                if (priceVal <= 0) {
                    Toast.makeText(ctx, "कृपया मान्य भाव (₹) दर्ज करें।", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    val res = ApiClient.respondToBuyerRequest(
                        context = ctx,
                        requestId = requestId,
                        offeredPriceRupees = priceVal,
                        availableQty = availQty,
                        message = msg
                    )
                    if (res.isSuccess) {
                        Toast.makeText(ctx, "ग्राहक को आपका भाव प्रस्ताव सफलतापूर्वक भेज दिया गया!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(ctx, res.errorMessage ?: "प्रस्ताव भेजने में समस्या आई।", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("रद्द करें", null)
            .show()
    }

    private fun renderFarmerOwnCropsItems(container: LinearLayout, productsArr: JSONArray) {
        val ctx = context ?: return
        container.removeAllViews()

        if (productsArr.length() == 0) {
            container.addView(
                TextView(ctx).apply {
                    text = "आपने अभी कोई फसल सूचीबद्ध नहीं की है। ऊपर '➕ नई फसल / उत्पाद डालें' दबाकर शुरुआत करें।"
                    setTextColor(Color.parseColor("#555555"))
                    setPadding(0, dp(8), 0, dp(8))
                }
            )
            return
        }

        for (i in 0 until productsArr.length()) {
            val prod = productsArr.optJSONObject(i) ?: continue
            container.addView(buildProductCard(prod, isFarmerOwnerView = true))
        }
    }

    // ========================================================================
    // 2. BUYER MODE HOME MARKETPLACE (merged.txt Sec 63 & Final Spec Sec 11)
    // ========================================================================

    private fun renderBuyerHomeMarketplace() {
        val ctx = context ?: return

        // A. Smart Search & Filter Bar
        val searchCard = createCardLayout()
        searchCard.addView(createSectionTitle("🔍 आप क्या खरीदना चाहते हैं?"))

        val searchRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val searchInput = EditText(ctx).apply {
            hint = "उदा. मुझे 10 किलो देशी गेहूं चाहिए..."
            background = roundedBg("#FAFAFA", "#B8CBB8", dp(8))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dp(8)
            }
        }
        val searchBtn = createActionButton("खोजें", "#1B5E20") {
            val q = searchInput.text?.toString()?.trim() ?: ""
            loadBuyerProductsFeed(searchQuery = q.ifBlank { null })
        }
        searchRow.addView(searchInput)
        searchRow.addView(searchBtn)
        searchCard.addView(searchRow)

        // Quick Filter Chips (मेरे आसपास | आज खेत से ताज़ा | छोटी मात्रा 1-25 kg | बीज | दवाई)
        val filterScroll = HorizontalScrollView(ctx).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(0, dp(10), 0, 0)
        }
        val chipRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }

        val userDist = SessionManager.getUserDistrict(ctx)
        addFilterChip(chipRow, "🌾 सभी उत्पाद") {
            loadBuyerProductsFeed()
        }
        if (userDist.isNotBlank() && FeatureManager.isEnabled("nearby_products", ctx)) {
            addFilterChip(chipRow, "📍 मेरे आसपास ($userDist)") {
                loadBuyerProductsFeed(district = userDist)
            }
        }
        if (FeatureManager.isEnabled("fresh_today", ctx)) {
            addFilterChip(chipRow, "🥬 आज खेत से ताज़ा") {
                loadBuyerProductsFeed(freshTodayOnly = true)
            }
        }
        if (FeatureManager.isEnabled("small_quantity", ctx)) {
            addFilterChip(chipRow, "⚖️ छोटी मात्रा (1–25 kg)") {
                loadBuyerProductsFeed(smallQtyOnly = true)
            }
        }
        addFilterChip(chipRow, "🌱 देशी व उन्नत बीज") {
            loadBuyerProductsFeed(category = "seeds")
        }
        addFilterChip(chipRow, "🫙 कच्ची घानी तेल") {
            loadBuyerProductsFeed(category = "cold_pressed_oil")
        }
        addFilterChip(chipRow, "🍯 गुड़ व मसाले") {
            loadBuyerProductsFeed(category = "spices_jaggery")
        }
        addFilterChip(chipRow, "🌾 देशी अनाज") {
            loadBuyerProductsFeed(category = "grains")
        }
        addFilterChip(chipRow, "🥜 दाल व तिलहन") {
            loadBuyerProductsFeed(category = "pulses_oilseeds")
        }
        addFilterChip(chipRow, "🥕 ताज़ा सब्जियां") {
            loadBuyerProductsFeed(category = "vegetables")
        }
        addFilterChip(chipRow, "🍊 देशी फल") {
            loadBuyerProductsFeed(category = "fruits")
        }

        filterScroll.addView(chipRow)
        searchCard.addView(filterScroll)
        dynamicContentLayout.addView(searchCard)

        // B. Reverse Marketplace CTA ("मुझे यह चाहिए" Demand Board Card)
        if (FeatureManager.isEnabled("buyer_request", ctx)) {
            val reqBanner = createCardLayout().apply {
                background = roundedBg("#FFF8E1", "#FFE082", dp(12))
                addView(
                    TextView(ctx).apply {
                        text = "📢 मनचाहा देशी उत्पाद नहीं मिला? “मुझे यह चाहिए” पोस्ट करें!"
                        setTextColor(Color.parseColor("#BF360C"))
                        setTypeface(typeface, Typeface.BOLD)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                    }
                )
                addView(
                    TextView(ctx).apply {
                        text = "अपनी जरूरत और बजट लिखें — आसपास के किसान सीधे आपको अपना भाव भेजेंगे।"
                        setTextColor(Color.parseColor("#4E342E"))
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                        setPadding(0, dp(4), 0, dp(8))
                    }
                )
                addView(
                    createActionButton("➕ अपनी मांग ('मुझे यह चाहिए') दर्ज करें", "#BF360C") {
                        (activity as? MainActivity)?.openBuyerRequestsScreen()
                    }
                )
            }
            dynamicContentLayout.addView(reqBanner)
        }

        // C. Buyer Products Feed Container
        val feedCard = createCardLayout()
        feedCard.addView(createSectionTitle("🛒 किसानों से सीधे उपलब्ध ताज़ा उत्पाद"))
        val productsFeedContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            tag = "BUYER_PRODUCTS_CONTAINER"
        }
        feedCard.addView(productsFeedContainer)
        dynamicContentLayout.addView(feedCard)

        loadBuyerProductsFeed()
    }

    private fun loadBuyerProductsFeed(
        category: String? = null,
        district: String? = null,
        smallQtyOnly: Boolean = false,
        freshTodayOnly: Boolean = false,
        searchQuery: String? = null
    ) {
        val ctx = context ?: return
        val container = dynamicContentLayout.findViewWithTag<LinearLayout>("BUYER_PRODUCTS_CONTAINER") ?: return
        container.removeAllViews()
        progressBar.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.getProducts(
                context = ctx,
                category = category,
                district = district,
                smallQuantityOnly = smallQtyOnly,
                freshTodayOnly = freshTodayOnly,
                searchQuery = searchQuery
            )
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            val arr = res.dataArray ?: JSONArray()
            if (arr.length() == 0) {
                container.addView(
                    TextView(ctx).apply {
                        text = "इस श्रेणी में अभी कोई उत्पाद उपलब्ध नहीं है। आप ऊपर 'मुझे यह चाहिए' में अपनी मांग डाल सकते हैं।"
                        setTextColor(Color.parseColor("#555555"))
                        setPadding(0, dp(12), 0, dp(12))
                    }
                )
                return@launch
            }

            for (i in 0 until arr.length()) {
                val prod = arr.optJSONObject(i) ?: continue
                container.addView(buildProductCard(prod, isFarmerOwnerView = false))
            }
        }
    }

    // ========================================================================
    // 3. SHARED PRODUCT CARD BUILDER (Evidence-Based Badges & Direct Actions)
    // ========================================================================

    private fun buildProductCard(prod: JSONObject, isFarmerOwnerView: Boolean): View {
        val ctx = requireContext()
        val prodId = prod.optString("id", "")
        val name = prod.optString("name", "कृषि उत्पाद")
        val variety = prod.optString("variety", "")
        val priceRupees = prod.optDouble("price_rupees", prod.optDouble("price", 0.0))
        val unit = prod.optString("unit", "kg")
        val stock = prod.optDouble("stock_quantity", 0.0)
        val minOrder = prod.optDouble("min_order_quantity", 1.0)
        val isSmallQty = prod.optBoolean("is_small_quantity", minOrder <= 25.0)
        val isFreshToday = prod.optBoolean("is_fresh_today", false)
        val farmerName = prod.optString("farmer_name", "किसान")
        val farmerPhone = prod.optString("farmer_phone", "")
        val village = prod.optString("village", "")
        val district = prod.optString("district", "")
        val verifStatus = prod.optString("verification_status", "farmer_declared")
        val shareUrl = prod.optString("share_url", "${ApiClient.SERVER_ORIGIN}/product/$prodId")

        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#FAFAFA", "#D4E2D4", dp(12))
            setPadding(dp(14), dp(12), dp(14), dp(12))
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(10)
            layoutParams = lp
            setOnClickListener {
                if (prodId.isNotBlank()) {
                    (activity as? MainActivity)?.openProductDetailScreen(prodId)
                }
            }
        }

        // Badges Row (Verification + Small Qty + Fresh Today)
        val badgeText = buildString {
            append(formatVerificationBadgeHi(verifStatus))
            if (isSmallQty) append(" • ⚖️ छोटी मात्रा उपलब्ध")
            if (isFreshToday) append(" • 🥬 आज खेत से ताज़ा")
        }
        card.addView(
            TextView(ctx).apply {
                text = badgeText
                setTextColor(Color.parseColor("#1B5E20"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTypeface(typeface, Typeface.BOLD)
            }
        )

        // Product Title & Price
        card.addView(
            TextView(ctx).apply {
                text = "$name ${if (variety.isNotBlank() && variety != "null") "($variety)" else ""}"
                setTextColor(Color.parseColor("#1A241A"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dp(4), 0, dp(2))
            }
        )

        card.addView(
            TextView(ctx).apply {
                text = "💰 भाव: ₹$priceRupees / $unit  |  📦 उपलब्ध: $stock $unit (न्यूनतम: $minOrder $unit)"
                setTextColor(Color.parseColor("#BF360C"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTypeface(typeface, Typeface.BOLD)
            }
        )

        card.addView(
            TextView(ctx).apply {
                text = "👨‍🌾 किसान: $farmerName • 📍 ${if (village.isNotBlank() && village != "null") "$village, " else ""}$district"
                setTextColor(Color.parseColor("#4A5A4A"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(0, dp(4), 0, dp(8))
            }
        )

        // Action Buttons Row
        val btnRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        if (isFarmerOwnerView) {
            val editBtn = createActionButton("✏️ एडिट / स्टॉक", "#0D47A1") {
                (activity as? MainActivity)?.openAddProductScreen(prodId)
            }.apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    rightMargin = dp(6)
                }
            }
            val shareBtn = createActionButton("📤 शेयर व पोस्टर", "#1B5E20") {
                (activity as? MainActivity)?.openProductDetailScreen(prodId)
            }.apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    leftMargin = dp(6)
                }
            }
            btnRow.addView(editBtn)
            btnRow.addView(shareBtn)
        } else {
            val buyBtn = createActionButton("🛒 खरीदें / भाव दें", "#1B5E20") {
                (activity as? MainActivity)?.openProductDetailScreen(prodId)
            }.apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    rightMargin = dp(6)
                }
            }
            btnRow.addView(buyBtn)

            if (FeatureManager.isEnabled("whatsapp", ctx) && farmerPhone.isNotBlank() && farmerPhone != "null") {
                val waBtn = createActionButton("🟢 WhatsApp", "#2E7D32") {
                    openWhatsAppChat(farmerPhone, "नमस्ते $farmerName जी, मैंने 'मेरा किसान' ऐप पर आपकी फसल '$name' (₹$priceRupees/$unit) देखी। लिंक: $shareUrl")
                }.apply {
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        leftMargin = dp(6)
                    }
                }
                btnRow.addView(waBtn)
            }
        }

        card.addView(btnRow)
        return card
    }

    private fun formatVerificationBadgeHi(status: String): String =
        when (status.trim().lowercase()) {
            "verified_farmer", "document_verified" -> "✔ मेरा किसान सत्यापित"
            "organic_certified" -> "🌿 प्रमाणित जैविक (Certified Organic)"
            "lab_tested" -> "🧪 लैब परीक्षित (Lab Tested)"
            else -> "👨‍🌾 किसान स्व-घोषित"
        }

    private fun openWhatsAppChat(phone: String, message: String) {
        val cleanDigits = phone.replace("\\D".toRegex(), "")
        val intlPhone = if (cleanDigits.length == 10) "91$cleanDigits" else cleanDigits
        try {
            val uri = Uri.parse("https://wa.me/$intlPhone?text=${Uri.encode(message)}")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: Exception) {
            Toast.makeText(requireContext(), "WhatsApp खोलने में समस्या आई।", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createCardLayout(): LinearLayout {
        val ctx = requireContext()
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#FFFFFF", "#D4E2D4", dp(14))
            setPadding(dp(16), dp(14), dp(16), dp(14))
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(12)
            layoutParams = lp
        }
    }

    private fun createSectionTitle(title: String): TextView =
        TextView(requireContext()).apply {
            text = title
            setTextColor(Color.parseColor("#124116"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(8))
        }

    private fun createStatBox(parent: LinearLayout, label: String, initialVal: String): TextView {
        val ctx = requireContext()
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = roundedBg("#FFFFFF", "#C8E6C9", dp(12))
            setPadding(dp(10), dp(10), dp(10), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(6)
            }
        }
        val valTv = TextView(ctx).apply {
            text = initialVal
            setTextColor(Color.parseColor("#1B5E20"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTypeface(typeface, Typeface.BOLD)
        }
        val lblTv = TextView(ctx).apply {
            text = label
            setTextColor(Color.parseColor("#4A5A4A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            gravity = Gravity.CENTER
        }
        box.addView(valTv)
        box.addView(lblTv)
        parent.addView(box)
        return valTv
    }

    private fun addFilterChip(parent: LinearLayout, label: String, onClick: () -> Unit) {
        val ctx = requireContext()
        val btn = Button(ctx).apply {
            text = label
            isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(Color.parseColor("#1B5E20"))
            background = roundedBg("#E8F5E9", "#81C784", dp(20))
            setPadding(dp(12), dp(6), dp(12), dp(6))
            minHeight = dp(36)
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.rightMargin = dp(8)
            layoutParams = lp
            setOnClickListener { onClick() }
        }
        parent.addView(btn)
    }

    private fun createActionButton(label: String, bgHex: String, onClick: () -> Unit): Button =
        Button(requireContext()).apply {
            text = label
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            background = roundedBg(bgHex, bgHex, dp(10))
            setPadding(dp(12), dp(8), dp(12), dp(8))
            minHeight = dp(44)
            setOnClickListener { onClick() }
        }

    private fun roundedBg(fillHex: String, strokeHex: String, radiusPx: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx.toFloat()
            setColor(Color.parseColor(fillHex))
            setStroke(dp(1), Color.parseColor(strokeHex))
        }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
}
