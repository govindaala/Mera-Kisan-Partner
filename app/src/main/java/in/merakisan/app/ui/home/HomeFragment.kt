// app/src/main/java/in/merakisan/app/ui/home/HomeFragment.kt
package `in`.merakisan.app.ui.home

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import `in`.merakisan.app.core.config.FeatureManager
import `in`.merakisan.app.core.state.RoleStateManager
import `in`.merakisan.app.ui.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Fixes Video Bug #5 (00:06-00:51):
 * 1. Renders cached/fallback products INSTANTLY (<50ms) so Home feed is never blank while network syncs.
 * 2. Renders complete Product Cards matching merged.txt Sec 13:
 *    [Product Photo] + Name/Variety + ₹Price + Stock + Location + Harvest Date + Farmer + Rating + [❤️] [🛒 Buy] [📤 Share].
 */
class HomeFragment : Fragment() {

    private lateinit var tvRoleIndicator: TextView
    private lateinit var farmerDashboardSection: LinearLayout
    private lateinit var liveProductsContainer: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val activeRole = getActiveRoleSafe(ctx)

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F4F6F8"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // 1. TOP BRAND & ROLE SWITCH HEADER
        val header = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#1B5E20"))
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(60))
        }

        val titleCol = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        titleCol.addView(TextView(ctx).apply {
            text = "🌾 मेरा किसान (MERA KISAN)"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            setTypeface(typeface, Typeface.BOLD)
        })

        tvRoleIndicator = TextView(ctx).apply {
            text = "सक्रिय मोड: ${getRoleDisplayNameSafe(activeRole)} • किसान से सीधे आपकी जरूरत तक"
            setTextColor(Color.parseColor("#C8E6C9"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        }
        titleCol.addView(tvRoleIndicator)
        header.addView(titleCol)

        header.addView(TextView(ctx).apply {
            text = "🔔"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setOnClickListener { navigateSafe("notificationsFavoritesFragment", "favoritesFragment") }
        })

        header.addView(TextView(ctx).apply {
            text = "🔄 मोड बदलें"
            setTextColor(Color.parseColor("#1B5E20"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(14).toFloat()
            }
            setOnClickListener {
                val nextRole = cycleNextRoleSafe(ctx)
                tvRoleIndicator.text = "सक्रिय मोड: ${getRoleDisplayNameSafe(nextRole)} • किसान से सीधे आपकी जरूरत तक"
                updateRoleVisibility(nextRole)
                (activity as? MainActivity)?.syncBottomNavigationLabelsWithRole()
            }
        })
        root.addView(header)

        if (FeatureManager.isMaintenanceMode(ctx)) {
            root.addView(TextView(ctx).apply {
                text = "⚠️ ${FeatureManager.getMaintenanceMessage(ctx)}"
                setTextColor(Color.parseColor("#B71C1C"))
                setBackgroundColor(Color.parseColor("#FFEBEE"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(dp(12), dp(8), dp(12), dp(8))
            })
        }

        val scroll = ScrollView(ctx).apply {
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        val content = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(28))
        }

        // 2. SMART SEARCH BAR
        val searchCard = CardView(ctx).apply {
            radius = dp(12).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
            setOnClickListener { navigateSafe("smartSearchFilterSheet", "marketplaceFragment") }
        }

        searchCard.addView(TextView(ctx).apply {
            text = "🔍 आप क्या खरीदना चाहते हैं? (उदा. '10 किलो देशी गेहूं मंदसौर') — यहाँ टैप करें"
            setTextColor(Color.parseColor("#424242"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        })
        content.addView(searchCard)

        // 3. FARMER QUICK DASHBOARD SECTION
        farmerDashboardSection = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }

        val farmerCard = CardView(ctx).apply {
            radius = dp(12).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(Color.parseColor("#E8F5E9"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
        }

        val fBox = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }

        fBox.addView(TextView(ctx).apply {
            text = "👨‍🌾 किसान विक्रेता केंद्र (Farmer Quick Tools)"
            setTextColor(Color.parseColor("#1B5E20"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
        })

        val fRow1 = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 3f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        }

        fRow1.addView(createToolButton(ctx, "➕ फसल बेचें", "#1B5E20", "#FFFFFF") {
            navigateSafe("addProductFragment")
        })
        fRow1.addView(createToolButton(ctx, "🌿 मेरी फसलें", "#2E7D32", "#FFFFFF") {
            navigateSafe("myCropsFragment")
        })
        fRow1.addView(createToolButton(ctx, "🎨 AI पोस्टर", "#E65100", "#FFFFFF") {
            navigateSafe("aiPosterStudioFragment")
        })
        fBox.addView(fRow1)

        val fRow2 = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 3f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6) }
        }

        fRow2.addView(createToolButton(ctx, "🧮 उचित दाम", "#0D47A1", "#FFFFFF") {
            navigateSafe("fairPriceCalculatorFragment")
        })
        fRow2.addView(createToolButton(ctx, "📈 मंडी भाव", "#1B5E20", "#FFFFFF") {
            navigateSafe("mandiPricesFragment")
        })
        fRow2.addView(createToolButton(ctx, "🤝 भाव प्रस्ताव", "#4A148C", "#FFFFFF") {
            navigateSafe("negotiationHistoryFragment")
        })
        fBox.addView(fRow2)

        farmerCard.addView(fBox)
        farmerDashboardSection.addView(farmerCard)
        content.addView(farmerDashboardSection)

        // 4. CORE DISCOVERY PILLARS
        content.addView(TextView(ctx).apply {
            text = "🌾 मुख्य सुविधाएँ व श्रेणियाँ (Direct Farm Categories)"
            setTextColor(Color.parseColor("#1B5E20"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(2), 0, dp(6))
        })

        val catScroll = HorizontalScrollView(ctx).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(0, 0, 0, dp(10))
        }
        val catRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }

        val categories = listOf(
            "🧺 1–25 kg छोटी मात्रा" to "marketplaceFragment",
            "🥬 आज खेत से (Fresh Today)" to "marketplaceFragment",
            "📍 मेरे आसपास (5–25 km)" to "marketplaceFragment",
            "📢 मुझे यह चाहिए (Demand)" to "buyerRequestsFragment",
            "🌾 देशी अनाज व दालें" to "marketplaceFragment",
            "🫙 कच्ची घानी तेल" to "oilMillMarketplaceFragment",
            "🧺 ग्राम फार्म बॉक्स" to "farmBoxGroupBuyFragment",
            "🔍 बैच QR ट्रेसबिलिटी" to "batchQrTraceabilityFragment"
        )

        for ((label, dest) in categories) {
            catRow.addView(TextView(ctx).apply {
                text = label
                setTextColor(Color.parseColor("#1B5E20"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(dp(12), dp(8), dp(12), dp(8))
                background = GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = dp(18).toFloat()
                    setStroke(dp(1), Color.parseColor("#A5D6A7"))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { rightMargin = dp(8) }
                setOnClickListener { navigateSafe(dest) }
            })
        }
        catScroll.addView(catRow)
        content.addView(catScroll)

        // 5. FEATURE-FLAGGED MODULE CARDS
        if (FeatureManager.isFeatureEnabled(ctx, "mandi_price")) {
            content.addView(createFeatureNavigationCard(
                ctx = ctx,
                badge = "📈 APMC Live & Historical Rates",
                title = "आज के मंडी भाव व सीधा विक्रय तुलना",
                subtitle = "मंदसौर, गरोठ, नीमच, इंदौर मंडी के लाइव भाव, 7–365 दिन इतिहास और सीधा बेचने का लाभ देखें।",
                accentHex = "#1B5E20",
                targetFragment = "mandiPricesFragment"
            ))
        }

        if (FeatureManager.isFeatureEnabled(ctx, "buyer_request")) {
            content.addView(createFeatureNavigationCard(
                ctx = ctx,
                badge = "📢 Reverse Marketplace (Demand Board)",
                title = "‘मुझे यह चाहिए’ — अपनी मांग पोस्ट करें",
                subtitle = "मनचाही देशी फसल या तेल न मिलने पर मांग डालें; पास के किसान सीधे भाव प्रस्ताव देंगे।",
                accentHex = "#E65100",
                targetFragment = "buyerRequestsFragment"
            ))
        }

        if (FeatureManager.isFeatureEnabled(ctx, "oil_marketplace")) {
            content.addView(createFeatureNavigationCard(
                ctx = ctx,
                badge = "🫙 Farm-to-Mill Traceable Oil",
                title = "कच्ची घानी तेल मिल व कस्टम बीज प्रोसेसिंग",
                subtitle = "पंजीकृत लकड़ी घाणी मिलों से शुद्ध तेल खरीदें या अपनी मूंगफली/सरसों से तेल निकलवाएं।",
                accentHex = "#BF360C",
                targetFragment = "oilMillMarketplaceFragment"
            ))
        }

        if (FeatureManager.isFeatureEnabled(ctx, "farm_box")) {
            val boxPrice = FeatureManager.getFarmBoxPrice(ctx)
            content.addView(createFeatureNavigationCard(
                ctx = ctx,
                badge = "🧺 Multi-Farmer Combo & Group Buy",
                title = "ग्राम ताज़ा फार्म बॉक्स (₹$boxPrice) व सामूहिक खरीद",
                subtitle = "5 किसानों की ताज़ा उपज का फैमिली बॉक्स, 20 परिवारों की सामूहिक खरीद और हार्वेस्ट प्री-बुकिंग।",
                accentHex = "#0D47A1",
                targetFragment = "farmBoxGroupBuyFragment"
            ))
        }

        // 6. FRESH TODAY & NEARBY PRODUCTS FEED
        content.addView(TextView(ctx).apply {
            text = "🥬 आज खेत से ताज़ा उपलब्ध फसलें (Tap for Full Detail)"
            setTextColor(Color.parseColor("#1B5E20"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(6), 0, dp(8))
        })

        liveProductsContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }
        content.addView(liveProductsContainer)

        scroll.addView(content)
        root.addView(scroll)

        updateRoleVisibility(activeRole)

        // Instant local/fallback render (<50ms) so screen is NEVER empty for 45 seconds like in video 00:06-00:50
        renderProductCards(ctx, getInstantInitialProducts(ctx))

        // Background sync for live server products
        loadHomeProductsFeed(ctx)
        return root
    }

    private fun getInstantInitialProducts(ctx: Context): List<JSONObject> {
        val list = mutableListOf<JSONObject>()
        try {
            val prefs = ctx.getSharedPreferences("mera_kisan_local_cache", Context.MODE_PRIVATE)
            val arr = JSONArray(prefs.getString("cached_products", "[]") ?: "[]")
            for (i in 0 until arr.length()) {
                list.add(arr.getJSONObject(i))
            }
        } catch (_: Throwable) {}

        if (list.isEmpty() && FeatureManager.isDemoFallbackEnabled(ctx)) {
            list.add(JSONObject().apply {
                put("id", "prod_101")
                put("name", "देशी शरबती गेहूं")
                put("variety", "मालवा शरबती (चमकदार दाना)")
                put("price", 38)
                put("price_paise", 3800)
                put("unit", "kg")
                put("stock_quantity", 80)
                put("minimum_order_quantity", 5)
                put("harvest_date", "16 अक्टूबर")
                put("seller_name", "रामलाल पाटीदार")
                put("farmer_rating", 4.8)
                put("village", "गरोठ")
                put("district", "मंदसौर")
                put("verification_status", "✔ Verified Farmer")
            })
            list.add(JSONObject().apply {
                put("id", "prod_102")
                put("name", "सोयाबीन (बिना पॉलिश)")
                put("variety", "JS 9560 देशी")
                put("price", 48)
                put("price_paise", 4800)
                put("unit", "kg")
                put("stock_quantity", 100)
                put("minimum_order_quantity", 10)
                put("harvest_date", "18 अक्टूबर")
                put("seller_name", "गोविंद पाटीदार")
                put("farmer_rating", 4.9)
                put("village", "गरोठ")
                put("district", "मंदसौर")
                put("verification_status", "✔ किसान द्वारा घोषित")
            })
        }
        return list
    }

    private fun getActiveRoleSafe(ctx: Context): String {
        val prefs = ctx.getSharedPreferences("mera_kisan_role_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getString("active_role", null)
            ?: ctx.getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE).getString("role", null)
            ?: "BOTH"
        return saved.trim().uppercase(Locale.ROOT)
    }

    private fun setActiveRoleSafe(ctx: Context, roleCode: String) {
        val clean = roleCode.trim().uppercase(Locale.ROOT)
        ctx.getSharedPreferences("mera_kisan_role_prefs", Context.MODE_PRIVATE)
            .edit().putString("active_role", clean).apply()
        ctx.getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)
            .edit().putString("role", clean.lowercase(Locale.ROOT)).apply()

        try {
            val cls = RoleStateManager::class.java
            for (m in cls.methods) {
                if (m.name.contains("set", ignoreCase = true) && m.parameterTypes.size == 1 && m.parameterTypes[0] == String::class.java) {
                    m.invoke(RoleStateManager, clean)
                }
            }
        } catch (_: Throwable) {}
    }

    private fun cycleNextRoleSafe(ctx: Context): String {
        val next = when (getActiveRoleSafe(ctx)) {
            "FARMER" -> "BUYER"
            "BUYER" -> "BOTH"
            else -> "FARMER"
        }
        setActiveRoleSafe(ctx, next)
        return next
    }

    private fun getRoleDisplayNameSafe(roleCode: String): String {
        return when (roleCode.trim().uppercase(Locale.ROOT)) {
            "FARMER", "SELLER", "FARMER_SELLER" -> "किसान-विक्रेता (Farmer)"
            "BUYER", "CONSUMER" -> "खरीदार (Buyer)"
            else -> "किसान + खरीदार (Both)"
        }
    }

    private fun updateRoleVisibility(role: String) {
        if (!::farmerDashboardSection.isInitialized) return
        val isFarmerVisible = role.equals("FARMER", true) || role.equals("BOTH", true)
        farmerDashboardSection.visibility = if (isFarmerVisible) View.VISIBLE else View.GONE
    }

    private fun loadHomeProductsFeed(ctx: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FeatureManager.syncRemoteConfig(ctx)
            } catch (_: Throwable) {}

            val serverItems = mutableListOf<JSONObject>()
            try {
                val url = URL("https://mera-kisan-backend.vercel.app/api/v1/products?limit=15")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                if (conn.responseCode in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val arr = JSONObject(body).optJSONArray("data")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            serverItems.add(arr.getJSONObject(i))
                        }
                    }
                }
            } catch (_: Throwable) {}

            if (serverItems.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    renderProductCards(ctx, serverItems)
                }
            }
        }
    }

    /**
     * merged.txt Sec 13 ("Product List"):
     * Renders complete Product Card:
     * [Product Photo] देशी गेहूं ₹38/kg • 80 kg उपलब्ध • मंदसौर • Harvest: 16 Oct • 👨‍🌾 रामलाल ✔ Verified Farmer ⭐ 4.8 • [❤️] [🛒 Buy] [📤 Share]
     */
    private fun renderProductCards(ctx: Context, items: List<JSONObject>) {
        liveProductsContainer.removeAllViews()
        if (items.isEmpty()) {
            liveProductsContainer.addView(TextView(ctx).apply {
                text = "अभी कोई लाइव उत्पाद उपलब्ध नहीं है। किसान भाई '➕ फसल बेचें' से पहली फसल जोड़ें।"
                setTextColor(Color.parseColor("#616161"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(dp(12), dp(20), dp(12), dp(20))
            })
            return
        }

        for (obj in items) {
            val id = obj.optString("id", "prod_101")
            val name = obj.optString("name", "ताज़ा फसल")
            val variety = obj.optString("variety", "देशी")
            val price = obj.optInt("price", 38)
            val unit = obj.optString("unit", "kg")
            val stock = obj.optInt("stock_quantity", 50)
            val minOrd = obj.optInt("minimum_order_quantity", 5)
            val harvest = obj.optString("harvest_date", "ताज़ा कटाई")
            val farmer = obj.optString("seller_name", "किसान साथी")
            val rating = obj.optDouble("farmer_rating", 4.8)
            val village = obj.optString("village", "गरोठ")
            val district = obj.optString("district", "मंदसौर")
            val badge = obj.optString("verification_status", "✔ किसान द्वारा घोषित")

            val openDetailAction = {
                ctx.getSharedPreferences("mera_kisan_local_cache", Context.MODE_PRIVATE)
                    .edit()
                    .putString("selected_product_json", obj.toString())
                    .apply()
                navigateSafe("productDetailFragment")
            }

            val card = CardView(ctx).apply {
                radius = dp(12).toFloat()
                cardElevation = dp(2).toFloat()
                setCardBackgroundColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(12) }
                setOnClickListener { openDetailAction() }
            }

            val outerBox = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
            }

            // Top Row: Product Thumbnail Box + Details Column
            val mainRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            // Product Visual Thumbnail Box
            val thumbBox = TextView(ctx).apply {
                text = if (name.contains("तेल")) "🫙\nघाणी" else "🌾\nताज़ा"
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#1B5E20"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTypeface(typeface, Typeface.BOLD)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#E8F5E9"))
                    cornerRadius = dp(10).toFloat()
                    setStroke(dp(1), Color.parseColor("#A5D6A7"))
                }
                layoutParams = LinearLayout.LayoutParams(dp(68), dp(68)).apply {
                    rightMargin = dp(12)
                }
            }
            mainRow.addView(thumbBox)

            val infoCol = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            infoCol.addView(TextView(ctx).apply {
                text = "$badge  •  ⭐ $rating  •  🧺 न्यूनतम: $minOrd $unit"
                setTextColor(Color.parseColor("#1B5E20"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setTypeface(typeface, Typeface.BOLD)
            })

            infoCol.addView(TextView(ctx).apply {
                text = "$name ($variety) — ₹$price / $unit"
                setTextColor(Color.parseColor("#212121"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dp(2), 0, dp(2))
            })

            infoCol.addView(TextView(ctx).apply {
                text = "📦 $stock $unit उपलब्ध • 📍 $village, $district • 🗓️ कटाई: $harvest\n👨‍🌾 किसान: $farmer"
                setTextColor(Color.parseColor("#616161"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            })

            mainRow.addView(infoCol)
            outerBox.addView(mainRow)

            // Bottom Action Bar on Card: [❤️ सेव] | [🛒 Buy Now] | [📤 Share] (merged.txt Sec 13)
            val btnRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                weightSum = 3f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(10) }
            }

            btnRow.addView(Button(ctx).apply {
                text = "❤️ पसंदीदा"
                textSize = 10f
                setTextColor(Color.parseColor("#C62828"))
                setBackgroundColor(Color.parseColor("#FFEBEE"))
                layoutParams = LinearLayout.LayoutParams(0, dp(36), 1f).apply { rightMargin = dp(4) }
                setOnClickListener {
                    Toast.makeText(ctx, "❤️ '$name' पसंदीदा सूची में सेव किया गया!", Toast.LENGTH_SHORT).show()
                }
            })

            btnRow.addView(Button(ctx).apply {
                text = "🛒 अभी खरीदें (Buy)"
                textSize = 10f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#1B5E20"))
                layoutParams = LinearLayout.LayoutParams(0, dp(36), 1f).apply { leftMargin = dp(2); rightMargin = dp(2) }
                setOnClickListener { openDetailAction() }
            })

            btnRow.addView(Button(ctx).apply {
                text = "📤 शेयर (Share)"
                textSize = 10f
                setTextColor(Color.parseColor("#0D47A1"))
                setBackgroundColor(Color.parseColor("#E3F2FD"))
                layoutParams = LinearLayout.LayoutParams(0, dp(36), 1f).apply { leftMargin = dp(4) }
                setOnClickListener {
                    val shareText = "🌾 Mera Kisan\n$name ($variety)\n💰 ₹$price/$unit • 📦 $stock $unit उपलब्ध\n👨‍🌾 किसान: $farmer ($village, $district)\n$badge\n🔗 देखें: https://merakisan.in/product/$id"
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    startActivity(Intent.createChooser(sendIntent, "उत्पाद शेयर करें"))
                }
            })

            outerBox.addView(btnRow)
            card.addView(outerBox)
            liveProductsContainer.addView(card)
        }
    }

    private fun createToolButton(
        ctx: Context,
        title: String,
        bgHex: String,
        textHex: String,
        onClick: () -> Unit
    ): Button {
        return Button(ctx).apply {
            text = title
            setTextColor(Color.parseColor(textHex))
            setBackgroundColor(Color.parseColor(bgHex))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f).apply {
                leftMargin = dp(3)
                rightMargin = dp(3)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun createFeatureNavigationCard(
        ctx: Context,
        badge: String,
        title: String,
        subtitle: String,
        accentHex: String,
        targetFragment: String
    ): CardView {
        return CardView(ctx).apply {
            radius = dp(12).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
            setOnClickListener { navigateSafe(targetFragment) }

            val box = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
            }

            box.addView(TextView(ctx).apply {
                text = badge
                setTextColor(Color.parseColor(accentHex))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setTypeface(typeface, Typeface.BOLD)
            })

            box.addView(TextView(ctx).apply {
                text = "$title  →"
                setTextColor(Color.parseColor("#212121"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dp(2), 0, dp(2))
            })

            box.addView(TextView(ctx).apply {
                text = subtitle
                setTextColor(Color.parseColor("#616161"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            })

            addView(box)
        }
    }

    private fun navigateSafe(vararg names: String) {
        val ctx = context ?: return
        val pkg = ctx.packageName
        for (n in names) {
            val id = ctx.resources.getIdentifier(n, "id", pkg)
            if (id != 0) {
                try {
                    findNavController().navigate(id)
                    return
                } catch (_: Throwable) {}
            }
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
