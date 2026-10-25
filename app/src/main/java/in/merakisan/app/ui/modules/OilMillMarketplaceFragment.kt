// app/src/main/java/in/merakisan/app/ui/modules/OilMillMarketplaceFragment.kt
package `in`.merakisan.app.ui.modules

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.InputType
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
import `in`.merakisan.app.core.config.FeatureManager
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.security.SessionManager
import `in`.merakisan.app.ui.main.MainActivity
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * MERA KISAN — COLD PRESSED OIL & LOCAL OIL MILL MARKETPLACE
 * Exact Repository Path: app/src/main/java/in/merakisan/app/ui/modules/OilMillMarketplaceFragment.kt
 *
 * Implements (Master Spec Sec 1, 5, 19 & merged.txt Sec 10-13, 42-43, 48-49):
 * - Tab 1: Traceable Cold-Pressed / Traditional Ghani Oils listed by farmers & local mills.
 * - Tab 2: Registered Local Oil Mills directory & new Mill registration.
 * - Tab 3: Custom Oil Processing ("मेरी मूंगफली / सरसों / तिल्ली से तेल निकलवाना है").
 * - Direct link to Batch QR Traceability (`openBatchQrTraceabilityScreen`).
 */
class OilMillMarketplaceFragment : Fragment() {

    private var activeTab: String = "OILS" // "OILS", "MILLS", "CUSTOM_PRESS"

    private lateinit var btnTabOils: Button
    private lateinit var btnTabMills: Button
    private lateinit var btnTabCustom: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var contentContainer: LinearLayout

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

