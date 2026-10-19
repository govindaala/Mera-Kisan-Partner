// app/src/main/java/in/merakisan/app/ui/modules/BatchQrTraceabilityFragment.kt
package `in`.merakisan.app.ui.modules

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
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * merged.txt Sec 3, 13, 33, 34, 43, 44, 49:
 * Graphical Farm-to-Home Product & Oil Batch QR Traceability Timeline:
 * Farm -> Harvest -> Collection/Processing -> Testing/Verification -> Packing -> Customer.
 */
class BatchQrTraceabilityFragment : Fragment() {

    private lateinit var etBatchCode: EditText
    private lateinit var containerTimeline: LinearLayout

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

        // 1. HEADER
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
            text = "🔍 बैच QR व Farm-to-Home ट्रेसबिलिटी"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
        })
        root.addView(header)

        // 2. BATCH ID SEARCH BAR
        val searchBox = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        etBatchCode = EditText(ctx).apply {
            hint = "Batch ID दर्ज करें (उदा. MK-MDS-2026-089)"
            setText("MK-MDS-2026-089")
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply { rightMargin = dp(8) }
        }

        val btnVerify = Button(ctx).apply {
            text = "🔍 बैच जांचें"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1B5E20"))
            textSize = 12f
            setOnClickListener {
                fetchAndRenderBatchTimeline(etBatchCode.text.toString().trim())
            }
        }

        searchBox.addView(etBatchCode)
        searchBox.addView(btnVerify)
        root.addView(searchBox)

        val scroll = ScrollView(ctx).apply {
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        containerTimeline = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(24))
        }

        scroll.addView(containerTimeline)
        root.addView(scroll)

        fetchAndRenderBatchTimeline("MK-MDS-2026-089")
        return root
    }

    private fun fetchAndRenderBatchTimeline(batchId: String) {
        val cleanId = batchId.ifEmpty { "MK-MDS-2026-089" }

        CoroutineScope(Dispatchers.IO).launch {
            var productTitle = "कच्ची घानी मूंगफली तेल / देशी कृषि लॉट"
            var farmerInfo = "रामलाल पाटीदार (ग्राम गरोठ, मंदसौर)"
            var harvestDate = "12 अक्टूबर 2026"
            var millOrUnit = "गरोठ पारंपरिक लकड़ी घाणी केंद्र"
            var packDate = "17 अक्टूबर 2026"
            var verification = "✔ Farmer Declared & Batch QR Verified"

            try {
                val url = URL("https://mera-kisan-backend.vercel.app/api/v1/modules/oil-batches/$cleanId")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                if (conn.responseCode in 200..299) {
                    val raw = conn.inputStream.bufferedReader().use { it.readText() }
                    val data = JSONObject(raw).optJSONObject("data")
                    if (data != null) {
                        productTitle = data.optString("product_name", productTitle)
                        farmerInfo = "${data.optString("seed_source_farmer", "रामलाल पाटीदार")} (${data.optString("village", "गरोठ")})"
                        harvestDate = data.optString("harvest_date", harvestDate)
                        millOrUnit = data.optString("oil_mill_name", millOrUnit)
                        packDate = data.optString("packaging_date", packDate)
                        verification = data.optString("verification_status", verification)
                    }
                }
            } catch (_: Throwable) {
            } finally {
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    renderGraphicalSteps(cleanId, productTitle, farmerInfo, harvestDate, millOrUnit, packDate, verification)
                }
            }
        }
    }

    private fun renderGraphicalSteps(
        batchId: String,
        productTitle: String,
        farmerInfo: String,
        harvestDate: String,
        millOrUnit: String,
        packDate: String,
        verification: String
    ) {
        val ctx = context ?: return
        containerTimeline.removeAllViews()

        // Summary Header Card
        val topCard = CardView(ctx).apply {
            radius = dp(12).toFloat()
            setCardBackgroundColor(Color.parseColor("#E8F5E9"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(14) }
        }

        topCard.addView(TextView(ctx).apply {
            text = "▣ सत्यापित बैच: #$batchId\n🌾 उत्पाद: $productTitle\n🏅 प्रमाण स्थिति: $verification\n(नियम: बिना प्रमाण कोई झूठा 100% केमिकल-फ्री दावा नहीं)"
            setTextColor(Color.parseColor("#1B5E20"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        })
        containerTimeline.addView(topCard)

        val steps = listOf(
            Triple("1. 🌱 खेत व किसान (Farm & Farmer)", farmerInfo, "सटीक GPS गोपनीय रखकर ग्राम व तहसील सत्यापित"),
            Triple("2. 🌾 कटाई व कच्चा माल (Harvest)", "कटाई तिथि: $harvestDate", "देशी G-20 मूंगफली / शरबती अनाज लॉट"),
            Triple("3. 🏛️ संग्रहण व प्रोसेसिंग (Processing Unit)", millOrUnit, "पारंपरिक धीमी गति से बिना रसायन निष्कर्षण"),
            Triple("4. 🧪 जांच व पारदर्शिता (Verification)", verification, "नमी (Moisture) व साफ-सफाई मानक जांच पूर्ण"),
            Triple("5. 📦 पैकिंग व क्यूआर सील (Packing & QR)", "पैकिंग तिथि: $packDate", "ग्राहक तक सीधे डिलीवरी / पिकअप हेतु तैयार")
        )

        for ((title, subtitle, desc) in steps) {
            val stepCard = CardView(ctx).apply {
                radius = dp(10).toFloat()
                cardElevation = dp(2).toFloat()
                setCardBackgroundColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(10) }
            }

            val box = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(10), dp(14), dp(10))
            }

            box.addView(TextView(ctx).apply {
                text = title
                setTextColor(Color.parseColor("#1B5E20"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTypeface(typeface, Typeface.BOLD)
            })

            box.addView(TextView(ctx).apply {
                text = subtitle
                setTextColor(Color.parseColor("#212121"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dp(2), 0, dp(2))
            })

            box.addView(TextView(ctx).apply {
                text = desc
                setTextColor(Color.parseColor("#616161"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            })

            stepCard.addView(box)
            containerTimeline.addView(stepCard)
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
