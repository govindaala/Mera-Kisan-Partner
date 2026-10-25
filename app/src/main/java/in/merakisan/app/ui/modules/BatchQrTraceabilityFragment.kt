// app/src/main/java/in/merakisan/app/ui/modules/BatchQrTraceabilityFragment.kt
package `in`.merakisan.app.ui.modules

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.security.SessionManager
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * MERA KISAN — FARM-TO-HOME BATCH QR TRACEABILITY TIMELINE
 * Exact Repository Path: app/src/main/java/in/merakisan/app/ui/modules/BatchQrTraceabilityFragment.kt
 *
 * Implements (Master Spec Sec 5 & merged.txt Sec 3, 10, 13, 34, 43, 44, 49):
 * - Graphical Traceability Timeline:
 *   🌱 Farm / Farmer -> 🌾 Raw Material & Harvest -> ⚙️ Processing Mill -> 🧪 Testing/Verification -> 📦 Packing -> 🏠 Customer
 * - Allows buyers to search/verify any Batch ID (e.g., Cold-Pressed Oil, Jaggery, Desi Grain batch).
 * - Allows farmers/mills to register a new traceable batch via `/modules/oil-batches`.
 */
class BatchQrTraceabilityFragment : Fragment() {

    private var initialBatchId: String? = null

