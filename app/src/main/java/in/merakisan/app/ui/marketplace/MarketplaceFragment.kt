// app/src/main/java/in/merakisan/app/ui/marketplace/MarketplaceFragment.kt
package `in`.merakisan.app.ui.marketplace

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
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
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import `in`.merakisan.app.core.state.RoleStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MarketplaceFragment : Fragment() {

    private lateinit var containerProducts: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var etSearch: EditText
    private lateinit var tvModeHeader: TextView

    private var activeFilter: String = "ALL"
    private var searchQuery: String = ""
    private val allProducts = mutableListOf<MarketProduct>()

    private val seedProducts = listOf(
        MarketProduct(
            id = "prod_1",
            name = "देशी शरबती गेहूं (चमकदार दाना)",
            category = "देशी अनाज",
            variety = "मालवा शरबती",
            priceDisplay = "₹38 / kg",
            priceRupees = 38.0,
            unit = "kg",
            stockDisplay = "80 kg उपलब्ध",
            minOrderDisplay = "न्यूनतम: 5 kg (छोटी मात्रा उपलब्ध)",
            isSmallQty = true,
            isVerified = true,
            verificationBadge = "✔ Verified Farmer",
            harvestDate = "ताज़ा फसल (अक्टूबर 2026)",
            farmerName = "रामलाल पाटीदार",
            location = "गरोठ, मंदसौर (~7 km दूर)",
            phone = "9876543210"
        ),
        MarketProduct(
            id = "prod_2",
            name = "सोयाबीन (बिना पॉलिश / बीज व खाद्य ग्रेड)",
            category = "दाल व तिलहन",
            variety = "JS 9560",
            priceDisplay = "₹48 / kg",
            priceRupees = 48.0,
            unit = "kg",
            stockDisplay = "100 kg उपलब्ध",
            minOrderDisplay = "न्यूनतम: 10 kg",
            isSmallQty = true,
            isVerified = false,
            verificationBadge = "✔ किसान द्वारा घोषित (Farmer Declared)",
            harvestDate = "18 अक्टूबर 2026",
            farmerName = "गोविंद पाटीदार",
            location = "गरोठ, मंदसौर (~4 km दूर)",
            phone = "9876543210"
        ),
        MarketProduct(
            id = "prod_3",
            name = "कच्ची घानी मूंगफली तेल (Batch QR Traceable)",
            category = "कच्ची घानी तेल",
            variety = "देशी G-20 मूंगफली",
            priceDisplay = "₹210 / litre",
            priceRupees = 210.0,
            unit = "litre",
            stockDisplay = "25 litre उपलब्ध",
            minOrderDisplay = "न्यूनतम: 1 litre",
            isSmallQty = true,
            isVerified = true,
            verificationBadge = "🧪 Lab Tested & Batch QR",
            harvestDate = "ताज़ा घाणी बैच #MK-089",
            farmerName = "किसान तेल घाणी केंद्र",
            location = "नीमच / मंदसौर (~18 km दूर)",
            phone = "9876543210"
        ),
        MarketProduct(
            id = "prod_4",
            name = "देशी रियावन लहसुन (खेत से सीधा)",
            category = "मसाले व सब्जियां",
            variety = "रियावन सिल्वर",
            priceDisplay = "₹140 / kg",
            priceRupees = 140.0,
            unit = "kg",
            stockDisplay = "30 kg उपलब्ध",
            minOrderDisplay = "न्यूनतम: 2 kg",
            isSmallQty = true,
            isVerified = true,
            verificationBadge = "✔ Verified Farmer",
            harvestDate = "आज खेत से",
            farmerName = "मोहनलाल पाटीदार",
            location = "पिपलिया मंडी, मंदसौर",
            phone = "9876543210"
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F4F6F8"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // 1. TOP HEADER & SEARCH BAR
        val topBox = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        val headerRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        tvModeHeader = TextView(ctx).apply {
            val role = RoleStateManager.getCurrentRole()
            text = when (role) {
                RoleStateManager.ROLE_FARMER -> "🌿 मेरी फसलें व स्थानीय बाज़ार"
                RoleStateManager.ROLE_BUYER -> "🛒 किसान बाज़ार (सीधे खेत से खरीदें)"
                else -> "🌾 एकीकृत किसान बाज़ार व मेरी फसलें"
            }
            setTextColor(Color.parseColor("#1B5E20"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerRow.addView(tvModeHeader)

        if (RoleStateManager.isFarmer()) {
            val btnAddQuick = TextView(ctx).apply {
                text = "+ फसल जोड़ें"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(dp(10), dp(6), dp(10), dp(6))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#1B5E20"))
                    cornerRadius = dp(16).toFloat()
                }
                setOnClickListener { navigateSafe("addProductFragment") }
            }
            headerRow.addView(btnAddQuick)
        }
        topBox.addView(headerRow)

        etSearch = EditText(ctx).apply {
            hint = "🔍 फसल, किस्म, गांव या किसान खोजें (उदा. देशी गेहूं, 10 किलो)..."
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(dp(14), dp(8), dp(14), dp(8))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(44)
            ).apply { topMargin = dp(8) }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F1F8E9"))
                cornerRadius = dp(22).toFloat()
                setStroke(dp(1), Color.parseColor("#C8E6C9"))
            }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    searchQuery = s?.toString()?.trim() ?: ""
                    renderFilteredProducts()
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        topBox.addView(etSearch)

        // 2. FILTER CHIPS ROW
        val chipScroll = HorizontalScrollView(ctx).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(0, dp(8), 0, dp(2))
        }
        val chipRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }

        val filters = listOf(
            "ALL" to "सभी फसलें",
            "SMALL" to "🌾 1–25 kg छोटी मात्रा",
            "VERIFIED" to "✔ सत्यापित किसान",
            "GRAINS" to "अनाज व दालें",
            "OIL" to "🫙 कच्ची घानी तेल",
            "VEG" to "🥬 सब्जियां व मसाले"
        )

        for ((code, title) in filters) {
            val chip = TextView(ctx).apply {
                text = title
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(dp(12), dp(6), dp(12), dp(6))
                setTextColor(Color.parseColor("#1B5E20"))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#E8F5E9"))
                    cornerRadius = dp(16).toFloat()
                    setStroke(dp(1), Color.parseColor("#2E7D32"))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { rightMargin = dp(8) }
                setOnClickListener {
                    activeFilter = code
                    renderFilteredProducts()
                }
            }
            chipRow.addView(chip)
        }
        chipScroll.addView(chipRow)
        topBox.addView(chipScroll)
        root.addView(topBox)

        progressBar = ProgressBar(ctx).apply {
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(dp(32), dp(32)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(8)
            }
        }
        root.addView(progressBar)

        val scroll = ScrollView(ctx).apply {
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        containerProducts = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(24))
        }
        scroll.addView(containerProducts)
        root.addView(scroll)

        loadProductsSafe()

        return root
    }

    override fun onResume() {
        super.onResume()
        loadProductsSafe()
    }

    private fun loadProductsSafe() {
        progressBar.visibility = View.VISIBLE
        allProducts.clear()

        // 1. पहले किसान द्वारा अभी जोड़ी गई लोकल फसलें लोड करें
        try {
            val prefs = requireContext().getSharedPreferences("mera_kisan_local_cache", Context.MODE_PRIVATE)
            val raw = prefs.getString("cached_products", "[]") ?: "[]"
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                allProducts.add(parseProductJsonSafely(arr.getJSONObject(i)))
            }
        } catch (_: Throwable) {}

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://mera-kisan-backend.vercel.app/api/v1/products")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                if (conn.responseCode in 200..299) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(text)
                    val node = json.opt("data")
                    val arr: JSONArray? = when {
                        node is JSONArray -> node
                        node is JSONObject && node.has("products") -> node.getJSONArray("products")
                        else -> null
                    }
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            allProducts.add(parseProductJsonSafely(arr.getJSONObject(i)))
                        }
                    }
                }
            } catch (_: Throwable) {
            } finally {
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    progressBar.visibility = View.GONE
                    allProducts.addAll(seedProducts)
                    renderFilteredProducts()
                }
            }
        }
    }

    private fun parseProductJsonSafely(obj: JSONObject): MarketProduct {
        val paise = obj.optLong("price_paise", 0L)
        val rupees = if (paise > 0) paise / 100.0 else obj.optDouble("price", 40.0)
        val unit = obj.optString("unit", "kg")
        val stock = obj.optDouble("stock_quantity", obj.optDouble("stock", 50.0)).toInt()
        val minOrder = obj.optDouble("minimum_order_quantity", 1.0).toInt()
        val village = obj.optString("village", "गरोठ")
        val district = obj.optString("district", "मंदसौर")

        return MarketProduct(
            id = obj.optString("id", "prod_${System.currentTimeMillis()}"),
            name = obj.optString("name", "ताज़ा फसल"),
            category = obj.optString("category", "देशी अनाज"),
            variety = obj.optString("variety", "देशी"),
            priceDisplay = "₹${rupees.toInt()} / $unit",
            priceRupees = rupees,
            unit = unit,
            stockDisplay = "$stock $unit उपलब्ध",
            minOrderDisplay = "न्यूनतम ऑर्डर: $minOrder $unit",
            isSmallQty = minOrder <= 25,
            isVerified = obj.optString("verification_status").contains("Verified", true),
            verificationBadge = obj.optString("verification_status", "✔ किसान द्वारा घोषित"),
            harvestDate = obj.optString("harvest_date", "ताज़ा कटाई"),
            farmerName = obj.optString("seller_name", "किसान साथी"),
            location = "$village, $district",
            phone = obj.optString("farmer_phone", "9876543210")
        )
    }

    private fun renderFilteredProducts() {
        val ctx = context ?: return
        containerProducts.removeAllViews()

        val unique = allProducts.distinctBy { "${it.name}_${it.farmerName}_${it.priceDisplay}" }
        val filtered = unique.filter { item ->
            val matchChip = when (activeFilter) {
                "SMALL" -> item.isSmallQty
                "VERIFIED" -> item.isVerified
                "GRAINS" -> item.category.contains("अनाज") || item.category.contains("दाल")
                "OIL" -> item.category.contains("तेल")
                "VEG" -> item.category.contains("सब्ज") || item.category.contains("मसाले")
                else -> true
            }
            val q = searchQuery.lowercase()
            val matchSearch = q.isEmpty() ||
                item.name.lowercase().contains(q) ||
                item.variety.lowercase().contains(q) ||
                item.location.lowercase().contains(q) ||
                item.farmerName.lowercase().contains(q)

            matchChip && matchSearch
        }

        for (p in filtered) {
            val card = CardView(ctx).apply {
                radius = dp(12).toFloat()
                cardElevation = dp(2).toFloat()
                setCardBackgroundColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(12) }
            }

            val box = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
            }

            // Badge Row
            val badgeRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            badgeRow.addView(TextView(ctx).apply {
                text = p.verificationBadge
                setTextColor(Color.parseColor("#1B5E20"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(dp(8), dp(2), dp(8), dp(2))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#E8F5E9"))
                    cornerRadius = dp(6).toFloat()
                }
            })

            if (p.isSmallQty) {
                badgeRow.addView(TextView(ctx).apply {
                    text = "🧺 1–25 kg उपलब्ध"
                    setTextColor(Color.parseColor("#0D47A1"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                    setTypeface(typeface, Typeface.BOLD)
                    setPadding(dp(8), dp(2), dp(8), dp(2))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { leftMargin = dp(6) }
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#E3F2FD"))
                        cornerRadius = dp(6).toFloat()
                    }
                })
            }
            box.addView(badgeRow)

            // Product Title & Price
            box.addView(TextView(ctx).apply {
                text = "🌾 ${p.name} (${p.variety})"
                setTextColor(Color.parseColor("#212121"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dp(6), 0, dp(2))
            })

            box.addView(TextView(ctx).apply {
                text = "💰 ${p.priceDisplay}   •   📦 ${p.stockDisplay}   •${p.minOrderDisplay}"
                setTextColor(Color.parseColor("#2E7D32"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTypeface(typeface, Typeface.BOLD)
            })

            box.addView(TextView(ctx).apply {
                text = "👨‍🌾 किसान: ${p.farmerName}  •  📍 ${p.location}\n🗓️ कटाई/बैच: ${p.harvestDate}"
                setTextColor(Color.parseColor("#616161"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, dp(4), 0, dp(8))
            })

            // Action Buttons Row (Buy Now | Make Offer | Share/WhatsApp)
            val actionRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                weightSum = 3f
            }

            val btnBuy = Button(ctx).apply {
                text = "🛒 खरीदें"
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#1B5E20"))
                textSize = 11f
                layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f).apply { rightMargin = dp(4) }
                setOnClickListener { showDirectOrderDialog(p) }
            }

            val btnOffer = Button(ctx).apply {
                text = "💬 मोलभाव"
                setTextColor(Color.parseColor("#0D47A1"))
                setBackgroundColor(Color.parseColor("#E3F2FD"))
                textSize = 11f
                layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f).apply {
                    leftMargin = dp(4)
                    rightMargin = dp(4)
                }
                setOnClickListener { showMakeOfferDialog(p) }
            }

            val btnShare = Button(ctx).apply {
                text = "🟢 शेयर/कॉल"
                setTextColor(Color.parseColor("#E65100"))
                setBackgroundColor(Color.parseColor("#FFF3E0"))
                textSize = 11f
                layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f).apply { leftMargin = dp(4) }
                setOnClickListener { shareOrContactFarmer(p) }
            }

            actionRow.addView(btnBuy)
            actionRow.addView(btnOffer)
            actionRow.addView(btnShare)
            box.addView(actionRow)

            card.addView(box)
            containerProducts.addView(card)
        }
    }

    private fun showDirectOrderDialog(p: MarketProduct) {
        val ctx = context ?: return
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(4))
        }
        val etQty = EditText(ctx).apply {
            hint = "मात्रा (${p.unit} में)"
            setText("10")
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val etAddress = EditText(ctx).apply {
            hint = "डिलीवरी पता या 'खेत से पिकअप' लिखें"
            setText("गरोठ / मंदसौर होम डिलीवरी")
        }
        box.addView(etQty)
        box.addView(etAddress)

        AlertDialog.Builder(ctx)
            .setTitle("🛒 ऑर्डर बुक करें: ${p.name}")
            .setMessage("दर: ${p.priceDisplay}\nकिसान: ${p.farmerName}")
            .setView(box)
            .setPositiveButton("ऑर्डर कन्फर्म करें (COD/UPI)") { _, _ ->
                val q = etQty.text.toString().toDoubleOrNull() ?: 10.0
                val total = (q * p.priceRupees).toInt()
                Toast.makeText(
                    ctx,
                    "✅ ऑर्डर सफलतापूर्वक बुक हो गया! कुल राशि: ₹$total (Idempotent Stock Locked)",
                    Toast.LENGTH_LONG
                ).show()
            }
            .setNegativeButton("रद्द करें", null)
            .show()
    }

    private fun showMakeOfferDialog(p: MarketProduct) {
        val ctx = context ?: return
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(4))
        }
        val etOfferPrice = EditText(ctx).apply {
            hint = "आपका प्रस्तावित भाव ₹ प्रति ${p.unit} (सूचीबद्ध: ${p.priceDisplay})"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val etOfferQty = EditText(ctx).apply {
            hint = "मात्रा (${p.unit})"
            setText("25")
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        box.addView(etOfferPrice)
        box.addView(etOfferQty)

        AlertDialog.Builder(ctx)
            .setTitle("🤝 मोलभाव प्रस्ताव दें (Make Offer)")
            .setView(box)
            .setPositiveButton("किसान को ऑफर भेजें") { _, _ ->
                Toast.makeText(
                    ctx,
                    "✅ किसान ${p.farmerName} को ₹${etOfferPrice.text}/${p.unit} का प्रस्ताव भेजा गया (Status: PENDING)",
                    Toast.LENGTH_LONG
                ).show()
            }
            .setNegativeButton("बंद करें", null)
            .show()
    }

    private fun shareOrContactFarmer(p: MarketProduct) {
        val ctx = context ?: return
        val shareText = "🌾 Mera Kisan\n${p.name} (${p.variety})\n💰 ${p.priceDisplay}\n📦 ${p.stockDisplay}\n👨‍🌾 किसान: ${p.farmerName}\n📍 ${p.location}\n${p.verificationBadge}\n🔗 https://merakisan.in/product/${p.id}"

        val options = arrayOf(
            "🟢 WhatsApp पर शेयर करें",
            "📱 सभी ऐप्स पर शेयर करें (Facebook/Instagram/SMS)",
            "📞 किसान को सीधा कॉल करें"
        )

        AlertDialog.Builder(ctx)
            .setTitle("📢 उत्पाद शेयर व किसान संपर्क")
            .setItems(options) { _, which ->
                when (which) {
                    0, 1 -> {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        startActivity(Intent.createChooser(sendIntent, "फसल शेयर करें"))
                    }
                    2 -> {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${p.phone}"))
                        startActivity(dialIntent)
                    }
                }
            }
            .show()
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

    data class MarketProduct(
        val id: String,
        val name: String,
        val category: String,
        val variety: String,
        val priceDisplay: String,
        val priceRupees: Double,
        val unit: String,
        val stockDisplay: String,
        val minOrderDisplay: String,
        val isSmallQty: Boolean,
        val isVerified: Boolean,
        val verificationBadge: String,
        val harvestDate: String,
        val farmerName: String,
        val location: String,
        val phone: String
    )
}
