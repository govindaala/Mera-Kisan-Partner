// app/src/main/java/in/merakisan/app/ui/modules/OilMillMarketplaceFragment.kt
package `in`.merakisan.app.ui.modules

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import `in`.merakisan.app.core.config.FeatureManager

/**
 * Master Spec Sec 1, 6, 16, 19 & merged.txt Sec 10, 11, 12, 42, 48:
 * Cold-Pressed Oil Mill Marketplace, Traceable Oil Batches & Custom Seed Processing Order.
 */
class OilMillMarketplaceFragment : Fragment() {

    private lateinit var containerContent: LinearLayout
    private var activeTab = "BUY_OIL" // BUY_OIL | CUSTOM_MILLING | OIL_MILLS

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
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

        // 1. TOP APP BAR
        val appBar = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#1B5E20"))
            setPadding(dp(12), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(58))
        }

        appBar.addView(TextView(ctx).apply {
            text = "←"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(6), dp(2), dp(14), dp(2))
            setOnClickListener {
                try { findNavController().popBackStack() } catch (_: Throwable) {}
            }
        })

        appBar.addView(TextView(ctx).apply {
            text = "🫙 कच्ची घानी तेल व घाणी मिल मार्केटप्लेस"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
        })
        root.addView(appBar)

        // Feature Flag Enforcement Check
        if (!FeatureManager.isFeatureEnabled(ctx, "oil_marketplace")) {
            root.addView(TextView(ctx).apply {
                text = "⚙️ यह मॉड्यूल वर्तमान में एडमिन पैनल द्वारा बंद (OFF) रखा गया है।"
                gravity = Gravity.CENTER
                setPadding(dp(20), dp(40), dp(20), dp(40))
            })
            return root
        }

        // 2. MODULE TABS
        val tabRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 3f
            setBackgroundColor(Color.WHITE)
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        val tabs = listOf(
            "BUY_OIL" to "🫙 शुद्ध तेल खरीदें",
            "CUSTOM_MILLING" to "🥜 बीज से तेल निकलवाएं",
            "OIL_MILLS" to "🏛️ पंजीकृत घाणी मिलें"
        )

        for ((code, title) in tabs) {
            tabRow.addView(Button(ctx).apply {
                text = title
                textSize = 10f
                setTextColor(Color.parseColor("#1B5E20"))
                setBackgroundColor(Color.parseColor("#E8F5E9"))
                layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f).apply {
                    leftMargin = dp(3)
                    rightMargin = dp(3)
                }
                setOnClickListener {
                    activeTab = code
                    renderCurrentTab()
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

        containerContent = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(24))
        }

        scroll.addView(containerContent)
        root.addView(scroll)

        renderCurrentTab()
        return root
    }

    private fun renderCurrentTab() {
        val ctx = context ?: return
        containerContent.removeAllViews()

        when (activeTab) {
            "BUY_OIL" -> renderTraceableOilProducts(ctx)
            "CUSTOM_MILLING" -> renderCustomOilProcessingForm(ctx)
            "OIL_MILLS" -> renderRegisteredMills(ctx)
        }
    }

    private fun renderTraceableOilProducts(ctx: Context) {
        val batches = listOf(
            OilLot(
                batchId = "MK-MDS-2026-089",
                name = "कच्ची घानी मूंगफली तेल (लकड़ी घाणी)",
                price = "₹210 / लीटर (1L, 5L, 15L उपलब्ध)",
                seedSource = "किसान: रामलाल पाटीदार (ग्राम गरोठ, मंदसौर)",
                millName = "गरोठ पारंपरिक लकड़ी घाणी केंद्र",
                extractionDate = "16 अक्टूबर 2026",
                badge = "✔ Batch QR Verified • Farmer Declared"
            ),
            OilLot(
                batchId = "MK-NMC-2026-092",
                name = "काली/पीली सरसों कच्ची घानी तेल",
                price = "₹185 / लीटर (1L, 5L उपलब्ध)",
                seedSource = "किसान: मोहनलाल पाटीदार (मनासा, नीमच)",
                millName = "नीमच शुद्ध कृषि घाणी मिल",
                extractionDate = "17 अक्टूबर 2026",
                badge = "✔ Batch QR Verified • Traditional Press"
            ),
            OilLot(
                batchId = "MK-MDS-2026-095",
                name = "देशी सफेद तिल्ली (Sesame) घाणी तेल",
                price = "₹260 / लीटर (1L, 2L उपलब्ध)",
                seedSource = "किसान: गोविंद पाटीदार (गरोठ, मंदसौर)",
                millName = "गरोठ पारंपरिक लकड़ी घाणी केंद्र",
                extractionDate = "18 अक्टूबर 2026",
                badge = "✔ Batch QR Verified • Cold Pressed"
            )
        )

        for (lot in batches) {
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

            box.addView(TextView(ctx).apply {
                text = "${lot.badge}  •  Batch: #${lot.batchId}"
                setTextColor(Color.parseColor("#1B5E20"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setTypeface(typeface, Typeface.BOLD)
            })

            box.addView(TextView(ctx).apply {
                text = "🫙 ${lot.name}"
                setTextColor(Color.parseColor("#212121"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dp(4), 0, dp(2))
            })

            box.addView(TextView(ctx).apply {
                text = "💰 ${lot.price}\n🌱 बीज स्रोत: ${lot.seedSource}\n🏛️ घाणी मिल: ${lot.millName}\n🗓️ तेल निष्कर्षण तिथि: ${lot.extractionDate}"
                setTextColor(Color.parseColor("#424242"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, dp(2), 0, dp(8))
            })

            val btnRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                weightSum = 2f
            }

            btnRow.addView(Button(ctx).apply {
                text = "🔍 बैच QR कुंडली देखें"
                textSize = 11f
                setTextColor(Color.parseColor("#0D47A1"))
                setBackgroundColor(Color.parseColor("#E3F2FD"))
                layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f).apply { rightMargin = dp(4) }
                setOnClickListener { navigateSafe("batchQrTraceabilityFragment") }
            })

            btnRow.addView(Button(ctx).apply {
                text = "🛒 अभी ऑर्डर करें"
                textSize = 11f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#1B5E20"))
                layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f).apply { leftMargin = dp(4) }
                setOnClickListener {
                    Toast.makeText(ctx, "✅ '${lot.name}' (Batch #${lot.batchId}) ऑर्डर के लिए चुना गया!", Toast.LENGTH_LONG).show()
                    navigateSafe("productDetailFragment")
                }
            })

            box.addView(btnRow)
            card.addView(box)
            containerContent.addView(card)
        }
    }

    private fun renderCustomOilProcessingForm(ctx: Context) {
        val card = CardView(ctx).apply {
            radius = dp(12).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(Color.WHITE)
        }

        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        box.addView(TextView(ctx).apply {
            text = "🥜 'मेरी मूंगफली / सरसों से तेल निकलवाएं' (Custom Batch Oil Order)"
            setTextColor(Color.parseColor("#1B5E20"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
        })

        box.addView(TextView(ctx).apply {
            text = "किसान अपना बीज (50–200 kg) पंजीकृत मिल को भेजें या ग्राहक किसान से बीज खरीदकर सीधे मिल से ताज़ा तेल निकलवाएं।"
            setTextColor(Color.parseColor("#616161"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, dp(4), 0, dp(10))
        })

        val etSeedType = EditText(ctx).apply {
            hint = "बीज का नाम (उदा. देशी मूंगफली दाना / सरसों / तिल्ली)"
            setText("देशी G-20 मूंगफली दाना")
        }
        val etSeedQty = EditText(ctx).apply {
            hint = "बीज की मात्रा (kg में)"
            setText("100")
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val etMillChoice = EditText(ctx).apply {
            hint = "चयनित घाणी मिल"
            setText("गरोठ पारंपरिक लकड़ी घाणी केंद्र")
        }

        box.addView(etSeedType)
        box.addView(etSeedQty)
        box.addView(etMillChoice)

        box.addView(Button(ctx).apply {
            text = "✅ कस्टम तेल प्रोसेसिंग व नया बैच QR बुक करें"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1B5E20"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(46)
            ).apply { topMargin = dp(10) }
            setOnClickListener {
                val qty = etSeedQty.text.toString().toIntOrNull() ?: 100
                val estOil = (qty * 0.42).toInt()
                AlertDialog.Builder(ctx)
                    .setTitle("✅ कस्टम घाणी बैच बुक हो गया!")
                    .setMessage("बीज: ${etSeedType.text} ($qty kg)\nअनुमानित शुद्ध तेल उत्पादन: ~$estOil लीटर + खली (Oil Cake)\nआवंटित Batch QR: #MK-CUSTOM-${System.currentTimeMillis() % 1000}")
                    .setPositiveButton("ठीक है", null)
                    .show()
            }
        })

        card.addView(box)
        containerContent.addView(card)
    }

    private fun renderRegisteredMills(ctx: Context) {
        val mills = listOf(
            "1. गरोठ पारंपरिक लकड़ी घाणी केंद्र — ग्राम गरोठ, मंदसौर (क्षमता: 400 kg/दिन • ✔ Verified Mill)",
            "2. नीमच शुद्ध कृषि घाणी मिल — नीमच मंडी रोड (क्षमता: 600 kg/दिन • ✔ Verified Mill)",
            "3. मालवा कच्छी घानी केंद्र — उज्जैन / इंदौर रोड (क्षमता: 500 kg/दिन • ✔ Verified Mill)"
        )

        for (m in mills) {
            val card = CardView(ctx).apply {
                radius = dp(10).toFloat()
                setCardBackgroundColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(10) }
            }
            card.addView(TextView(ctx).apply {
                text = "🏛️ $m"
                setTextColor(Color.parseColor("#212121"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(dp(14), dp(14), dp(14), dp(14))
            })
            containerContent.addView(card)
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

    data class OilLot(
        val batchId: String,
        val name: String,
        val price: String,
        val seedSource: String,
        val millName: String,
        val extractionDate: String,
        val badge: String
    )
}