    private lateinit var inputBatchIdSearch: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var timelineContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialBatchId = arguments?.getString("batch_id")
    }

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

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(28))
        }

        val topCard = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#FFFFFF", "#D4E2D4", dp(14))
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        topCard.addView(
            TextView(ctx).apply {
                text = "🔍 बैच QR ट्रेसबिलिटी (Farm-to-Home Traceability)"
                setTextColor(Color.parseColor("#124116"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                setTypeface(typeface, Typeface.BOLD)
            }
        )
        topCard.addView(
            TextView(ctx).apply {
                text = "जानें कि आपका तेल, गुड़ या अनाज किस किसान के खेत से आया, कब कटा और किस मिल में तैयार हुआ:"
                setTextColor(Color.parseColor("#4A5A4A"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(0, dp(4), 0, dp(10))
            }
        )

        inputBatchIdSearch = EditText(ctx).apply {
            hint = "बैच नंबर (Batch ID) लिखें (खाली छोड़ने पर सभी हालिया बैच दिखेंगे)"
            setText(initialBatchId ?: "")
            background = roundedBg("#FAFAFA", "#B8CBB8", dp(8))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            minHeight = dp(44)
        }
        topCard.addView(inputBatchIdSearch)

        val btnRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
        }

        val btnSearchBatch = Button(ctx).apply {
            text = "🔍 बैच इतिहास खोजें"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            background = roundedBg("#1B5E20", "#1B5E20", dp(10))
            minHeight = dp(44)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dp(6)
            }
            setOnClickListener { loadTraceableBatches() }
        }
        btnRow.addView(btnSearchBatch)

        if (SessionManager.isFarmerModeActive(ctx)) {
            val btnNewBatch = Button(ctx).apply {
                text = "➕ नया बैच दर्ज करें"
                isAllCaps = false
                setTextColor(Color.WHITE)
                setTypeface(typeface, Typeface.BOLD)
                background = roundedBg("#BF360C", "#BF360C", dp(10))
                minHeight = dp(44)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    leftMargin = dp(6)
                }
                setOnClickListener { showCreateBatchDialog() }
            }
            btnRow.addView(btnNewBatch)
        }

        topCard.addView(btnRow)
        root.addView(topCard)

        progressBar = ProgressBar(ctx).apply {
            visibility = View.GONE
            setPadding(0, dp(12), 0, dp(12))
        }
        root.addView(progressBar)

        timelineContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        root.addView(timelineContainer)

        scroll.addView(root)
        return scroll
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadTraceableBatches()
    }

    private fun loadTraceableBatches() {
        val ctx = context ?: return
        val batchQuery = inputBatchIdSearch.text?.toString()?.trim()
        progressBar.visibility = View.VISIBLE
        timelineContainer.removeAllViews()

        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.request(
                context = ctx,
                endpointPath = "/modules/oil-batches",
                method = "GET",
                queryParams = mapOf("batch_id" to batchQuery?.ifBlank { null })
            )
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            val arr = res.dataArray ?: JSONArray()
            if (arr.length() == 0) {
                timelineContainer.addView(
                    TextView(ctx).apply {
                        text = "कोई बैच रिकॉर्ड नहीं मिला।"
                        setTextColor(Color.parseColor("#555555"))
                        setPadding(dp(8), dp(18), dp(8), dp(18))
                    }
                )
                return@launch
            }

            for (i in 0 until arr.length()) {
                val batchObj = arr.optJSONObject(i) ?: continue
                timelineContainer.addView(buildBatchTimelineCard(batchObj))
            }
        }
    }

    private fun buildBatchTimelineCard(batch: JSONObject): View {
        val ctx = requireContext()
        val batchId = batch.optString("id", batch.optString("batch_number", "BATCH"))
        val productName = batch.optString("product_name", "कच्ची घानी तेल / देशी उत्पाद")
        val seedSource = batch.optString("seed_source", "देशी मूंगफली / तिलहन")
        val sourceFarmer = batch.optString("source_farmer_name", "स्थानीय किसान")
        val village = batch.optString("village", "")
        val district = batch.optString("district", "")
        val rawQtyKg = batch.optDouble("raw_material_kg", 100.0)
        val outputLitres = batch.optDouble("output_quantity", 40.0)
        val millName = batch.optString("mill_name", "स्थानीय पारंपरिक घाणी")
        val processingDate = batch.optString("processing_date", batch.optString("created_at", "").take(10))
        val packingDate = batch.optString("packaging_date", processingDate)
        val verifStatus = batch.optString("verification_status", "farmer_declared")

        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#FFFFFF", "#C8E6C9", dp(14))
            setPadding(dp(16), dp(14), dp(16), dp(14))
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(10)
            layoutParams = lp
        }

        card.addView(
            TextView(ctx).apply {
                text = "🏷️ बैच ID: #$batchId — $productName"
                setTextColor(Color.parseColor("#1B5E20"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                setTypeface(typeface, Typeface.BOLD)
            }
        )

        val steps = listOf(
            "1. 🌱 खेत व किसान (Farm & Farmer)" to "$sourceFarmer (${if (village.isNotBlank()) "$village, " else ""}$district)",
            "2. 🌾 कच्चा माल (Raw Material)" to "$seedSource — $rawQtyKg kg",
            "3. ⚙️ प्रसंस्करण इकाई (Processing Unit)" to "$millName (तारीख: $processingDate)",
            "4. 🧪 शुद्धता व सत्यापन स्तर (Verification)" to verifStatus,
            "5. 📦 पैकिंग व तैयार उत्पाद (Output)" to "$outputLitres इकाई तैयार (पैकिंग: $packingDate)",
            "6. 🔗 स्थायी QR सत्यापन लिंक" to "${ApiClient.SERVER_ORIGIN}/api/v1/modules/oil-batches?batch_id=$batchId"
        )

        for ((stepTitle, stepDetail) in steps) {
            val stepBox = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                background = roundedBg("#F9FBE7", "#DCEDC8", dp(8))
                setPadding(dp(10), dp(8), dp(10), dp(8))
                val lp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                lp.topMargin = dp(6)
                layoutParams = lp
            }
            stepBox.addView(
                TextView(ctx).apply {
                    text = stepTitle
                    setTextColor(Color.parseColor("#124116"))
                    setTypeface(typeface, Typeface.BOLD)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                }
            )
            stepBox.addView(
                TextView(ctx).apply {
                    text = "↳ $stepDetail"
                    setTextColor(Color.parseColor("#333333"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                }
            )
            card.addView(stepBox)
        }

        return card
    }

    private fun showCreateBatchDialog() {
        val ctx = context ?: return
        val scroll = ScrollView(ctx)
        val form = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(8))
        }

        val prodInput = EditText(ctx).apply { hint = "उत्पाद का नाम (उदा. कच्ची घानी मूंगफली तेल) *" }
        val seedInput = EditText(ctx).apply { hint = "कच्चा माल / बीज स्रोत (उदा. देशी मूंगफली) *" }
        val rawKgInput = EditText(ctx).apply { hint = "कच्चा माल मात्रा (kg में - उदा. 100) *" }
        val outQtyInput = EditText(ctx).apply { hint = "तैयार तेल/उत्पाद मात्रा (Litre/kg में - उदा. 42) *" }
        val millInput = EditText(ctx).apply { hint = "तेल मिल / प्रसंस्करण केंद्र का नाम *" }

        form.addView(prodInput)
        form.addView(seedInput)
        form.addView(rawKgInput)
        form.addView(outQtyInput)
        form.addView(millInput)
        scroll.addView(form)

        AlertDialog.Builder(ctx)
            .setTitle("➕ नया ट्रेस करने योग्य बैच दर्ज करें")
            .setView(scroll)
            .setPositiveButton("बैच सेव करें") { _, _ ->
                val pName = prodInput.text?.toString()?.trim() ?: ""
                val sName = seedInput.text?.toString()?.trim() ?: ""
                val mill = millInput.text?.toString()?.trim() ?: ""
                if (pName.isBlank() || sName.isBlank() || mill.isBlank()) {
                    Toast.makeText(ctx, "कृपया सभी आवश्यक विवरण भरें।", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    val body = JSONObject().apply {
                        put("product_name", pName)
                        put("seed_source", sName)
                        put("raw_material_kg", rawKgInput.text?.toString()?.toDoubleOrNull() ?: 100.0)
                        put("output_quantity", outQtyInput.text?.toString()?.toDoubleOrNull() ?: 40.0)
                        put("mill_name", mill)
                        put("village", SessionManager.getUserVillage(ctx))
                        put("district", SessionManager.getUserDistrict(ctx))
                    }
                    val res = ApiClient.request(ctx, "/modules/oil-batches", method = "POST", bodyJson = body)
                    if (res.isSuccess) {
                        Toast.makeText(ctx, "✅ नया बैच सफलतापूर्वक पंजीकृत हो गया!", Toast.LENGTH_LONG).show()
                        loadTraceableBatches()
                    } else {
                        Toast.makeText(ctx, res.errorMessage ?: "बैच सेव करने में त्रुटि।", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("रद्द करें", null)
            .show()
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