        val headerCard = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#FFFFFF", "#D4E2D4", dp(14))
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        headerCard.addView(
            TextView(ctx).apply {
                text = "🫙 पारंपरिक कच्ची घानी तेल व स्थानीय ऑयल मिल बाज़ार"
                setTextColor(Color.parseColor("#124116"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                setTypeface(typeface, Typeface.BOLD)
            }
        )

        headerCard.addView(
            TextView(ctx).apply {
                text = "किसान के खेत के तिलहन (मूंगफली, सरसों, तिल, अलसी) से स्थानीय घाणी में निकला ट्रेस करने योग्य तेल:"
                setTextColor(Color.parseColor("#4A5A4A"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(0, dp(4), 0, dp(10))
            }
        )

        val tabRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        btnTabOils = createTabBtn("🫙 घाणी तेल") {
            activeTab = "OILS"
            updateTabStyles()
            loadActiveTabContent()
        }
        btnTabMills = createTabBtn("⚙️ स्थानीय मिलें") {
            activeTab = "MILLS"
            updateTabStyles()
            loadActiveTabContent()
        }
        btnTabCustom = createTabBtn("🥜 कस्टम पिराई") {
            activeTab = "CUSTOM_PRESS"
            updateTabStyles()
            loadActiveTabContent()
        }

        tabRow.addView(btnTabOils)
        tabRow.addView(btnTabMills)
        tabRow.addView(btnTabCustom)
        headerCard.addView(tabRow)
        root.addView(headerCard)

        progressBar = ProgressBar(ctx).apply {
            visibility = View.GONE
            setPadding(0, dp(12), 0, dp(12))
        }
        root.addView(progressBar)

        contentContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        root.addView(contentContainer)

        scroll.addView(root)
        return scroll
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateTabStyles()
        loadActiveTabContent()
    }

    private fun updateTabStyles() {
        val tabs = listOf(
            btnTabOils to "OILS",
            btnTabMills to "MILLS",
            btnTabCustom to "CUSTOM_PRESS"
        )
        for ((btn, key) in tabs) {
            if (key == activeTab) {
                btn.background = roundedBg("#4E342E", "#4E342E", dp(8))
                btn.setTextColor(Color.WHITE)
            } else {
                btn.background = roundedBg("#FFF8E1", "#FFE082", dp(8))
                btn.setTextColor(Color.parseColor("#4E342E"))
            }
        }
    }

    private fun loadActiveTabContent() {
        when (activeTab) {
            "OILS" -> loadColdPressedOils()
            "MILLS" -> loadRegisteredOilMills()
            "CUSTOM_PRESS" -> renderCustomOilPressingSection()
        }
    }

    private fun loadColdPressedOils() {
        val ctx = context ?: return
        progressBar.visibility = View.VISIBLE
        contentContainer.removeAllViews()

        val topBar = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(8))
        }

        if (SessionManager.isFarmerModeActive(ctx)) {
            topBar.addView(
                createFullButton("➕ नया कच्ची घानी तेल उत्पाद सूचीबद्ध करें", "#1B5E20") {
                    (activity as? MainActivity)?.openAddProductScreen(null)
                }.apply {
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        rightMargin = dp(6)
                    }
                }
            )
        }

        topBar.addView(
            createFullButton("🔍 बैच QR ट्रेसबिलिटी जांचें", "#0D47A1") {
                (activity as? MainActivity)?.openBatchQrTraceabilityScreen(null)
            }.apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
        )
        contentContainer.addView(topBar)

        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.getProducts(ctx, category = "cold_pressed_oil")
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            val arr = res.dataArray ?: JSONArray()
            if (arr.length() == 0) {
                contentContainer.addView(
                    TextView(ctx).apply {
                        text = "अभी इस श्रेणी में कोई कच्ची घानी तेल सूचीबद्ध नहीं है।"
                        setTextColor(Color.parseColor("#555555"))
                        setPadding(dp(8), dp(16), dp(8), dp(16))
                    }
                )
                return@launch
            }

            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                contentContainer.addView(buildOilProductCard(item))
            }
        }
    }

    private fun buildOilProductCard(prod: JSONObject): View {
        val ctx = requireContext()
        val prodId = prod.optString("id", "")
        val name = prod.optString("name", "कच्ची घानी तेल")
        val variety = prod.optString("variety", "")
        val price = prod.optDouble("price_rupees", prod.optDouble("price", 0.0))
        val unit = prod.optString("unit", "litre")
        val stock = prod.optDouble("stock_quantity", 0.0)
        val farmerName = prod.optString("farmer_name", "किसान / घाणी")
        val district = prod.optString("district", "")
        val verif = prod.optString("verification_status", "farmer_declared")
        val batchId = prod.optString("batch_id", "")

        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#FFFFFF", "#FFE082", dp(12))
            setPadding(dp(14), dp(12), dp(14), dp(12))
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(10)
            layoutParams = lp
        }

        card.addView(
            TextView(ctx).apply {
                text = "🫙 $name ${if (variety.isNotBlank() && variety != "null") "($variety)" else ""}"
                setTextColor(Color.parseColor("#3E2723"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                setTypeface(typeface, Typeface.BOLD)
            }
        )
        card.addView(
            TextView(ctx).apply {
                text = "💰 ₹$price / $unit  •  📦 उपलब्ध: $stock $unit"
                setTextColor(Color.parseColor("#BF360C"))
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dp(3), 0, dp(3))
            }
        )
        card.addView(
            TextView(ctx).apply {
                text = "👨‍🌾 उत्पादक: $farmerName ($district) • सत्यापन: $verif"
                setTextColor(Color.parseColor("#4A5A4A"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, 0, 0, dp(8))
            }
        )

        val btnRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        btnRow.addView(
            createFullButton("🛒 खरीदें / विवरण", "#1B5E20") {
                (activity as? MainActivity)?.openProductDetailScreen(prodId)
            }.apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    rightMargin = dp(6)
                }
            }
        )
        btnRow.addView(
            createFullButton("🔍 बैच QR देखें", "#4E342E") {
                (activity as? MainActivity)?.openBatchQrTraceabilityScreen(batchId.ifBlank { null })
            }.apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
        )
        card.addView(btnRow)
        return card
    }

    private fun loadRegisteredOilMills() {
        val ctx = context ?: return
        progressBar.visibility = View.VISIBLE
        contentContainer.removeAllViews()

        contentContainer.addView(
            createFullButton("➕ अपनी स्थानीय घाणी / ऑयल मिल पंजीकृत करें", "#4E342E") {
                showRegisterOilMillDialog()
            }
        )

        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.request(ctx, "/modules/oil-mills", method = "GET")
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            val arr = res.dataArray ?: JSONArray()
            if (arr.length() == 0) {
                contentContainer.addView(
                    TextView(ctx).apply {
                        text = "अभी आपके क्षेत्र में कोई ऑयल मिल सूचीबद्ध नहीं है। ऊपर बटन दबाकर पहली घाणी/मिल जोड़ें।"
                        setTextColor(Color.parseColor("#555555"))
                        setPadding(dp(8), dp(16), dp(8), dp(16))
                    }
                )
                return@launch
            }

            for (i in 0 until arr.length()) {
                val m = arr.optJSONObject(i) ?: continue
                val millName = m.optString("mill_name", "स्थानीय कच्ची घानी")
                val owner = m.optString("owner_name", "")
                val seeds = m.optString("supported_seeds", "मूंगफली, सरसों, तिल्ली")
                val district = m.optString("district", "")
                val village = m.optString("village", "")
                val phone = m.optString("phone", "")
                val ratePerKg = m.optDouble("pressing_charge_per_kg", 15.0)

                val card = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    background = roundedBg("#FFFFFF", "#D4E2D4", dp(12))
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                    val lp = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    lp.topMargin = dp(10)
                    layoutParams = lp
                }

                card.addView(
                    TextView(ctx).apply {
                        text = "⚙️ $millName"
                        setTextColor(Color.parseColor("#124116"))
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                        setTypeface(typeface, Typeface.BOLD)
                    }
                )
                card.addView(
                    TextView(ctx).apply {
                        text = "🥜 पिराई सुविधा: $seeds\n" +
                            "💰 पिराई शुल्क: ₹$ratePerKg/kg • 📍 ${if (village.isNotBlank()) "$village, " else ""}$district" +
                            if (owner.isNotBlank()) " • संचालक: $owner" else ""
                        setTextColor(Color.parseColor("#333333"))
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                        setPadding(0, dp(4), 0, dp(8))
                    }
                )

                if (phone.isNotBlank() && phone != "null") {
                    card.addView(
                        createFullButton("📞 मिल से संपर्क करें ($phone)", "#1B5E20") {
                            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                        }
                    )
                }

                contentContainer.addView(card)
            }
        }
    }

    private fun renderCustomOilPressingSection() {
        val ctx = requireContext()
        contentContainer.removeAllViews()

        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#FFFFFF", "#D4E2D4", dp(14))
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        card.addView(
            TextView(ctx).apply {
                text = "🥜 कस्टम तेल पिराई (“मेरी मूंगफली / सरसों से तेल निकलवाना है”)"
                setTextColor(Color.parseColor("#3E2723"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                setTypeface(typeface, Typeface.BOLD)
            }
        )
        card.addView(
            TextView(ctx).apply {
                text = "किसान अपने तिलहन से तेल निकलवाकर बेच सकता है, या ग्राहक सीधे किसान की मूंगफली खरीदकर पंजीकृत घाणी से ताज़ा तेल निकलवाने का ऑर्डर दे सकता है:"
                setTextColor(Color.parseColor("#4A5A4A"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(0, dp(4), 0, dp(10))
            }
        )

        val seedTypeInput = EditText(ctx).apply {
            hint = "तिलहन का नाम (उदा. देशी मूंगफली, पीली सरसों, तिल्ली) *"
        }
        val rawQtyInput = EditText(ctx).apply {
            hint = "तिलहन की मात्रा (kg में - उदा. 50) *"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val distInput = EditText(ctx).apply {
            hint = "ज़िला / शहर *"
            setText(SessionManager.getUserDistrict(ctx))
        }
        val notesInput = EditText(ctx).apply {
            hint = "निर्देश (उदा. लकड़ी की घाणी में बिना फ़िल्टर तेल + खली वापस चाहिए)"
        }

        card.addView(seedTypeInput)
        card.addView(rawQtyInput)
        card.addView(distInput)
        card.addView(notesInput)

        card.addView(
            createFullButton("📤 कस्टम तेल पिराई अनुरोध दर्ज करें", "#BF360C") {
                val seed = seedTypeInput.text?.toString()?.trim() ?: ""
                val qty = rawQtyInput.text?.toString()?.toDoubleOrNull() ?: 0.0
                val dist = distInput.text?.toString()?.trim() ?: ""
                if (seed.length < 2 || qty <= 0.0 || dist.length < 2) {
                    Toast.makeText(ctx, "कृपया तिलहन का नाम, मात्रा और ज़िला भरें।", Toast.LENGTH_SHORT).show()
                    return@createFullButton
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    val payload = JSONObject().apply {
                        put("product_name", "कस्टम घाणी तेल पिराई: $seed")
                        put("quantity", qty)
                        put("unit", "kg")
                        put("target_price_paise", 2000)
                        put("district", dist)
                        put("notes", notesInput.text?.toString()?.trim() ?: "Custom Oil Pressing Request")
                    }
                    val res = ApiClient.createBuyerRequest(ctx, payload)
                    if (res.isSuccess) {
                        Toast.makeText(
                            ctx,
                            "✅ आपका कस्टम तेल पिराई अनुरोध आसपास की पंजीकृत घाणियों और किसानों को भेज दिया गया है!",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(ctx, res.errorMessage ?: "अनुरोध भेजने में त्रुटि।", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )

        contentContainer.addView(card)
    }

    private fun showRegisterOilMillDialog() {
        val ctx = context ?: return
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(8))
        }
        val millNameInput = EditText(ctx).apply { hint = "घाणी / ऑयल मिल का नाम *" }
        val seedsInput = EditText(ctx).apply {
            hint = "किन तिलहनों की पिराई होती है? *"
            setText("मूंगफली, सरसों, तिल्ली, अलसी")
        }
        val chargeInput = EditText(ctx).apply {
            hint = "पिराई शुल्क (₹ प्रति kg - उदा. 15) *"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val distInput = EditText(ctx).apply {
            hint = "ज़िला *"
            setText(SessionManager.getUserDistrict(ctx))
        }

        layout.addView(millNameInput)
        layout.addView(seedsInput)
        layout.addView(chargeInput)
        layout.addView(distInput)

        AlertDialog.Builder(ctx)
            .setTitle("⚙️ स्थानीय घाणी / ऑयल मिल पंजीकरण")
            .setView(layout)
            .setPositiveButton("पंजीकृत करें") { _, _ ->
                val mName = millNameInput.text?.toString()?.trim() ?: ""
                val dist = distInput.text?.toString()?.trim() ?: ""
                if (mName.length < 2 || dist.length < 2) {
                    Toast.makeText(ctx, "कृपया मिल का नाम और ज़िला लिखें।", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    val body = JSONObject().apply {
                        put("mill_name", mName)
                        put("supported_seeds", seedsInput.text?.toString()?.trim() ?: "")
                        put("pressing_charge_per_kg", chargeInput.text?.toString()?.toDoubleOrNull() ?: 15.0)
                        put("district", dist)
                        put("village", SessionManager.getUserVillage(ctx))
                        put("phone", SessionManager.getSelfDeclaredPhone(ctx))
                    }
                    val res = ApiClient.request(ctx, "/modules/oil-mills", method = "POST", bodyJson = body)
                    if (res.isSuccess) {
                        Toast.makeText(ctx, "✅ ऑयल मिल सफलतापूर्वक पंजीकृत हो गई!", Toast.LENGTH_LONG).show()
                        loadRegisteredOilMills()
                    } else {
                        Toast.makeText(ctx, res.errorMessage ?: "पंजीकरण में समस्या आई।", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("रद्द करें", null)
            .show()
    }

    private fun createTabBtn(label: String, onClick: () -> Unit): Button =
        Button(requireContext()).apply {
            text = label
            isAllCaps = false
            setTypeface(typeface, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            minHeight = dp(40)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(4)
            }
            setOnClickListener { onClick() }
        }

    private fun createFullButton(label: String, bgHex: String, onClick: () -> Unit): Button =
        Button(requireContext()).apply {
            text = label
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            background = roundedBg(bgHex, bgHex, dp(10))
            minHeight = dp(44)
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(6)
            layoutParams = lp
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
