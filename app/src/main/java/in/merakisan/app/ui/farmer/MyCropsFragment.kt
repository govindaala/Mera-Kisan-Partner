// app/src/main/java/in/merakisan/app/ui/farmer/MyCropsFragment.kt
package `in`.merakisan.app.ui.farmer

import android.content.Context
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
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import `in`.merakisan.app.core.config.FeatureManager
import `in`.merakisan.app.data.model.ProductModel
import org.json.JSONArray

/**
 * merged.txt Sec 36 & 86:
 * Farmer Product Management Screen ("मेरी फसलें"):
 * Tabs: Active | Sold | Expired | Draft + Share Analytics + Crop Boost + AI Poster.
 */
class MyCropsFragment : Fragment() {

    private lateinit var containerList: LinearLayout
    private var selectedStatusTab: String = "ACTIVE"
    private val farmerProducts = mutableListOf<ProductModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        loadFarmerCrops(ctx)

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F4F6F8"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // 1. TOP BAR
        val header = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#1B5E20"))
            setPadding(dp(12), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(58))
        }

        header.addView(TextView(ctx).apply {
            text = "←"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(6), dp(2), dp(14), dp(2))
            setOnClickListener {
                try { findNavController().popBackStack() } catch (_: Throwable) {}
            }
        })

        header.addView(TextView(ctx).apply {
            text = "🌿 मेरी सूचीबद्ध फसलें (My Products)"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        header.addView(TextView(ctx).apply {
            text = "➕ नई फसल"
            setTextColor(Color.parseColor("#1B5E20"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(14).toFloat()
            }
            setOnClickListener { navigateSafe("addProductFragment") }
        })
        root.addView(header)

        // 2. SHARE & VIEWS ANALYTICS SUMMARY BANNER (merged.txt Sec 34 & 86)
        val analyticsBanner = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#E8F5E9"))
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }
        analyticsBanner.addView(TextView(ctx).apply {
            text = "📊 शेयर एनालिटिक्स: कुल व्यूज: 820  •  WhatsApp शेयर: 102  •  प्राप्त ऑर्डर्स: 14"
            setTextColor(Color.parseColor("#1B5E20"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(typeface, Typeface.BOLD)
        })
        root.addView(analyticsBanner)

        // 3. STATUS TABS (Active | Sold | Expired | Draft)
        val tabRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 4f
            setBackgroundColor(Color.WHITE)
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        val tabs = listOf(
            "ACTIVE" to "सक्रिय (Active)",
            "SOLD" to "बिक चुकी (Sold)",
            "EXPIRED" to "अवधि समाप्त",
            "DRAFT" to "ड्राफ्ट (Draft)"
        )

        for ((code, label) in tabs) {
            tabRow.addView(Button(ctx).apply {
                text = label
                textSize = 11f
                setTextColor(Color.parseColor("#1B5E20"))
                setBackgroundColor(Color.parseColor("#F1F8E9"))
                layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                    leftMargin = dp(2)
                    rightMargin = dp(2)
                }
                setOnClickListener {
                    selectedStatusTab = code
                    renderCropsByTab()
                }
            })
        }
        root.addView(tabRow)

        val scroll = ScrollView(ctx).apply {
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        containerList = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(24))
        }

        scroll.addView(containerList)
        root.addView(scroll)

        renderCropsByTab()
        return root
    }

    private fun loadFarmerCrops(ctx: Context) {
        farmerProducts.clear()
        try {
            val prefs = ctx.getSharedPreferences("mera_kisan_local_cache", Context.MODE_PRIVATE)
            val arr = JSONArray(prefs.getString("cached_products", "[]") ?: "[]")
            for (i in 0 until arr.length()) {
                farmerProducts.add(ProductModel.fromJson(arr.getJSONObject(i)))
            }
        } catch (_: Throwable) {}

        // Default sample farmer inventory across tabs
        farmerProducts.add(
            ProductModel(
                id = "my_crop_1",
                name = "सोयाबीन (बिना पॉलिश / JS 9560)",
                category = "दाल व तिलहन",
                variety = "JS 9560",
                description = "साफ दाना, सीधे किसान के गोदाम से उपलब्ध।",
                pricePaise = 4800L,
                unit = "kg",
                stockQuantity = 100.0,
                minOrderQuantity = 10.0,
                harvestDate = "18 अक्टूबर 2026",
                verificationStatus = "✔ किसान द्वारा घोषित",
                village = "गरोठ",
                district = "मंदसौर",
                approxDistanceKm = 2,
                farmerUid = "usr_govind_01",
                farmerName = "गोविंद पाटीदार",
                farmerPhone = "9876543210",
                farmerRating = 4.9,
                deliveryOptions = "खेत से पिकअप व लोकल डिलीवरी",
                status = "ACTIVE",
                boosted = true,
                videoStatus = "VERIFIED_YOUTUBE",
                viewsCount = 428,
                sharesCount = 64,
                createdAtDisplay = "आज"
            )
        )
        farmerProducts.add(
            ProductModel(
                id = "my_crop_2",
                name = "देशी शरबती गेहूं (छोटी मात्रा)",
                category = "देशी अनाज",
                variety = "मालवा शरबती",
                description = "परिवारों के लिए 5–25 kg पैक में उपलब्ध।",
                pricePaise = 3800L,
                unit = "kg",
                stockQuantity = 0.0,
                minOrderQuantity = 5.0,
                harvestDate = "10 अक्टूबर 2026",
                verificationStatus = "✔ Verified Farmer",
                village = "गरोठ",
                district = "मंदसौर",
                approxDistanceKm = 2,
                farmerUid = "usr_govind_01",
                farmerName = "गोविंद पाटीदार",
                farmerPhone = "9876543210",
                farmerRating = 4.9,
                deliveryOptions = "लोकल डिलीवरी",
                status = "SOLD",
                boosted = false,
                videoStatus = "NONE",
                viewsCount = 392,
                sharesCount = 38,
                createdAtDisplay = "पिछले सप्ताह"
            )
        )
    }

    private fun renderCropsByTab() {
        val ctx = context ?: return
        containerList.removeAllViews()

        val filtered = farmerProducts.filter { it.status.equals(selectedStatusTab, ignoreCase = true) }
        if (filtered.isEmpty()) {
            containerList.addView(TextView(ctx).apply {
                text = "📦 '$selectedStatusTab' श्रेणी में अभी कोई फसल नहीं है।\nऊपर '➕ नई फसल' बटन दबाकर उत्पाद जोड़ें।"
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#616161"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setPadding(dp(16), dp(40), dp(16), dp(40))
            })
            return
        }

        val boostPrice = FeatureManager.getParamInt(ctx, "boost_price", 99)
        val boostDays = FeatureManager.getParamInt(ctx, "boost_days", 3)

        for (crop in filtered) {
            val card = CardView(ctx).apply {
                radius = dp(10).toFloat()
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

            box.addView(TextView(ctx).apply {
                text = "🌾 ${crop.name} (${crop.variety})  ${if (crop.boosted) "🚀 [Boosted]" else ""}"
                setTextColor(Color.parseColor("#1B5E20"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setTypeface(typeface, Typeface.BOLD)
            })

            box.addView(TextView(ctx).apply {
                text = "💰 ₹${crop.priceRupees.toInt()}/${crop.unit}  •  स्टॉक: ${crop.stockQuantity.toInt()} ${crop.unit}\n" +
                       "👁️ व्यूज: ${crop.viewsCount}  •  🟢 शेयर्स: ${crop.sharesCount}  •  तिथि: ${crop.harvestDate}"
                setTextColor(Color.parseColor("#424242"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, dp(4), 0, dp(8))
            })

            val btnRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                weightSum = 3f
            }

            btnRow.addView(Button(ctx).apply {
                text = "🎨 AI पोस्टर"
                textSize = 10f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#1B5E20"))
                layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply { rightMargin = dp(4) }
                setOnClickListener { navigateSafe("aiPosterStudioFragment") }
            })

            btnRow.addView(Button(ctx).apply {
                text = "🚀 बूस्ट (₹$boostPrice/${boostDays}d)"
                textSize = 10f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#E65100"))
                layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply { leftMargin = dp(4); rightMargin = dp(4) }
                setOnClickListener {
                    Toast.makeText(ctx, "🚀 '${crop.name}' के लिए बूस्ट अनुरोध भेजा गया!", Toast.LENGTH_SHORT).show()
                }
            })

            btnRow.addView(Button(ctx).apply {
                text = "🔍 विवरण देखें"
                textSize = 10f
                setTextColor(Color.parseColor("#0D47A1"))
                setBackgroundColor(Color.parseColor("#E3F2FD"))
                layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply { leftMargin = dp(4) }
                setOnClickListener { navigateSafe("productDetailFragment") }
            })

            box.addView(btnRow)
            card.addView(box)
            containerList.addView(card)
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
