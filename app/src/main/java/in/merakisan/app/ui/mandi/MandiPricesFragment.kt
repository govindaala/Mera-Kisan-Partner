// app/src/main/java/in/merakisan/app/ui/mandi/MandiPricesFragment.kt
package `in`.merakisan.app.ui.mandi

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Spinner
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
 * MERA KISAN — ZERO-DEFAULT LIVE GOVERNMENT MANDI, 1-2 YEAR GRAPH,
 * 2-MANDI COMPARISON & AI SELLING ADVISOR SCREEN
 *
 * Rules Enforced:
 * 1. 0% Default Data in Code: States, Districts, Mandis, and Commodities are fetched live
 *    from government source (api.data.gov.in via ApiClient) and the UI waits until data arrives.
 * 2. Step-by-Step Cascading: State -> District -> Mandi & Commodity -> Live Prices.
 * 3. Range Fluctuation Graph: 7d, 30d, 90d, 180d, 1 Year (1y), and 2 Years (2y) interactive Canvas chart.
 * 4. 2-Mandi Comparison: Side-by-side comparison over selected range.
 * 5. AI Selling Advisor: Net profit calculation after transport cost based on farmer location.
 * 6. AI Spelling Matcher: Matches Hindi/Hinglish/misspelled crop or mandi names with government dataset.
 */
class MandiPricesFragment : Fragment() {

    private lateinit var statusBanner: TextView
    private lateinit var progressBar: ProgressBar

    // Step-by-Step Cascading Spinners (0% Default Data)
    private lateinit var spinnerState: Spinner
    private lateinit var spinnerDistrict: Spinner
    private lateinit var spinnerMandi: Spinner
    private lateinit var spinnerCommodity: Spinner

    private lateinit var liveRatesContainer: LinearLayout

    // 1-2 Year Historical Trend Graph Controls
    private lateinit var inputTrendCommodity: EditText
    private lateinit var spinnerTrendRange: Spinner
    private lateinit var trendSummaryText: TextView
    private lateinit var trendCanvasView: MandiTrendCanvasView

    // Two-Mandi Comparison Controls
    private lateinit var inputCompareMandiA: EditText
    private lateinit var inputCompareMandiB: EditText
    private lateinit var inputCompareCommodity: EditText
    private lateinit var spinnerCompareRange: Spinner
    private lateinit var compareVerdictText: TextView

    // AI Location-Based Selling Advisor Controls
    private lateinit var inputAdvisorState: EditText
    private lateinit var inputAdvisorDistrict: EditText
    private lateinit var inputAdvisorCommodity: EditText
    private lateinit var inputAdvisorQtyQuintals: EditText
    private lateinit var aiAdvisorResultText: TextView

    // AI Spelling Correction Box
    private lateinit var inputSpellingQuery: EditText
    private lateinit var spinnerSpellingType: Spinner
    private lateinit var spellingResultText: TextView

    private val discoveredStates = mutableListOf<String>()
    private val discoveredDistricts = mutableListOf<String>()
    private val discoveredMandis = mutableListOf<String>()
    private val discoveredCommodities = mutableListOf<String>()

