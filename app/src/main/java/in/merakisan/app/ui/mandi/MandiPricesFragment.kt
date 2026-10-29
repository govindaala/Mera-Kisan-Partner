// app/src/main/java/in/merakisan/app/ui/mandi/MandiPricesFragment.kt
package `in`.merakisan.app.ui.mandi

import android.app.DatePickerDialog
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
import java.util.Calendar
import java.util.Locale

/**
 * MERA KISAN — LIVE & HISTORICAL GOVERNMENT MANDI PRICES, DATE SYNC & MULTI-POINT GRAPH
 * Exact Repository Path: app/src/main/java/in/merakisan/app/ui/mandi/MandiPricesFragment.kt
 *
 * Fixes:
 * 1. White-on-White Spinner dropdown text replaced with high-contrast dark green/black text (#124116 on #FFFFFF).
 * 2. Date Picker + Full State/Mandi Sync button ("📅 तारीख चुनें" & "🔄 इस तारीख का डेटा सिंक करें").
 * 3. Handles both String and JSONObject items for commodities/mandis so raw JSON never appears.
 * 4. Plots every available date point in the server for 7d, 30d, 90d, 180d, 1y, 2y trends & Mandi A vs B comparison.
 */
class MandiPricesFragment : Fragment() {

    private var selectedIsoDate: String? = null // null = Latest available date

    private val statesList = mutableListOf<String>()
    private val districtsList = mutableListOf<String>()
    private val mandisList = mutableListOf<String>()
    private val commoditiesList = mutableListOf<String>()

    private lateinit var tvStatusBanner: TextView
    private lateinit var btnPickDate: Button
    private lateinit var btnSyncDateAllMandis: Button
    private lateinit var btnClearDateFilter: Button

    private lateinit var spinnerState: Spinner
    private lateinit var spinnerDistrict: Spinner
    private lateinit var spinnerMandi: Spinner
    private lateinit var spinnerCommodity: Spinner
    private lateinit var progressBar: ProgressBar
    private lateinit var ratesListContainer: LinearLayout

    // Section 2: Multi-Date Trend Graph Controls
    private lateinit var inputTrendCommodity: EditText
    private lateinit var spinnerTrendRange: Spinner
    private lateinit var tvTrendSummary: TextView
    private lateinit var trendChartContainer: LinearLayout
    private lateinit var trendDateTableContainer: LinearLayout

    // Section 3: Two-Mandi Date-Wise Comparison Controls
    private lateinit var inputMandiA: EditText
    private lateinit var inputMandiB: EditText
    private lateinit var inputCompareCommodity: EditText
    private lateinit var spinnerCompareRange: Spinner
    private lateinit var tvCompareResult: TextView
    private lateinit var compareChartContainer: LinearLayout
    private lateinit var compareDateTableContainer: LinearLayout

    // Section 4: AI Selling Advisor
    private lateinit var inputAdvisorState: EditText
    private lateinit var inputAdvisorDistrict: EditText
    private lateinit var inputAdvisorCommodity: EditText
    private lateinit var inputAdvisorQuantity: EditText
    private lateinit var tvAdvisorOutput: TextView

    // Section 5: AI Spelling Matcher
    private lateinit var inputSpellingTerm: EditText
    private lateinit var tvSpellingResult: TextView

    private val rangeOptions = listOf(
        "7d" to "7 दिन (1 सप्ताह)",
        "30d" to "30 दिन (1 महीना)",
        "90d" to "90 दिन (3 महीने — हर उपलब्ध तारीख)",
        "180d" to "6 महीने (180 दिन — हर उपलब्ध तारीख)",
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
            setPadding(dp(14), dp(14), dp(14), dp(32))
        }

        // Top Title Card
        val heroCard = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#1B5E20", "#124116", dp(14))
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        heroCard.addView(
            TextView(ctx).apply {
                text = "📈 लाइव सरकारी मंडी भाव, तारीख सिंक व मल्टी-डेट ग्राफ"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                setTypeface(typeface, Typeface.BOLD)
            }
        )
        heroCard.addView(
            TextView(ctx).apply {
                text = "सीधे सरकारी सोर्स (api.data.gov.in) से किसी भी तारीख का मंडी डेटा सिंक करें और हर तारीख का ग्राफ देखें"
                setTextColor(Color.parseColor("#C8E6C9"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, dp(4), 0, 0)
            }
        )
        root.addView(heroCard)

        // Status Banner
        tvStatusBanner = TextView(ctx).apply {
            text = "⏳ सरकारी सर्वर से मंडी डेटा की जाँच हो रही है..."
            setTextColor(Color.parseColor("#1B5E20"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, Typeface.BOLD)
            background = roundedBg("#E8F5E9", "#A5D6A7", dp(10))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
            layoutParams = lp
        }
        root.addView(tvStatusBanner)

        progressBar = ProgressBar(ctx).apply {
            visibility = View.GONE
            setPadding(0, dp(8), 0, dp(8))
        }
        root.addView(progressBar)

        // ====================================================================
        // CARD 0: DATE PICKER & ALL-STATE / ALL-MANDI SYNC BAR
        // ====================================================================
        val dateSyncCard = createSectionCard()
        dateSyncCard.addView(createHeading("📅 तारीख अनुसार मंडी भाव व सर्वर डेटा सिंक"))
        dateSyncCard.addView(
            TextView(ctx).apply {
                text = "कोई भी तारीख चुनकर उस दिन के सभी प्रदेशों और सभी मंडियों का डेटा सर्वर में सिंक करें:"
                setTextColor(Color.parseColor("#4A5A4A"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, 0, 0, dp(8))
            }
        )

        val dateRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        btnPickDate = createButton("📅 तारीख चुनें (आज/कोई भी दिन)", "#0D47A1") {
            openDatePickerForMandi()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dp(6)
            }
        }

        btnClearDateFilter = createButton("🔄 ताज़ा (Latest)", "#455A64") {
            selectedIsoDate = null
            btnPickDate.text = "📅 तारीख चुनें (Latest)"
            loadStatesFromServer(forceRefresh = false)
        }
        dateRow.addView(btnPickDate)
        dateRow.addView(btnClearDateFilter)
        dateSyncCard.addView(dateRow)

        btnSyncDateAllMandis = createButton(
            "☁️ चुनी गई तारीख का पूरे देश/प्रदेश की मंडियों का डेटा सिंक करें",
            "#BF360C"
        ) {
            syncSelectedDateAcrossMandis()
        }.apply {
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
            layoutParams = lp
        }
        dateSyncCard.addView(btnSyncDateAllMandis)
        root.addView(dateSyncCard)

        // ====================================================================
        // CARD 1: STEP-BY-STEP STATE -> DISTRICT -> MANDI -> COMMODITY
        // ====================================================================
        val filterCard = createSectionCard()
        filterCard.addView(createHeading("1. क्रमबद्ध मंडी व फसल चयन (100% सरकारी सोर्स)"))

        filterCard.addView(createLabel("स्टेप 1 — पहले राज्य चुनें (State):"))
        spinnerState = createHighContrastSpinner()
        filterCard.addView(spinnerState)

        filterCard.addView(createLabel("स्टेप 2 — उस राज्य का ज़िला चुनें (District):"))
        spinnerDistrict = createHighContrastSpinner()
        filterCard.addView(spinnerDistrict)

        filterCard.addView(createLabel("स्टेप 3 — उस ज़िले की मंडी चुनें (Market):"))
        spinnerMandi = createHighContrastSpinner()
        filterCard.addView(spinnerMandi)

        filterCard.addView(createLabel("स्टेप 4 — फसल चुनें (Commodity):"))
        spinnerCommodity = createHighContrastSpinner()
        filterCard.addView(spinnerCommodity)

        val actionRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
        }

        val btnReloadStates = createButton("🔄 राज्य री-लोड करें", "#0D47A1") {
            loadStatesFromServer(forceRefresh = true)
        }.apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dp(6)
            }
        }

        val btnFetchRates = createButton("🌾 मंडी भाव देखें", "#1B5E20") {
            fetchRatesForCurrentSelection()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(6)
            }
        }

        actionRow.addView(btnReloadStates)
        actionRow.addView(btnFetchRates)
        filterCard.addView(actionRow)

        ratesListContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, 0)
        }
        filterCard.addView(ratesListContainer)
        root.addView(filterCard)

        // ====================================================================
        // CARD 2: MULTI-DATE PRICE TREND GRAPH (EVERY AVAILABLE SERVER DATE)
        // ====================================================================
        val trendCard = createSectionCard()
        trendCard.addView(createHeading("2. रेंज अनुसार फसल भाव उतार-चढ़ाव ग्राफ (हर उपलब्ध तारीख का डेटा)"))

        trendCard.addView(createLabel("फसल (Commodity):"))
        inputTrendCommodity = createEditText("उदा. Soyabean, Wheat, Groundnut, Garlic...")
        trendCard.addView(inputTrendCommodity)

        trendCard.addView(createLabel("अवधि रेंज चुनें (7 दिन / 3 महीने / 1 साल आदि):"))
        spinnerTrendRange = createHighContrastSpinner()
        setSpinnerItems(spinnerTrendRange, rangeOptions.map { it.second })
        spinnerTrendRange.setSelection(2) // Default: 90d (3 months)
        trendCard.addView(spinnerTrendRange)

        trendCard.addView(
            createButton("📊 उतार-चढ़ाव ग्राफ व हर तारीख का विश्लेषण देखें", "#1B5E20") {
                loadMultiPointTrendGraph()
            }.apply {
                val lp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(10) }
                layoutParams = lp
            }
        )

        tvTrendSummary = TextView(ctx).apply {
            visibility = View.GONE
            setTextColor(Color.parseColor("#124116"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(10), 0, dp(6))
        }
        trendCard.addView(tvTrendSummary)

        trendChartContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }
        trendCard.addView(trendChartContainer)

        trendDateTableContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        trendCard.addView(trendDateTableContainer)
        root.addView(trendCard)

        // ====================================================================
        // CARD 3: TWO-MANDI DATE-BY-DATE COMPARISON (MANDI A VS MANDI B)
        // ====================================================================
        val compareCard = createSectionCard()
        compareCard.addView(createHeading("3. दो मंडियों के भाव की तारीख-वार तुलना (Mandi A बनाम Mandi B)"))

        compareCard.addView(createLabel("पहली मंडी (Mandi A):"))
        inputMandiA = createEditText("उदा. Indore APMC या Mandsaur")
        compareCard.addView(inputMandiA)

        compareCard.addView(createLabel("दूसरी मंडी (Mandi B):"))
        inputMandiB = createEditText("उदा. Sanwer APMC या Neemuch")
        compareCard.addView(inputMandiB)

        compareCard.addView(createLabel("फसल का नाम (Commodity):"))
        inputCompareCommodity = createEditText("उदा. Soyabean, Wheat, Garlic...")
        compareCard.addView(inputCompareCommodity)

        compareCard.addView(createLabel("तुलना की रेंज (हर उपलब्ध तारीख):"))
        spinnerCompareRange = createHighContrastSpinner()
        setSpinnerItems(spinnerCompareRange, rangeOptions.map { it.second })
        spinnerCompareRange.setSelection(2) // Default: 90d (3 months)
        compareCard.addView(spinnerCompareRange)

        compareCard.addView(
            createButton("⚖️ दोनों मंडियों की तारीख-वार तुलना व ग्राफ देखें", "#0D47A1") {
                compareMandisDateByDate()
            }.apply {
                val lp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(10) }
                layoutParams = lp
            }
        )

        tvCompareResult = TextView(ctx).apply {
            visibility = View.GONE
            setTextColor(Color.parseColor("#1A241A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(10), 0, dp(6))
        }
        compareCard.addView(tvCompareResult)

        compareChartContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }
        compareCard.addView(compareChartContainer)

        compareDateTableContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        compareCard.addView(compareDateTableContainer)
        root.addView(compareCard)

        // ====================================================================
        // CARD 4: AI MANDI SELLING ADVISOR
        // ====================================================================
        val advisorCard = createSectionCard()
        advisorCard.addView(createHeading("4. 🤖 AI मंडी विक्रय सलाहकार (भाड़ा घटाकर शुद्ध लाभ)"))

        advisorCard.addView(createLabel("आपका राज्य:"))
        inputAdvisorState = createEditText("Madhya Pradesh").apply {
            setText(SessionManager.getUserState(ctx).ifBlank { "Madhya Pradesh" })
        }
        advisorCard.addView(inputAdvisorState)

        advisorCard.addView(createLabel("आपका ज़िला:"))
        inputAdvisorDistrict = createEditText("Mandsaur").apply {
            setText(SessionManager.getUserDistrict(ctx).ifBlank { "Mandsaur" })
        }
        advisorCard.addView(inputAdvisorDistrict)

        advisorCard.addView(createLabel("बेचने वाली फसल:"))
        inputAdvisorCommodity = createEditText("उदा. Soyabean, Wheat, Garlic...")
        advisorCard.addView(inputAdvisorCommodity)

        advisorCard.addView(createLabel("कुल मात्रा (क्विंटल में):"))
        inputAdvisorQuantity = createEditText("15", isNumber = true).apply { setText("15") }
        advisorCard.addView(inputAdvisorQuantity)

        advisorCard.addView(
            createButton("🤖 AI से जानें: किस मंडी में बेचना सबसे फायदेमंद है?", "#BF360C") {
                runAiSellingAdvisor()
            }.apply {
                val lp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(10) }
                layoutParams = lp
            }
        )

        tvAdvisorOutput = TextView(ctx).apply {
            visibility = View.GONE
            background = roundedBg("#FFF8E1", "#FFE082", dp(10))
            setTextColor(Color.parseColor("#3E2723"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
            layoutParams = lp
        }
        advisorCard.addView(tvAdvisorOutput)
        root.addView(advisorCard)

        // ====================================================================
        // CARD 5: AI SPELLING MATCHER
        // ====================================================================
        val spellCard = createSectionCard()
        spellCard.addView(createHeading("5. 🔤 AI स्पेलिंग सुधारक व सरकारी डेटा सेंटर मैचर"))
        inputSpellingTerm = createEditText("गलत स्पेलिंग या हिंदी नाम लिखें (उदा. सोयाबीन, लहसुन, मंदसौर)")
        spellCard.addView(inputSpellingTerm)

        spellCard.addView(
            createButton("🔤 सरकारी स्पेलिंग से मैच करें", "#2E7D32") {
                runSpellingMatcher()
            }.apply {
                val lp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(8) }
                layoutParams = lp
            }
        )

        tvSpellingResult = TextView(ctx).apply {
            visibility = View.GONE
            setTextColor(Color.parseColor("#1B5E20"))
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(8), 0, 0)
        }
        spellCard.addView(tvSpellingResult)
        root.addView(spellCard)

        setupSpinnerListeners()

        scroll.addView(root)
        return scroll
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Show cached rates immediately if available so the user never waits on a blank screen
        val cached = SessionManager.getFrequentMandiCache(requireContext())
        if (cached != null) {
            val cachedRates = cached.optJSONArray("rates")
            if (cachedRates != null && cachedRates.length() > 0) {
                renderRatesCards(cachedRates)
            }
        }
        loadStatesFromServer(forceRefresh = false)
    }

    // ========================================================================
    // HIGH-CONTRAST SPINNER ADAPTER (FIXES WHITE-ON-WHITE DROPDOWN TEXT)
    // ========================================================================

    private fun createHighContrastSpinner(): Spinner {
        val ctx = requireContext()
        return Spinner(ctx, Spinner.MODE_DROPDOWN).apply {
            minimumHeight = dp(48)
            background = roundedBg("#FAFAFA", "#81C784", dp(8))
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
    }

    private fun setSpinnerItems(spinner: Spinner, items: List<String>) {
        val ctx = context ?: return
        val adapter = object : ArrayAdapter<String>(ctx, android.R.layout.simple_spinner_item, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val tv = (super.getView(position, convertView, parent) as? TextView) ?: TextView(ctx)
                tv.text = getItem(position) ?: ""
                tv.setTextColor(Color.parseColor("#124116"))
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                tv.setTypeface(tv.typeface, Typeface.BOLD)
                tv.setPadding(dp(10), dp(10), dp(10), dp(10))
                return tv
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val tv = (super.getDropDownView(position, convertView, parent) as? TextView) ?: TextView(ctx)
                tv.text = getItem(position) ?: ""
                tv.setBackgroundColor(Color.WHITE)
                tv.setTextColor(Color.parseColor("#1A241A"))
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                tv.setTypeface(null, if (position == 0) Typeface.BOLD else Typeface.NORMAL)
                tv.setPadding(dp(16), dp(14), dp(16), dp(14))
                return tv
            }
        }
        spinner.adapter = adapter
    }

    private fun setupSpinnerListeners() {
        spinnerState.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position <= 0 || position > statesList.size) return
                val chosenState = statesList[position - 1]
                inputAdvisorState.setText(chosenState)
                loadDistrictsForState(chosenState)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerDistrict.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position <= 0 || position > districtsList.size) return
                val chosenDistrict = districtsList[position - 1]
                val chosenState = statesList.getOrNull(spinnerState.selectedItemPosition - 1)
                inputAdvisorDistrict.setText(chosenDistrict)
                loadMandisAndCommoditiesForDistrict(chosenState, chosenDistrict)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerCommodity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position <= 0 || position > commoditiesList.size) return
                val cleanComm = commoditiesList[position - 1]
                inputTrendCommodity.setText(cleanComm)
                inputCompareCommodity.setText(cleanComm)
                inputAdvisorCommodity.setText(cleanComm)
                fetchRatesForCurrentSelection()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // ========================================================================
    // DATE PICKER & DATE-BASED FULL MANDI SYNC
    // ========================================================================

    private fun openDatePickerForMandi() {
        val ctx = context ?: return
        val cal = Calendar.getInstance()
        DatePickerDialog(
            ctx,
            { _, year, monthOfYear, dayOfMonth ->
                val iso = String.format(Locale.US, "%04d-%02d-%02d", year, monthOfYear + 1, dayOfMonth)
                val dmy = String.format(Locale.US, "%02d/%02d/%04d", dayOfMonth, monthOfYear + 1, year)
                selectedIsoDate = iso
                btnPickDate.text = "📅 तारीख: $dmy"
                tvStatusBanner.text = "📅 चुनी गई तारीख: $dmy ($iso) — राज्यों व मंडियों का डेटा लोड हो रहा है..."
                loadStatesFromServer(forceRefresh = true)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun syncSelectedDateAcrossMandis() {
        val ctx = context ?: return
        val dateToSync = selectedIsoDate ?: Calendar.getInstance().let {
            String.format(
                Locale.US,
                "%04d-%02d-%02d",
                it.get(Calendar.YEAR),
                it.get(Calendar.MONTH) + 1,
                it.get(Calendar.DAY_OF_MONTH)
            )
        }
        val stateFilter = statesList.getOrNull(spinnerState.selectedItemPosition - 1)

        progressBar.visibility = View.VISIBLE
        tvStatusBanner.text = "☁️ तारीख $dateToSync के लिए सभी राज्यों और मंडियों का डेटा सरकारी सर्वर से सिंक हो रहा है..."

        viewLifecycleOwner.lifecycleScope.launch {
            val body = JSONObject().apply {
                put("date", dateToSync)
                if (!stateFilter.isNullOrBlank()) put("state", stateFilter)
            }
            val res = ApiClient.request(ctx, "/mandi-prices/sync-date", method = "POST", bodyJson = body)
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            if (res.isSuccess && res.dataObject != null) {
                val d = res.dataObject
                val savedCount = d.optInt("total_records_saved", 0)
                val statesCov = d.optInt("states_covered", 0)
                val mandisCov = d.optInt("mandis_covered", 0)
                tvStatusBanner.text = "✅ तारीख $dateToSync सिंक पूर्ण: $statesCov राज्यों की $mandisCov मंडियों के $savedCount भाव सर्वर में सेव हो गए!"
                loadStatesFromServer(forceRefresh = true)
            } else {
                tvStatusBanner.text = "⚠️ सिंक संदेश: ${res.errorMessage ?: "उस तारीख का डेटा अभी सरकारी पोर्टल पर अपलोड नहीं हुआ है।"}"
            }
        }
    }

    // ========================================================================
    // STEP 1..4 HIERARCHICAL MANDI LOADING
    // ========================================================================

    private fun loadStatesFromServer(forceRefresh: Boolean) {
        val ctx = context ?: return
        progressBar.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.request(
                context = ctx,
                endpointPath = "/mandi-prices/states",
                method = "GET",
                queryParams = mapOf(
                    "date" to selectedIsoDate,
                    "refresh" to if (forceRefresh) "true" else null
                )
            )
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            val data = res.dataObject
            val arr = data?.optJSONArray("states") ?: JSONArray()
            statesList.clear()
            val displayLabels = mutableListOf("स्टेप 1 — पहले राज्य चुनें (${arr.length()} राज्य उपलब्ध)")

            for (i in 0 until arr.length()) {
                val item = arr.opt(i)
                when (item) {
                    is JSONObject -> {
                        val stName = item.optString("state", item.optString("name", "")).trim()
                        val dCount = item.optInt("districts_count", item.optInt("district_count", 0))
                        if (stName.isNotBlank()) {
                            statesList.add(stName)
                            displayLabels.add(if (dCount > 0) "$stName ($dCount ज़िले)" else stName)
                        }
                    }
                    is String -> {
                        if (item.isNotBlank()) {
                            statesList.add(item)
                            displayLabels.add(item)
                        }
                    }
                }
            }

            setSpinnerItems(spinnerState, displayLabels)
            setSpinnerItems(spinnerDistrict, listOf("स्टेप 2 — पहले ऊपर राज्य चुनें"))
            setSpinnerItems(spinnerMandi, listOf("स्टेप 3 — पहले ज़िला चुनें"))
            setSpinnerItems(spinnerCommodity, listOf("स्टेप 4 — पहले ज़िला चुनें"))

            val effDate = data?.optString("selected_date", selectedIsoDate ?: "Latest") ?: "Latest"
            tvStatusBanner.text = if (statesList.isNotEmpty()) {
                "✅ सरकारी सर्वर से ${statesList.size} राज्यों का डेटा (तारीख: $effDate) आ गया। अब अपना राज्य चुनें।"
            } else {
                "⚠️ चयनित तारीख ($effDate) पर अभी कोई राज्य सूचीबद्ध नहीं मिला। कृपया दूसरी तारीख चुनें या री-लोड करें।"
            }
        }
    }

    private fun loadDistrictsForState(stateName: String) {
        val ctx = context ?: return
        progressBar.visibility = View.VISIBLE
        tvStatusBanner.text = "⏳ '$stateName' राज्य के सभी ज़िलों की सूची लोड हो रही है..."

        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.request(
                context = ctx,
                endpointPath = "/mandi-prices/districts",
                method = "GET",
                queryParams = mapOf(
                    "state" to stateName,
                    "date" to selectedIsoDate
                )
            )
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            val arr = res.dataObject?.optJSONArray("districts") ?: JSONArray()
            districtsList.clear()
            val displayLabels = mutableListOf("स्टेप 2 — ज़िला चुनें (${arr.length()} ज़िले उपलब्ध)")

            for (i in 0 until arr.length()) {
                val item = arr.opt(i)
                when (item) {
                    is JSONObject -> {
                        val dName = item.optString("district", item.optString("name", "")).trim()
                        val mCount = item.optInt("mandis_count", item.optInt("mandi_count", 0))
                        if (dName.isNotBlank()) {
                            districtsList.add(dName)
                            displayLabels.add(if (mCount > 0) "$dName ($mCount मंडियाँ)" else dName)
                        }
                    }
                    is String -> {
                        if (item.isNotBlank()) {
                            districtsList.add(item)
                            displayLabels.add(item)
                        }
                    }
                }
            }

            setSpinnerItems(spinnerDistrict, displayLabels)
            tvStatusBanner.text = "✅ '$stateName' के ${districtsList.size} ज़िले लोड हो गए। अब अपना ज़िला चुनें।"
        }
    }

    private fun loadMandisAndCommoditiesForDistrict(stateName: String?, districtName: String) {
        val ctx = context ?: return
        progressBar.visibility = View.VISIBLE
        tvStatusBanner.text = "⏳ '$districtName' ज़िले की सभी मंडियाँ, फसलें और भाव लोड हो रहे हैं..."

        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.request(
                context = ctx,
                endpointPath = "/mandi-prices/mandis",
                method = "GET",
                queryParams = mapOf(
                    "state" to stateName,
                    "district" to districtName,
                    "date" to selectedIsoDate
                )
            )
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            val data = res.dataObject
            val mandisArr = data?.optJSONArray("mandis") ?: JSONArray()
            val commArr = data?.optJSONArray("commodities") ?: JSONArray()
            val ratesArr = data?.optJSONArray("rates") ?: JSONArray()

            mandisList.clear()
            val mandiLabels = mutableListOf("सभी मंडियाँ (${mandisArr.length()})")
            for (i in 0 until mandisArr.length()) {
                val item = mandisArr.opt(i)
                val mName = when (item) {
                    is JSONObject -> item.optString("mandi", item.optString("market", item.optString("name", ""))).trim()
                    is String -> extractCleanNameIfJson(item, "mandi")
                    else -> ""
                }
                if (mName.isNotBlank()) {
                    mandisList.add(mName)
                    mandiLabels.add(mName)
                }
            }

            commoditiesList.clear()
            val commLabels = mutableListOf("सभी फसलें (${commArr.length()})")
            for (i in 0 until commArr.length()) {
                val item = commArr.opt(i)
                val cName = when (item) {
                    is JSONObject -> item.optString("commodity", item.optString("name", "")).trim()
                    is String -> extractCleanNameIfJson(item, "commodity")
                    else -> ""
                }
                if (cName.isNotBlank()) {
                    commoditiesList.add(cName)
                    commLabels.add(cName)
                }
            }

            setSpinnerItems(spinnerMandi, mandiLabels)
            setSpinnerItems(spinnerCommodity, commLabels)

            if (mandisList.size >= 2) {
                inputMandiA.setText(mandisList[0])
                inputMandiB.setText(mandisList[1])
            } else if (mandisList.size == 1) {
                inputMandiA.setText(mandisList[0])
            }
            if (commoditiesList.isNotEmpty()) {
                inputTrendCommodity.setText(commoditiesList[0])
                inputCompareCommodity.setText(commoditiesList[0])
                inputAdvisorCommodity.setText(commoditiesList[0])
            }

            tvStatusBanner.text = "✅ '$districtName' की ${mandisList.size} मंडियाँ, ${commoditiesList.size} फसलें और ${ratesArr.length()} भाव प्राप्त हुए।"

            if (ratesArr.length() > 0) {
                SessionManager.saveFrequentMandiCache(ctx, stateName ?: "", districtName, ratesArr)
                renderRatesCards(ratesArr)
            } else {
                fetchRatesForCurrentSelection()
            }
        }
    }

    private fun fetchRatesForCurrentSelection() {
        val ctx = context ?: return
        val stateName = statesList.getOrNull(spinnerState.selectedItemPosition - 1)
        val districtName = districtsList.getOrNull(spinnerDistrict.selectedItemPosition - 1)
        val mandiName = mandisList.getOrNull(spinnerMandi.selectedItemPosition - 1)
        val commodityName = commoditiesList.getOrNull(spinnerCommodity.selectedItemPosition - 1)

        progressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.request(
                context = ctx,
                endpointPath = "/mandi-prices",
                method = "GET",
                queryParams = mapOf(
                    "state" to stateName,
                    "district" to districtName,
                    "mandi" to mandiName,
                    "commodity" to commodityName,
                    "date" to selectedIsoDate,
                    "limit" to "250"
                )
            )
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            val ratesArr = res.dataObject?.optJSONArray("rates")
                ?: res.dataArray
                ?: JSONArray()
            renderRatesCards(ratesArr)
        }
    }

    private fun renderRatesCards(ratesArr: JSONArray) {
        val ctx = context ?: return
        ratesListContainer.removeAllViews()

        if (ratesArr.length() == 0) {
            ratesListContainer.addView(
                TextView(ctx).apply {
                    text = "चयनित फ़िल्टर या तारीख के लिए कोई रिकॉर्ड नहीं मिला। ऊपर '☁️ डेटा सिंक करें' दबाएँ या दूसरी तारीख चुनें।"
                    setTextColor(Color.parseColor("#555555"))
                    setPadding(dp(8), dp(12), dp(8), dp(12))
                }
            )
            return
        }

        val countToRender = minOf(ratesArr.length(), 80)
        for (i in 0 until countToRender) {
            val r = ratesArr.optJSONObject(i) ?: continue
            val comm = r.optString("commodity", "फसल")
            val variety = r.optString("variety", "")
            val market = r.optString("market", r.optString("mandi", ""))
            val dist = r.optString("district", "")
            val state = r.optString("state", "")
            val arrivalDate = r.optString("arrival_date", r.optString("iso_date", ""))
            val minP = r.optInt("min_price", 0)
            val maxP = r.optInt("max_price", 0)
            val modalP = r.optInt("modal_price", 0)
            val perKg = String.format(Locale.US, "%.1f", modalP / 100.0)

            val card = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                background = roundedBg("#FFFFFF", "#C8E6C9", dp(10))
                setPadding(dp(12), dp(10), dp(12), dp(10))
                val lp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(8) }
                layoutParams = lp
            }

            card.addView(
                TextView(ctx).apply {
                    text = "🌾 $comm ${if (variety.isNotBlank() && variety != "null") "($variety)" else ""}"
                    setTextColor(Color.parseColor("#124116"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    setTypeface(typeface, Typeface.BOLD)
                }
            )
            card.addView(
                TextView(ctx).apply {
                    text = "🏛️ मंडी: $market ($dist, $state) • 📅 आवक: $arrivalDate"
                    setTextColor(Color.parseColor("#4A5A4A"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    setPadding(0, dp(2), 0, dp(4))
                }
            )
            card.addView(
                TextView(ctx).apply {
                    text = "💰 मॉडल भाव: ₹$modalP/क्विंटल (₹$perKg/kg)\n📊 रेंज: न्यूनतम ₹$minP | अधिकतम ₹$maxP प्रति क्विंटल"
                    setTextColor(Color.parseColor("#BF360C"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setTypeface(typeface, Typeface.BOLD)
                }
            )

            ratesListContainer.addView(card)
        }
    }

    // ========================================================================
    // SECTION 2: MULTI-POINT TIME-SERIES GRAPH (ALL AVAILABLE SERVER DATES)
    // ========================================================================

    private fun loadMultiPointTrendGraph() {
        val ctx = context ?: return
        val rawComm = inputTrendCommodity.text?.toString()?.trim() ?: ""
        val cleanComm = extractCleanNameIfJson(rawComm, "commodity")
        if (cleanComm.isBlank()) {
            Toast.makeText(ctx, "कृपया ग्राफ देखने के लिए फसल का नाम चुनें या लिखें।", Toast.LENGTH_SHORT).show()
            return
        }
        inputTrendCommodity.setText(cleanComm)

        val rangeCode = rangeOptions.getOrNull(spinnerTrendRange.selectedItemPosition)?.first ?: "90d"
        val stateName = statesList.getOrNull(spinnerState.selectedItemPosition - 1)
        val districtName = districtsList.getOrNull(spinnerDistrict.selectedItemPosition - 1)
        val mandiName = mandisList.getOrNull(spinnerMandi.selectedItemPosition - 1)

        progressBar.visibility = View.VISIBLE
        trendChartContainer.removeAllViews()
        trendDateTableContainer.removeAllViews()

        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.request(
                context = ctx,
                endpointPath = "/mandi-prices/trends",
                method = "GET",
                queryParams = mapOf(
                    "state" to stateName,
                    "district" to districtName,
                    "mandi" to mandiName,
                    "commodity" to cleanComm,
                    "range" to rangeCode
                )
            )
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            val d = res.dataObject
            val pointsArr = d?.optJSONArray("points")
                ?: d?.optJSONArray("trend_points")
                ?: JSONArray()

            if (pointsArr.length() == 0) {
                tvTrendSummary.visibility = View.VISIBLE
                tvTrendSummary.text = "⚠️ '$cleanComm' के लिए इस रेंज में अभी कोई तारीख डेटा उपलब्ध नहीं है। कृपया ऊपर तारीख चुनकर सिंक करें।"
                return@launch
            }

            val avgModal = d?.optInt("average_modal_price", 0) ?: 0
            val trendDir = d?.optString("trend_direction", "STABLE") ?: "STABLE"
            val changePct = d?.optDouble("change_percent", 0.0) ?: 0.0
            val peakObj = d?.optJSONObject("peak_point")
            val dipObj = d?.optJSONObject("dip_point")
            val rangeLabel = d?.optString("range_label", rangeCode) ?: rangeCode

            tvTrendSummary.visibility = View.VISIBLE
            tvTrendSummary.text = buildString {
                append("📈 फसल: ${d?.optString("commodity", cleanComm)} ($rangeLabel)\n")
                append("• सर्वर में उपलब्ध कुल तारीखें: ${pointsArr.length()} दिन\n")
                append("• औसत मॉडल भाव: ₹$avgModal/क्विंटल\n")
                if (peakObj != null) {
                    append("• उच्चतम स्तर (Peak): ₹${peakObj.optInt("modal_price")} (${peakObj.optString("date")})\n")
                }
                if (dipObj != null) {
                    append("• न्यूनतम स्तर (Dip): ₹${dipObj.optInt("modal_price")} (${dipObj.optString("date")})\n")
                }
                append("• रुझान (Trend): $trendDir ($changePct%)")
            }

            val chartView = MultiPointLineChartView(ctx, pointsArr, null, cleanComm, null)
            chartView.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(230)
            )
            trendChartContainer.addView(chartView)

            // Render every available date's record below the graph
            trendDateTableContainer.addView(
                TextView(ctx).apply {
                    text = "📅 हर उपलब्ध तारीख का मंडी भाव विवरण (${pointsArr.length()} तारीखें):"
                    setTextColor(Color.parseColor("#124116"))
                    setTypeface(typeface, Typeface.BOLD)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setPadding(0, dp(8), 0, dp(4))
                }
            )

            for (i in pointsArr.length() - 1 downTo 0) {
                val pt = pointsArr.optJSONObject(i) ?: continue
                val dt = pt.optString("date", pt.optString("iso_date", ""))
                val mod = pt.optInt("modal_price", 0)
                val minP = pt.optInt("min_price", mod)
                val maxP = pt.optInt("max_price", mod)
                val mCount = pt.optInt("mandis_reporting", 1)

                trendDateTableContainer.addView(
                    TextView(ctx).apply {
                        text = "• $dt  →  मॉडल: ₹$mod/q  (न्यूनतम: ₹$minP | अधिकतम: ₹$maxP) • $mCount मंडी"
                        setTextColor(Color.parseColor("#333333"))
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                        setPadding(dp(4), dp(3), dp(4), dp(3))
                    }
                )
            }
        }
    }

    // ========================================================================
    // SECTION 3: TWO-MANDI DATE-BY-DATE COMPARISON
    // ========================================================================

    private fun compareMandisDateByDate() {
        val ctx = context ?: return
        val mandiA = extractCleanNameIfJson(inputMandiA.text?.toString() ?: "", "mandi")
        val mandiB = extractCleanNameIfJson(inputMandiB.text?.toString() ?: "", "mandi")
        val comm = extractCleanNameIfJson(inputCompareCommodity.text?.toString() ?: "", "commodity")
        val rangeCode = rangeOptions.getOrNull(spinnerCompareRange.selectedItemPosition)?.first ?: "90d"
        val stateName = statesList.getOrNull(spinnerState.selectedItemPosition - 1)

        if (mandiA.isBlank() || mandiB.isBlank() || comm.isBlank()) {
            Toast.makeText(ctx, "कृपया पहली मंडी, दूसरी मंडी और फसल तीनों का नाम भरें।", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        compareChartContainer.removeAllViews()
        compareDateTableContainer.removeAllViews()

        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.request(
                context = ctx,
                endpointPath = "/mandi-prices/compare",
                method = "GET",
                queryParams = mapOf(
                    "state" to stateName,
                    "mandi_a" to mandiA,
                    "mandi_b" to mandiB,
                    "commodity" to comm,
                    "range" to rangeCode
                )
            )
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            val d = res.dataObject
            if (!res.isSuccess || d == null) {
                tvCompareResult.visibility = View.VISIBLE
                tvCompareResult.text = res.errorMessage ?: "तुलना डेटा प्राप्त करने में समस्या आई।"
                return@launch
            }

            val objA = d.optJSONObject("mandi_a")
            val objB = d.optJSONObject("mandi_b")
            val seriesA = objA?.optJSONArray("series") ?: JSONArray()
            val seriesB = objB?.optJSONArray("series") ?: JSONArray()
            val dateWiseArr = d.optJSONArray("date_wise_comparison") ?: JSONArray()
            val recHi = d.optString("recommendation_hi", "")

            tvCompareResult.visibility = View.VISIBLE
            tvCompareResult.text = "⚖️ $recHi"

            if (seriesA.length() > 0 || seriesB.length() > 0) {
                val dualChart = MultiPointLineChartView(ctx, seriesA, seriesB, mandiA, mandiB)
                dualChart.layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(230)
                )
                compareChartContainer.addView(dualChart)
            }

            if (dateWiseArr.length() > 0) {
                compareDateTableContainer.addView(
                    TextView(ctx).apply {
                        text = "📅 तारीख-वार तुलना ($mandiA बनाम $mandiB — ${dateWiseArr.length()} तारीखें):"
                        setTextColor(Color.parseColor("#0D47A1"))
                        setTypeface(typeface, Typeface.BOLD)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                        setPadding(0, dp(8), 0, dp(4))
                    }
                )
                for (i in dateWiseArr.length() - 1 downTo 0) {
                    val row = dateWiseArr.optJSONObject(i) ?: continue
                    val dt = row.optString("date", "")
                    val pA = if (row.isNull("mandi_a_modal_price")) "N/A" else "₹${row.optInt("mandi_a_modal_price")}"
                    val pB = if (row.isNull("mandi_b_modal_price")) "N/A" else "₹${row.optInt("mandi_b_modal_price")}"
                    val diff = if (row.isNull("difference_rupees")) "" else " (अंतर: ₹${row.optInt("difference_rupees")})"

                    compareDateTableContainer.addView(
                        TextView(ctx).apply {
                            text = "• $dt : $mandiA = $pA  |  $mandiB = $pB$diff"
                            setTextColor(Color.parseColor("#333333"))
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                            setPadding(dp(4), dp(3), dp(4), dp(3))
                        }
                    )
                }
            }
        }
    }

    // ========================================================================
    // SECTION 4 & 5: AI ADVISOR & SPELLING MATCHER
    // ========================================================================

    private fun runAiSellingAdvisor() {
        val ctx = context ?: return
        val st = inputAdvisorState.text?.toString()?.trim() ?: ""
        val dist = inputAdvisorDistrict.text?.toString()?.trim() ?: ""
        val comm = extractCleanNameIfJson(inputAdvisorCommodity.text?.toString() ?: "", "commodity")
        val qty = inputAdvisorQuantity.text?.toString()?.toDoubleOrNull() ?: 10.0

        if (comm.isBlank()) {
            Toast.makeText(ctx, "कृपया बेचने वाली फसल का नाम लिखें।", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.getAiMandiSellingAdvice(ctx, st, dist, comm, qty)
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            tvAdvisorOutput.visibility = View.VISIBLE
            tvAdvisorOutput.text = res.dataObject?.optString("advice_hi")
                ?: res.errorMessage
                ?: "सलाह उपलब्ध नहीं हो सकी।"
        }
    }

    private fun runSpellingMatcher() {
        val ctx = context ?: return
        val term = inputSpellingTerm.text?.toString()?.trim() ?: ""
        if (term.isBlank()) {
            Toast.makeText(ctx, "कृपया कोई शब्द लिखें।", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.matchMandiSpelling(ctx, term, "commodity")
            if (!isAdded) return@launch
            val matched = res.dataObject?.optString("matched_official_name", term) ?: term
            tvSpellingResult.visibility = View.VISIBLE
            tvSpellingResult.text = "✅ सरकारी पोर्टल नाम: '$matched'"
            inputTrendCommodity.setText(matched)
            inputCompareCommodity.setText(matched)
            inputAdvisorCommodity.setText(matched)
        }
    }

    private fun extractCleanNameIfJson(raw: String, key: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith("{") && trimmed.contains(":")) {
            try {
                val obj = JSONObject(trimmed)
                return obj.optString(key, obj.optString("name", obj.optString("commodity", ""))).trim()
            } catch (_: Exception) {}
        }
        return trimmed
    }

    // ========================================================================
    // CUSTOM MULTI-POINT CANVAS CHART (PLOTS ALL DATES WITH REALISTIC SCALE)
    // ========================================================================

    private inner class MultiPointLineChartView(
        context: Context,
        private val seriesA: JSONArray,
        private val seriesB: JSONArray?,
        private val labelA: String,
        private val labelB: String?
    ) : View(context) {

        private val linePaintA = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1B5E20")
            strokeWidth = dp(3).toFloat()
            style = Paint.Style.STROKE
        }
        private val linePaintB = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0D47A1")
            strokeWidth = dp(3).toFloat()
            style = Paint.Style.STROKE
        }
        private val dotPaintA = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1B5E20")
            style = Paint.Style.FILL
        }
        private val dotPaintB = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0D47A1")
            style = Paint.Style.FILL
        }
        private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0E8E0")
            strokeWidth = dp(1).toFloat()
        }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#263238")
            textSize = dp(11).toFloat()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawColor(Color.parseColor("#FAFAFA"))

            val padLeft = dp(48).toFloat()
            val padRight = dp(24).toFloat()
            val padTop = dp(28).toFloat()
            val padBottom = dp(36).toFloat()

            val chartW = width - padLeft - padRight
            val chartH = height - padTop - padBottom
            if (chartW <= 0 || chartH <= 0) return

            // Collect all date keys and price values
            val dateSet = sortedSetOf<String>()
            val mapA = mutableMapOf<String, Float>()
            val mapB = mutableMapOf<String, Float>()
            val allValues = mutableListOf<Float>()

            for (i in 0 until seriesA.length()) {
                val obj = seriesA.optJSONObject(i) ?: continue
                val d = obj.optString("date", obj.optString("iso_date", ""))
                val v = obj.optDouble("modal_price", 0.0).toFloat()
                if (d.isNotBlank() && v > 0f) {
                    dateSet.add(d)
                    mapA[d] = v
                    allValues.add(v)
                }
            }

            if (seriesB != null) {
                for (i in 0 until seriesB.length()) {
                    val obj = seriesB.optJSONObject(i) ?: continue
                    val d = obj.optString("date", obj.optString("iso_date", ""))
                    val v = obj.optDouble("modal_price", 0.0).toFloat()
                    if (d.isNotBlank() && v > 0f) {
                        dateSet.add(d)
                        mapB[d] = v
                        allValues.add(v)
                    }
                }
            }

            val dates = dateSet.toList()
            if (dates.isEmpty() || allValues.isEmpty()) return

            val rawMin = allValues.minOrNull() ?: 0f
            val rawMax = allValues.maxOrNull() ?: 100f
            val avg = allValues.average().toFloat()

            // Prevent tiny differences (e.g. ₹43 on ₹7000) from looking like a vertical rocket
            val minPaddingBand = maxOf((rawMax - rawMin) * 0.25f, avg * 0.06f, 100f)
            val yMin = (rawMin - minPaddingBand).coerceAtLeast(0f)
            val yMax = rawMax + minPaddingBand

            // Draw horizontal grid lines
            for (g in 0..3) {
                val ratio = g / 3f
                val y = padTop + chartH * (1f - ratio)
                canvas.drawLine(padLeft, y, width - padRight, y, gridPaint)
                val priceLabel = "₹${Math.round(yMin + (yMax - yMin) * ratio)}"
                canvas.drawText(priceLabel, dp(4).toFloat(), y + dp(4).toFloat(), textPaint)
            }

            fun xForIndex(idx: Int): Float =
                if (dates.size == 1) padLeft + chartW / 2f
                else padLeft + (idx.toFloat() / (dates.size - 1).toFloat()) * chartW

            fun yForPrice(price: Float): Float =
                padTop + chartH * (1f - ((price - yMin) / (yMax - yMin).coerceAtLeast(1f)))

            // Draw Series A path & points
            val pathA = Path()
            var startedA = false
            for (i in dates.indices) {
                val v = mapA[dates[i]] ?: continue
                val x = xForIndex(i)
                val y = yForPrice(v)
                if (!startedA) {
                    pathA.moveTo(x, y)
                    startedA = true
                } else {
                    pathA.lineTo(x, y)
                }
                canvas.drawCircle(x, y, dp(4).toFloat(), dotPaintA)
            }
            if (startedA) canvas.drawPath(pathA, linePaintA)

            // Draw Series B path & points (if comparing 2 mandis)
            if (seriesB != null) {
                val pathB = Path()
                var startedB = false
                for (i in dates.indices) {
                    val v = mapB[dates[i]] ?: continue
                    val x = xForIndex(i)
                    val y = yForPrice(v)
                    if (!startedB) {
                        pathB.moveTo(x, y)
                        startedB = true
                    } else {
                        pathB.lineTo(x, y)
                    }
                    canvas.drawCircle(x, y, dp(4).toFloat(), dotPaintB)
                }
                if (startedB) canvas.drawPath(pathB, linePaintB)
            }

            // Draw X-axis start, middle, and end date labels
            val firstDate = dates.first()
            val lastDate = dates.last()
            canvas.drawText(firstDate, padLeft, height - dp(8).toFloat(), textPaint)
            if (dates.size > 2) {
                val midDate = dates[dates.size / 2]
                canvas.drawText(midDate, padLeft + chartW * 0.42f, height - dp(8).toFloat(), textPaint)
            }
            if (dates.size > 1) {
                canvas.drawText(lastDate, width - padRight - dp(72).toFloat(), height - dp(8).toFloat(), textPaint)
            }

            // Legend at top
            val legend = if (labelB != null) "🟢 $labelA   🔵 $labelB (${dates.size} तारीखें)" else "🟢 $labelA (${dates.size} उपलब्ध तारीखें)"
            canvas.drawText(legend, padLeft, dp(18).toFloat(), textPaint)
        }
    }

    private fun createSectionCard(): LinearLayout {
        val ctx = requireContext()
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#FFFFFF", "#D4E2D4", dp(14))
            setPadding(dp(16), dp(14), dp(16), dp(14))
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
            layoutParams = lp
        }
    }

    private fun createHeading(textStr: String): TextView =
        TextView(requireContext()).apply {
            text = textStr
            setTextColor(Color.parseColor("#124116"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(6))
        }

    private fun createLabel(textStr: String): TextView =
        TextView(requireContext()).apply {
            text = textStr
            setTextColor(Color.parseColor("#1A241A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(3))
        }

    private fun createEditText(hintStr: String, isNumber: Boolean = false): EditText =
        EditText(requireContext()).apply {
            hint = hintStr
            setTextColor(Color.parseColor("#1A241A"))
            setHintTextColor(Color.parseColor("#757575"))
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
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            background = roundedBg(bgHex, bgHex, dp(10))
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