    private val rangeOptions = listOf(
        "7d" to "7 दिन (1 सप्ताह)",
        "30d" to "30 दिन (1 महीना)",
        "90d" to "90 दिन (3 महीने)",
        "180d" to "6 महीने (180 दिन)",
        "1y" to "1 साल (365 दिन)",
        "2y" to "2 साल (730 दिन)"
    )

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
            setPadding(dp(14), dp(14), dp(14), dp(30))
        }

        // Header
        val header = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#1B5E20", "#124116", dp(14))
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        header.addView(
            TextView(ctx).apply {
                text = "📈 लाइव सरकारी मंडी भाव, 1–2 साल ग्राफ व AI सलाहकार"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                setTypeface(typeface, Typeface.BOLD)
            }
        )
        header.addView(
            TextView(ctx).apply {
                text = "सीधे सरकारी सोर्स (data.gov.in) से लाइव डेटा • राज्य → ज़िला → मंडी → फसल"
                setTextColor(Color.parseColor("#E8F5E9"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(0, dp(4), 0, 0)
            }
        )
        root.addView(header)

        statusBanner = TextView(ctx).apply {
            text = "⏳ सरकारी सर्वर से लाइव राज्यों की सूची मंगाई जा रही है, कृपया प्रतीक्षा करें..."
            setTextColor(Color.parseColor("#1B5E20"))
            setTypeface(typeface, Typeface.BOLD)
            background = roundedBg("#E8F5E9", "#A5D6A7", dp(10))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(10)
            layoutParams = lp
        }
        root.addView(statusBanner)

        progressBar = ProgressBar(ctx).apply {
            visibility = View.VISIBLE
            setPadding(0, dp(8), 0, dp(8))
        }
        root.addView(progressBar)

        // ====================================================================
        // SECTION 1: ZERO-DEFAULT CASCADING DROPDOWNS (State -> District -> Mandi -> Crop)
        // ====================================================================
        val cascadeCard = createCard("1. क्रमबद्ध मंडी व फसल चयन (100% सरकारी सोर्स से लाइव)")

        cascadeCard.addView(createLabel("स्टेप 1 — पहले राज्य चुनें (State):"))
        spinnerState = Spinner(ctx).apply { minimumHeight = dp(46) }
        cascadeCard.addView(spinnerState)

        cascadeCard.addView(createLabel("स्टेप 2 — उस राज्य का ज़िला चुनें (District):"))
        spinnerDistrict = Spinner(ctx).apply {
            minimumHeight = dp(46)
            isEnabled = false
        }
        cascadeCard.addView(spinnerDistrict)

        cascadeCard.addView(createLabel("स्टेप 3 — उस ज़िले की मंडी चुनें (Market):"))
        spinnerMandi = Spinner(ctx).apply {
            minimumHeight = dp(46)
            isEnabled = false
        }
        cascadeCard.addView(spinnerMandi)

        cascadeCard.addView(createLabel("स्टेप 4 — फसल चुनें (Commodity):"))
        spinnerCommodity = Spinner(ctx).apply {
            minimumHeight = dp(46)
            isEnabled = false
        }
        cascadeCard.addView(spinnerCommodity)

        val btnRowCascade = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
        }
        val btnReloadGovStates = createButton("🔄 राज्य री-लोड करें", "#0D47A1") {
            loadStatesFromGovernmentSource(forceRefresh = true)
        }.apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dp(6)
            }
        }
        val btnFetchRates = createButton("🌾 ताज़ा भाव देखें", "#1B5E20") {
            fetchFilteredMandiRates()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(6)
            }
        }
        btnRowCascade.addView(btnReloadGovStates)
        btnRowCascade.addView(btnFetchRates)
        cascadeCard.addView(btnRowCascade)

        liveRatesContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, 0)
        }
        cascadeCard.addView(liveRatesContainer)
        root.addView(cascadeCard)

        // ====================================================================
        // SECTION 2: 1 TO 2 YEAR HISTORICAL FLUCTUATION & INTERACTIVE GRAPH
        // ====================================================================
        val graphCard = createCard("2. रेंज अनुसार फसल भाव उतार-चढ़ाव ग्राफ (7 दिन से 2 साल तक)")
        graphCard.addView(createLabel("फसल (Commodity):"))
        inputTrendCommodity = createInput("ऊपर से फसल चुनें या नाम लिखें...")
        graphCard.addView(inputTrendCommodity)

        graphCard.addView(createLabel("अवधि रेंज चुनें (1 साल / 2 साल आदि):"))
        spinnerTrendRange = Spinner(ctx).apply {
            minimumHeight = dp(46)
            adapter = ArrayAdapter(
                ctx,
                android.R.layout.simple_spinner_dropdown_item,
                rangeOptions.map { it.second }
            )
            setSelection(4) // Default "1 साल (365 दिन)"
        }
        graphCard.addView(spinnerTrendRange)

        graphCard.addView(
            createButton("📊 उतार-चढ़ाव ग्राफ व विश्लेषण देखें", "#1B5E20") {
                loadHistoricalTrendGraph()
            }
        )

        trendSummaryText = TextView(ctx).apply {
            visibility = View.GONE
            setTextColor(Color.parseColor("#124116"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(10), 0, dp(6))
        }
        graphCard.addView(trendSummaryText)

        trendCanvasView = MandiTrendCanvasView(ctx).apply {
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(210)
            ).apply { topMargin = dp(6) }
        }
        graphCard.addView(trendCanvasView)
        root.addView(graphCard)

        // ====================================================================
        // SECTION 3: TWO-MANDI COMPARISON OVER A RANGE (Mandi A vs Mandi B)
        // ====================================================================
        val compareCard = createCard("3. दो मंडियों के भाव की रेंज अनुसार तुलना (Mandi A बनाम Mandi B)")
        compareCard.addView(createLabel("पहली मंडी (Mandi A):"))
        inputCompareMandiA = createInput("पहली मंडी का नाम...")
        compareCard.addView(inputCompareMandiA)

        compareCard.addView(createLabel("दूसरी मंडी (Mandi B):"))
        inputCompareMandiB = createInput("दूसरी मंडी का नाम...")
        compareCard.addView(inputCompareMandiB)

        compareCard.addView(createLabel("फसल का नाम (Commodity):"))
        inputCompareCommodity = createInput("फसल का नाम...")
        compareCard.addView(inputCompareCommodity)

        compareCard.addView(createLabel("तुलना की रेंज:"))
        spinnerCompareRange = Spinner(ctx).apply {
            minimumHeight = dp(46)
            adapter = ArrayAdapter(
                ctx,
                android.R.layout.simple_spinner_dropdown_item,
                rangeOptions.map { it.second }
            )
            setSelection(4)
        }
        compareCard.addView(spinnerCompareRange)

        compareCard.addView(
            createButton("⚖️ दोनों मंडियों की तुलना करें", "#0D47A1") {
                runTwoMandiComparison()
            }
        )

        compareVerdictText = TextView(ctx).apply {
            visibility = View.GONE
            background = roundedBg("#E3F2FD", "#90CAF9", dp(10))
            setTextColor(Color.parseColor("#0D47A1"))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(10)
            layoutParams = lp
        }
        compareCard.addView(compareVerdictText)
        root.addView(compareCard)

        // ====================================================================
        // SECTION 4: AI LOCATION-BASED SELLING ADVISOR
        // ====================================================================
        val aiCard = createCard("4. 🤖 AI मंडी विक्रय सलाहकार (आपकी लोकेशन व भाड़े के अनुसार)")
        aiCard.addView(createLabel("आपका राज्य:"))
        inputAdvisorState = createInput("राज्य...").apply {
            setText(SessionManager.getUserState(ctx))
        }
        aiCard.addView(inputAdvisorState)

        aiCard.addView(createLabel("आपका ज़िला:"))
        inputAdvisorDistrict = createInput("आपका ज़िला...").apply {
            setText(SessionManager.getUserDistrict(ctx))
        }
        aiCard.addView(inputAdvisorDistrict)

        aiCard.addView(createLabel("बेचने वाली फसल:"))
        inputAdvisorCommodity = createInput("फसल का नाम...")
        aiCard.addView(inputAdvisorCommodity)

        aiCard.addView(createLabel("कुल मात्रा (क्विंटल में):"))
        inputAdvisorQtyQuintals = createInput("उदा. 15", isNumber = true).apply {
            setText("15")
        }
        aiCard.addView(inputAdvisorQtyQuintals)

        aiCard.addView(
            createButton("🤖 AI से जानें: किस मंडी में बेचना सबसे फायदेमंद है?", "#BF360C") {
                fetchAiLocationSellingAdvice()
            }
        )

        aiAdvisorResultText = TextView(ctx).apply {
            visibility = View.GONE
            background = roundedBg("#FFF8E1", "#FFB300", dp(10))
            setTextColor(Color.parseColor("#3E2723"))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(10)
            layoutParams = lp
        }
        aiCard.addView(aiAdvisorResultText)
        root.addView(aiCard)

        // ====================================================================
        // SECTION 5: AI SPELLING CORRECTION & DATASET MATCHER
        // ====================================================================
        val spellCard = createCard("5. 🔤 AI स्पेलिंग सुधारक व सरकारी डेटा सेंटर मैचर")
        spellCard.addView(createLabel("गलत स्पेलिंग या हिंदी नाम लिखें (फसल / मंडी / ज़िला):"))
        inputSpellingQuery = createInput("उदा. सोयाबिन, लसून, धनीया...")
        spellCard.addView(inputSpellingQuery)

        spinnerSpellingType = Spinner(ctx).apply {
            minimumHeight = dp(46)
            adapter = ArrayAdapter(
                ctx,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("commodity (फसल)", "mandi (मंडी)", "district (ज़िला)", "state (राज्य)")
            )
        }
        spellCard.addView(spinnerSpellingType)

        spellCard.addView(
            createButton("🔤 सरकारी स्पेलिंग से मैच करें", "#2E7D32") {
                runSpellingMatcher()
            }
        )

        spellingResultText = TextView(ctx).apply {
            visibility = View.GONE
            setTextColor(Color.parseColor("#1B5E20"))
            setPadding(0, dp(8), 0, 0)
        }
        spellCard.addView(spellingResultText)
        root.addView(spellCard)

        scroll.addView(root)
        return scroll
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadStatesFromGovernmentSource(forceRefresh = false)
    }

    // ========================================================================
    // STEP 1: LOAD LIVE STATES FROM GOVERNMENT SOURCE (Wait until response)
    // ========================================================================
    private fun loadStatesFromGovernmentSource(forceRefresh: Boolean) {
        val ctx = context ?: return
        progressBar.visibility = View.VISIBLE
        statusBanner.text = "⏳ सरकारी सर्वर (api.data.gov.in) से लाइव राज्यों की सूची आ रही है, कृपया प्रतीक्षा करें..."

        spinnerState.adapter = ArrayAdapter(
            ctx,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("⏳ सरकारी सर्वर से राज्य लोड हो रहे हैं...")
        )
        spinnerDistrict.isEnabled = false
        spinnerMandi.isEnabled = false
        spinnerCommodity.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.getMandiStates(ctx, forceRefresh = forceRefresh)
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            val statesArr = res.dataObject?.optJSONArray("states")
            if (!res.isSuccess || statesArr == null || statesArr.length() == 0) {
                statusBanner.text = "⚠️ सरकारी सर्वर से राज्य लोड करने में देरी: ${res.errorMessage ?: "कृपया 'राज्य री-लोड करें' दबाएँ"}"
                return@launch
            }

            discoveredStates.clear()
            val displayLabels = mutableListOf("-- 1. अपना राज्य चुनें (${statesArr.length()} राज्य लाइव) --")
            for (i in 0 until statesArr.length()) {
                val obj = statesArr.optJSONObject(i) ?: continue
                val stName = obj.optString("state", "").trim()
                val distCount = obj.optInt("active_districts_count", 0)
                if (stName.isNotEmpty()) {
                    discoveredStates.add(stName)
                    displayLabels.add("$stName ($distCount ज़िले)")
                }
            }

            spinnerState.adapter = ArrayAdapter(
                ctx,
                android.R.layout.simple_spinner_dropdown_item,
                displayLabels
            )

            statusBanner.text = "✅ सरकारी सर्वर से ${discoveredStates.size} राज्यों का लाइव डेटा आ गया। कृपया अब अपना राज्य चुनें।"

            spinnerState.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position > 0 && position <= discoveredStates.size) {
                        val selectedState = discoveredStates[position - 1]
                        inputAdvisorState.setText(selectedState)
                        loadDistrictsForSelectedState(selectedState)
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    // ========================================================================
    // STEP 2: ON STATE SELECT -> LOAD ALL DISTRICTS OF THAT STATE
    // ========================================================================
    private fun loadDistrictsForSelectedState(stateName: String) {
        val ctx = context ?: return
        progressBar.visibility = View.VISIBLE
        statusBanner.text = "⏳ '$stateName' राज्य के सभी ज़िलों की सूची सरकारी सर्वर से लोड हो रही है..."

        spinnerDistrict.isEnabled = false
        spinnerMandi.isEnabled = false
        spinnerCommodity.isEnabled = false
        spinnerDistrict.adapter = ArrayAdapter(
            ctx,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("⏳ $stateName के ज़िले आ रहे हैं...")
        )

        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.getMandiDistricts(ctx, state = stateName, forceRefresh = false)
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            val distArr = res.dataObject?.optJSONArray("districts")
            if (!res.isSuccess || distArr == null || distArr.length() == 0) {
                statusBanner.text = "⚠️ '$stateName' के ज़िले लोड करने में समस्या: ${res.errorMessage ?: "पुनः प्रयास करें"}"
                return@launch
            }

            discoveredDistricts.clear()
            val displayLabels = mutableListOf("-- 2. ज़िला चुनें (${distArr.length()} ज़िले उपलब्ध) --")
            for (i in 0 until distArr.length()) {
                val dObj = distArr.optJSONObject(i) ?: continue
                val dName = dObj.optString("district", "").trim()
                val mCount = dObj.optInt("mandis_count", 0)
                if (dName.isNotEmpty()) {
                    discoveredDistricts.add(dName)
                    displayLabels.add("$dName ($mCount मंडियाँ)")
                }
            }

            spinnerDistrict.adapter = ArrayAdapter(
                ctx,
                android.R.layout.simple_spinner_dropdown_item,
                displayLabels
            )
            spinnerDistrict.isEnabled = true
            statusBanner.text = "✅ '$stateName' के ${discoveredDistricts.size} ज़िले लोड हो गए। अब अपना ज़िला चुनें।"

            spinnerDistrict.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position > 0 && position <= discoveredDistricts.size) {
                        val selectedDistrict = discoveredDistricts[position - 1]
                        inputAdvisorDistrict.setText(selectedDistrict)
                        loadMandisAndCommoditiesForSelectedDistrict(stateName, selectedDistrict)
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    // ========================================================================
    // STEP 3: ON DISTRICT SELECT -> LOAD ALL MANDIS, CROPS & LIVE RATES
    // ========================================================================
    private fun loadMandisAndCommoditiesForSelectedDistrict(stateName: String, districtName: String) {
        val ctx = context ?: return
        progressBar.visibility = View.VISIBLE
        statusBanner.text = "⏳ '$districtName' ज़िले की सभी मंडियाँ, फसलें और ताज़ा भाव लोड हो रहे हैं..."

        spinnerMandi.isEnabled = false
        spinnerCommodity.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.getMandisAndCommoditiesByDistrict(ctx, stateName, districtName)
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            val data = res.dataObject
            if (!res.isSuccess || data == null) {
                statusBanner.text = "⚠️ '$districtName' की मंडियाँ लोड करने में समस्या: ${res.errorMessage ?: ""}"
                return@launch
            }

            val mandisArr = data.optJSONArray("mandis") ?: JSONArray()
            val commsArr = data.optJSONArray("commodities") ?: JSONArray()
            val ratesArr = data.optJSONArray("rates") ?: JSONArray()

            discoveredMandis.clear()
            val mandiLabels = mutableListOf("सभी मंडियाँ (${mandisArr.length()})")
            for (i in 0 until mandisArr.length()) {
                val mObj = mandisArr.optJSONObject(i) ?: continue
                val mName = mObj.optString("mandi", "").trim()
                val recCount = mObj.optInt("records_count", 0)
                if (mName.isNotEmpty()) {
                    discoveredMandis.add(mName)
                    mandiLabels.add("$mName ($recCount भाव)")
                }
            }

            discoveredCommodities.clear()
            val commLabels = mutableListOf("सभी फसलें (${commsArr.length()})")
            for (i in 0 until commsArr.length()) {
                val cName = commsArr.optString(i, "").trim()
                if (cName.isNotEmpty()) {
                    discoveredCommodities.add(cName)
                    commLabels.add(cName)
                }
            }

            spinnerMandi.adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, mandiLabels)
            spinnerCommodity.adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, commLabels)
            spinnerMandi.isEnabled = true
            spinnerCommodity.isEnabled = true

            // ग्राफ, तुलना और AI सलाहकार के इनपुट्स में सुविधा के लिए पहली वास्तविक मंडी व फसल भरें
            if (discoveredCommodities.isNotEmpty()) {
                val firstCrop = discoveredCommodities[0]
                inputTrendCommodity.setText(firstCrop)
                inputCompareCommodity.setText(firstCrop)
                inputAdvisorCommodity.setText(firstCrop)
            }
            if (discoveredMandis.size >= 2) {
                inputCompareMandiA.setText(discoveredMandis[0])
                inputCompareMandiB.setText(discoveredMandis[1])
            } else if (discoveredMandis.size == 1) {
                inputCompareMandiA.setText(discoveredMandis[0])
            }

            spinnerMandi.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position > 0) fetchFilteredMandiRates()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            spinnerCommodity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position > 0 && position <= discoveredCommodities.size) {
                        val chosenCrop = discoveredCommodities[position - 1]
                        inputTrendCommodity.setText(chosenCrop)
                        inputCompareCommodity.setText(chosenCrop)
                        inputAdvisorCommodity.setText(chosenCrop)
                        fetchFilteredMandiRates()
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            statusBanner.text = "✅ '$districtName' की ${discoveredMandis.size} मंडियाँ, ${discoveredCommodities.size} फसलें और ${ratesArr.length()} लाइव भाव प्राप्त हुए।"
            SessionManager.saveFrequentMandiCache(ctx, stateName, districtName, ratesArr)
            renderLiveMandiRatesList(ratesArr)
        }
    }

    private fun fetchFilteredMandiRates() {
        val ctx = context ?: return
        val stateIdx = spinnerState.selectedItemPosition
        val distIdx = spinnerDistrict.selectedItemPosition
        val mandiIdx = spinnerMandi.selectedItemPosition
        val commIdx = spinnerCommodity.selectedItemPosition

        val stateVal = if (stateIdx > 0 && stateIdx <= discoveredStates.size) discoveredStates[stateIdx - 1] else null
        val distVal = if (distIdx > 0 && distIdx <= discoveredDistricts.size) discoveredDistricts[distIdx - 1] else null
        val mandiVal = if (mandiIdx > 0 && mandiIdx <= discoveredMandis.size) discoveredMandis[mandiIdx - 1] else null
        val commVal = if (commIdx > 0 && commIdx <= discoveredCommodities.size) discoveredCommodities[commIdx - 1] else null

        progressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.getLiveMandiRates(
                context = ctx,
                state = stateVal,
                district = distVal,
                mandi = mandiVal,
                commodity = commVal,
                limit = 150
            )
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            val ratesArr = res.dataObject?.optJSONArray("rates") ?: JSONArray()
            renderLiveMandiRatesList(ratesArr)
        }
    }

    private fun renderLiveMandiRatesList(ratesArr: JSONArray) {
        val ctx = context ?: return
        liveRatesContainer.removeAllViews()

        if (ratesArr.length() == 0) {
            liveRatesContainer.addView(
                TextView(ctx).apply {
                    text = "चयनित फ़िल्टर के लिए अभी सरकारी सर्वर पर कोई रिकॉर्ड नहीं मिला।"
                    setTextColor(Color.parseColor("#666666"))
                    setPadding(0, dp(8), 0, dp(8))
                }
            )
            return
        }

        for (i in 0 until ratesArr.length()) {
            val r = ratesArr.optJSONObject(i) ?: continue
            val mandi = r.optString("mandi", "—")
            val district = r.optString("district", "")
            val state = r.optString("state", "")
            val comm = r.optString("commodity", "—")
            val variety = r.optString("variety", "")
            val arrivalDate = r.optString("arrival_date", "—")
            val minQ = r.optInt("min_price_quintal", 0)
            val maxQ = r.optInt("max_price_quintal", 0)
            val modalQ = r.optInt("modal_price_quintal", 0)
            val modalKg = r.optDouble("modal_price_kg", modalQ / 100.0)

            val itemCard = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                background = roundedBg("#FAFAFA", "#C8E6C9", dp(10))
                setPadding(dp(12), dp(10), dp(12), dp(10))
                val lp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                lp.topMargin = dp(8)
                layoutParams = lp
            }

            itemCard.addView(
                TextView(ctx).apply {
                    text = "🌾 $comm ${if (variety.isNotBlank() && variety != "null") "($variety)" else ""}"
                    setTextColor(Color.parseColor("#1B5E20"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    setTypeface(typeface, Typeface.BOLD)
                }
            )
            itemCard.addView(
                TextView(ctx).apply {
                    text = "🏛️ मंडी: $mandi ($district, $state) • 📅 आवक: $arrivalDate"
                    setTextColor(Color.parseColor("#4A5A4A"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    setPadding(0, dp(2), 0, dp(4))
                }
            )
            itemCard.addView(
                TextView(ctx).apply {
                    text = "💰 मॉडल भाव: ₹$modalQ/क्विंटल (₹$modalKg/kg)\n📊 रेंज: न्यूनतम ₹$minQ  |  अधिकतम ₹$maxQ प्रति क्विंटल"
                    setTextColor(Color.parseColor("#BF360C"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setTypeface(typeface, Typeface.BOLD)
                }
            )

            liveRatesContainer.addView(itemCard)
        }
    }

    // ========================================================================
    // SECTION 2 LOGIC: 1-2 YEAR HISTORICAL FLUCTUATION GRAPH
    // ========================================================================
    private fun loadHistoricalTrendGraph() {
        val ctx = context ?: return
        val comm = inputTrendCommodity.text?.toString()?.trim() ?: ""
        if (comm.isBlank()) {
            Toast.makeText(ctx, "कृपया ग्राफ देखने के लिए फसल का नाम चुनें या लिखें।", Toast.LENGTH_SHORT).show()
            return
        }

        val stateIdx = spinnerState.selectedItemPosition
        val distIdx = spinnerDistrict.selectedItemPosition
        val mandiIdx = spinnerMandi.selectedItemPosition

        val stateVal = if (stateIdx > 0 && stateIdx <= discoveredStates.size) discoveredStates[stateIdx - 1] else null
        val distVal = if (distIdx > 0 && distIdx <= discoveredDistricts.size) discoveredDistricts[distIdx - 1] else null
        val mandiVal = if (mandiIdx > 0 && mandiIdx <= discoveredMandis.size) discoveredMandis[mandiIdx - 1] else null
        val rangeCode = rangeOptions.getOrNull(spinnerTrendRange.selectedItemPosition)?.first ?: "1y"

        progressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.getMandiPriceTrends(
                context = ctx,
                state = stateVal,
                district = distVal,
                mandi = mandiVal,
                commodity = comm,
                range = rangeCode
            )
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            val data = res.dataObject
            if (!res.isSuccess || data == null) {
                Toast.makeText(ctx, res.errorMessage ?: "ग्राफ डेटा लोड करने में समस्या आई।", Toast.LENGTH_LONG).show()
                return@launch
            }

            val summary = data.optJSONObject("summary")
            val avgModal = summary?.optInt("average_modal_quintal", 0) ?: 0
            val changePct = summary?.optDouble("price_change_percent", 0.0) ?: 0.0
            val direction = summary?.optString("trend_direction", "STABLE") ?: "STABLE"
            val highObj = summary?.optJSONObject("highest_peak")
            val lowObj = summary?.optJSONObject("lowest_dip")

            trendSummaryText.visibility = View.VISIBLE
            trendSummaryText.text = buildString {
                append("📈 फसल: ${data.optString("commodity", comm)} (${rangeCode.uppercase()})\n")
                append("• औसत मॉडल भाव: ₹$avgModal/क्विंटल\n")
                if (highObj != null) {
                    append("• उच्चतम स्तर (Peak): ₹${highObj.optInt("modal_price_quintal")} (${highObj.optString("date")})\n")
                }
                if (lowObj != null) {
                    append("• न्यूनतम स्तर (Dip): ₹${lowObj.optInt("modal_price_quintal")} (${lowObj.optString("date")})\n")
                }
                append("• रुझान (Trend): $direction ($changePct%)")
            }

            val seriesArr = data.optJSONArray("graph_series") ?: JSONArray()
            val modalPoints = mutableListOf<Float>()
            val movingAvgPoints = mutableListOf<Float>()
            val labels = mutableListOf<String>()

            for (i in 0 until seriesArr.length()) {
                val pt = seriesArr.optJSONObject(i) ?: continue
                modalPoints.add(pt.optDouble("modal_price_quintal", 0.0).toFloat())
                movingAvgPoints.add(pt.optDouble("moving_avg_quintal", 0.0).toFloat())
                labels.add(pt.optString("date", ""))
            }

            trendCanvasView.visibility = View.VISIBLE
            trendCanvasView.setSeriesData(modalPoints, movingAvgPoints, labels)
        }
    }

    // ========================================================================
    // SECTION 3 LOGIC: TWO-MANDI COMPARISON OVER RANGE
    // ========================================================================
    private fun runTwoMandiComparison() {
        val ctx = context ?: return
        val mA = inputCompareMandiA.text?.toString()?.trim() ?: ""
        val mB = inputCompareMandiB.text?.toString()?.trim() ?: ""
        val comm = inputCompareCommodity.text?.toString()?.trim() ?: ""

        if (mA.isBlank() || mB.isBlank() || comm.isBlank()) {
            Toast.makeText(ctx, "कृपया दोनों मंडियों के नाम और फसल का नाम भरें।", Toast.LENGTH_SHORT).show()
            return
        }

        val stateIdx = spinnerState.selectedItemPosition
        val stateVal = if (stateIdx > 0 && stateIdx <= discoveredStates.size) discoveredStates[stateIdx - 1] else null
        val rangeCode = rangeOptions.getOrNull(spinnerCompareRange.selectedItemPosition)?.first ?: "1y"

        progressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.compareTwoMandis(
                context = ctx,
                state = stateVal,
                mandiA = mA,
                mandiB = mB,
                commodity = comm,
                range = rangeCode
            )
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            val data = res.dataObject
            if (!res.isSuccess || data == null) {
                Toast.makeText(ctx, res.errorMessage ?: "तुलना करने में त्रुटि।", Toast.LENGTH_LONG).show()
                return@launch
            }

            val cmp = data.optJSONObject("comparison")
            val verdictHi = cmp?.optString("verdict_hi", "") ?: ""
            val diff = cmp?.optInt("average_difference_per_quintal", 0) ?: 0
            val avgA = data.optJSONObject("mandi_a")?.optJSONObject("summary")?.optInt("average_modal_quintal", 0) ?: 0
            val avgB = data.optJSONObject("mandi_b")?.optJSONObject("summary")?.optInt("average_modal_quintal", 0) ?: 0

            compareVerdictText.visibility = View.VISIBLE
            compareVerdictText.text = "⚖️ तुलना परिणाम ($comm — $rangeCode):\n" +
                "• $mA औसत भाव: ₹$avgA/क्विंटल\n" +
                "• $mB औसत भाव: ₹$avgB/क्विंटल\n" +
                "• औसत अंतर: ₹$diff/क्विंटल\n\n" +
                " निष्कर्ष: $verdictHi"
        }
    }

    // ========================================================================
    // SECTION 4 LOGIC: LOCATION-BASED AI SELLING ADVISOR
    // ========================================================================
    private fun fetchAiLocationSellingAdvice() {
        val ctx = context ?: return
        val st = inputAdvisorState.text?.toString()?.trim()
        val dist = inputAdvisorDistrict.text?.toString()?.trim()
        val comm = inputAdvisorCommodity.text?.toString()?.trim() ?: ""
        val qty = inputAdvisorQtyQuintals.text?.toString()?.toDoubleOrNull() ?: 10.0

        if (comm.isBlank() || (st.isNullOrBlank() && dist.isNullOrBlank())) {
            Toast.makeText(ctx, "कृपया अपना राज्य/ज़िला और फसल का नाम दर्ज करें।", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.getAiMandiSellingAdvice(
                context = ctx,
                state = st,
                farmerDistrict = dist,
                commodity = comm,
                quantityQuintals = qty
            )
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            val data = res.dataObject
            if (!res.isSuccess || data == null) {
                Toast.makeText(ctx, res.errorMessage ?: "AI सलाह प्राप्त करने में समस्या आई।", Toast.LENGTH_LONG).show()
                return@launch
            }

            val recHi = data.optString("ai_recommendation_hi", data.optString("message_hi", ""))
            val disclaimer = data.optString("disclaimer_hi", "")
            val bestObj = data.optJSONObject("best_recommended_mandi")

            aiAdvisorResultText.visibility = View.VISIBLE
            aiAdvisorResultText.text = buildString {
                append("🤖 AI मंडी विक्रय सलाह:\n$recHi\n")
                if (bestObj != null) {
                    append("\n🏆 सर्वोच्च शुद्ध लाभ मंडी: ${bestObj.optString("mandi")} (${bestObj.optString("district")})\n")
                    append("• मंडी मॉडल भाव: ₹${bestObj.optInt("modal_price_quintal")}/क्विंटल\n")
                    append("• अनुमानित भाड़ा: ₹${bestObj.optInt("estimated_transport_per_quintal")}/क्विंटल\n")
                    append("• भाड़ा घटाकर शुद्ध भाव: ₹${bestObj.optInt("net_price_per_quintal")}/क्विंटल\n")
                    append("• $qty क्विंटल पर कुल अनुमानित शुद्ध प्राप्ति: ₹${bestObj.optInt("estimated_total_net_rupees")}\n")
                }
                if (disclaimer.isNotBlank()) {
                    append("\n⚠️ सूचना: $disclaimer")
                }
            }
        }
    }

    // ========================================================================
    // SECTION 5 LOGIC: AI SPELLING CORRECTION MATCHER
    // ========================================================================
    private fun runSpellingMatcher() {
        val ctx = context ?: return
        val term = inputSpellingQuery.text?.toString()?.trim() ?: ""
        if (term.isBlank()) {
            Toast.makeText(ctx, "कृपया कोई शब्द लिखें।", Toast.LENGTH_SHORT).show()
            return
        }

        val rawType = spinnerSpellingType.selectedItem?.toString() ?: "commodity"
        val cleanType = rawType.substringBefore(" ").trim()
        val stateIdx = spinnerState.selectedItemPosition
        val distIdx = spinnerDistrict.selectedItemPosition
        val stateVal = if (stateIdx > 0 && stateIdx <= discoveredStates.size) discoveredStates[stateIdx - 1] else null
        val distVal = if (distIdx > 0 && distIdx <= discoveredDistricts.size) discoveredDistricts[distIdx - 1] else null

        progressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.matchMandiSpelling(ctx, term, cleanType, stateVal, distVal)
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            val data = res.dataObject
            if (res.isSuccess && data != null) {
                val matched = data.optString("matched", term)
                val conf = (data.optDouble("confidence", 0.0) * 100).toInt()
                spellingResultText.visibility = View.VISIBLE
                spellingResultText.text = "✅ आपका शब्द: '$term' → सरकारी डेटा सेंटर मिलान: '$matched' (सटीकता: $conf%)"
            } else {
                spellingResultText.visibility = View.VISIBLE
                spellingResultText.text = "मिलान परिणाम उपलब्ध नहीं हो सका।"
            }
        }
    }

    // ========================================================================
    // CUSTOM CANVAS LINE CHART FOR 1-2 YEAR PRICE TRENDS
    // ========================================================================
    private class MandiTrendCanvasView(context: Context) : View(context) {

        private var modalSeries: List<Float> = emptyList()
        private var maSeries: List<Float> = emptyList()
        private var dateLabels: List<String> = emptyList()

        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FAFDFA")
            style = Paint.Style.FILL
        }
        private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D4E2D4")
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        private val modalLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1B5E20")
            strokeWidth = 6f
            style = Paint.Style.STROKE
        }
        private val maLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#BF360C")
            strokeWidth = 4f
            style = Paint.Style.STROKE
        }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1A241A")
            textSize = 26f
        }

        fun setSeriesData(modal: List<Float>, movingAvg: List<Float>, labels: List<String>) {
            this.modalSeries = modal
            this.maSeries = movingAvg
            this.dateLabels = labels
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            canvas.drawRect(0f, 0f, w, h, bgPaint)

            if (modalSeries.isEmpty()) {
                canvas.drawText("इस रेंज के लिए पर्याप्त ग्राफ बिंदु उपलब्ध नहीं हैं", 32f, h / 2f, textPaint)
                return
            }

            val minVal = modalSeries.minOrNull() ?: 0f
            val maxVal = modalSeries.maxOrNull() ?: 100f
            val span = (maxVal - minVal).coerceAtLeast(10f)

            val padLeft = 40f
            val padRight = 30f
            val padTop = 36f
            val padBottom = 42f
            val plotW = (w - padLeft - padRight).coerceAtLeast(10f)
            val plotH = (h - padTop - padBottom).coerceAtLeast(10f)

            // Draw horizontal grid lines
            canvas.drawLine(padLeft, padTop, w - padRight, padTop, gridPaint)
            canvas.drawLine(padLeft, padTop + plotH / 2f, w - padRight, padTop + plotH / 2f, gridPaint)
            canvas.drawLine(padLeft, padTop + plotH, w - padRight, padTop + plotH, gridPaint)

            // Draw Modal Price Line
            val modalPath = Path()
            modalSeries.forEachIndexed { idx, value ->
                val x = if (modalSeries.size == 1) padLeft + plotW / 2f
                else padLeft + (idx.toFloat() / (modalSeries.size - 1)) * plotW
                val y = padTop + plotH - ((value - minVal) / span) * plotH
                if (idx == 0) modalPath.moveTo(x, y) else modalPath.lineTo(x, y)
            }
            canvas.drawPath(modalPath, modalLinePaint)

            // Draw Moving Average Line
            if (maSeries.size == modalSeries.size && maSeries.size > 1) {
                val maPath = Path()
                maSeries.forEachIndexed { idx, value ->
                    val x = padLeft + (idx.toFloat() / (maSeries.size - 1)) * plotW
                    val y = padTop + plotH - ((value - minVal) / span) * plotH
                    if (idx == 0) maPath.moveTo(x, y) else maPath.lineTo(x, y)
                }
                canvas.drawPath(maPath, maLinePaint)
            }

            // Min/Max & Date Labels
            canvas.drawText("Max: ₹${maxVal.toInt()}", padLeft, 26f, textPaint)
            canvas.drawText("Min: ₹${minVal.toInt()}", w - 220f, 26f, textPaint)
            if (dateLabels.isNotEmpty()) {
                canvas.drawText(dateLabels.first(), padLeft, h - 10f, textPaint)
                if (dateLabels.size > 1) {
                    canvas.drawText(dateLabels.last(), w - 200f, h - 10f, textPaint)
                }
            }
        }
    }

    private fun createCard(title: String): LinearLayout {
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

            addView(
                TextView(ctx).apply {
                    text = title
                    setTextColor(Color.parseColor("#124116"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    setTypeface(typeface, Typeface.BOLD)
                    setPadding(0, 0, 0, dp(6))
                }
            )
        }
    }

    private fun createLabel(textStr: String): TextView =
        TextView(requireContext()).apply {
            text = textStr
            setTextColor(Color.parseColor("#1A241A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(6), 0, dp(2))
        }

    private fun createInput(hintStr: String, isNumber: Boolean = false): EditText =
        EditText(requireContext()).apply {
            hint = hintStr
            inputType = if (isNumber) {
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            } else {
                InputType.TYPE_CLASS_TEXT
            }
            background = roundedBg("#FAFAFA", "#B8CBB8", dp(8))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            minHeight = dp(44)
        }

    private fun createButton(label: String, bgHex: String, onClick: () -> Unit): Button =
        Button(requireContext()).apply {
            text = label
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            background = roundedBg(bgHex, bgHex, dp(10))
            minHeight = dp(46)
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
