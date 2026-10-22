// app/src/main/java/in/merakisan/app/ui/mandi/MandiPricesFragment.kt
package `in`.merakisan.app.ui.mandi

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import `in`.merakisan.app.core.config.FeatureManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Fixes Video Bug #2 (00:09-00:40):
 * 1. Fixes invisible white-on-white Spinner dropdown items by enforcing dark text (#212121) on white background.
 * 2. Removes all 10 hard-coded dummy mandi rates (100% Zero Dummy Data).
 * 3. Fetches on-demand from data.gov.in (/api/v1/mandi-prices) and caches only searched Mandis on the user's phone.
 */
class MandiPricesFragment : Fragment() {

    private lateinit var spinnerDistrict: Spinner
    private lateinit var spinnerCategory: Spinner
    private lateinit var etSearchCrop: EditText
    private lateinit var tvStatusBanner: TextView
    private lateinit var ratesListContainer: LinearLayout

    private var selectedDistrictFilter = "सभी ज़िले (All)"
    private var selectedCategoryFilter = "सभी फसलें (All)"
    private val liveMandiItems = mutableListOf<MandiRateItem>()
    private var dataSourceLabel = "लाइव API"

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
            setPadding(dp(6), dp(2), dp(12), dp(2))
            setOnClickListener {
                try { findNavController().popBackStack() } catch (_: Throwable) {}
            }
        })

        val titleCol = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleCol.addView(TextView(ctx).apply {
            text = "📈 लाइव APMC मंडी भाव (data.gov.in)"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
        })
        titleCol.addView(TextView(ctx).apply {
            text = "ऑन-डिमांड लाइव API • केवल सर्च की गई मंडी मोबाइल कैश में"
            setTextColor(Color.parseColor("#C8E6C9"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        })
        header.addView(titleCol)

        header.addView(Button(ctx).apply {
            text = "🔄 ताज़ा करें"
            textSize = 11f
            setTextColor(Color.parseColor("#1B5E20"))
            setBackgroundColor(Color.WHITE)
            setOnClickListener { fetchLiveMandiRatesFromApi(ctx, true) }
        })
        root.addView(header)

        val scroll = ScrollView(ctx).apply {
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        val content = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(24))
        }

        // 2. HIGH-CONTRAST CASCADING FILTERS CARD (Fixes White-on-White Dropdown Bug)
        val filterCard = CardView(ctx).apply {
            radius = dp(12).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
        }

        val filterBox = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        filterBox.addView(TextView(ctx).apply {
            text = "🔍 ज़िला/मंडी और फसल श्रेणी चुनें:"
            setTextColor(Color.parseColor("#1B5E20"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(typeface, Typeface.BOLD)
        })

        val spinnerRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 2f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6); bottomMargin = dp(6) }
        }

        val districts = arrayOf(
            "सभी ज़िले (All)",
            "Mandsaur (मंदसौर)",
            "Neemuch (नीमच)",
            "Indore (इंदौर)",
            "Ratlam (रतलाम)",
            "Ujjain (उज्जैन)",
            "Sehore (सीहोर)",
            "Dewas (देवास)",
            "Kota (कोटा)"
        )
        spinnerDistrict = Spinner(ctx).apply {
            adapter = createHighContrastSpinnerAdapter(ctx, districts)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F1F8E9"))
                cornerRadius = dp(8).toFloat()
                setStroke(dp(1), Color.parseColor("#2E7D32"))
            }
            layoutParams = LinearLayout.LayoutParams(0, dp(42), 1f).apply { rightMargin = dp(4) }
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                    selectedDistrictFilter = districts[pos]
                    loadFromMobileCacheThenFetchLive(ctx)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        val categories = arrayOf(
            "सभी फसलें (All)",
            "Soyabean / तिलहन",
            "Wheat / अनाज",
            "Garlic / मसाले",
            "Gram / दलहन"
        )
        spinnerCategory = Spinner(ctx).apply {
            adapter = createHighContrastSpinnerAdapter(ctx, categories)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F1F8E9"))
                cornerRadius = dp(8).toFloat()
                setStroke(dp(1), Color.parseColor("#2E7D32"))
            }
            layoutParams = LinearLayout.LayoutParams(0, dp(42), 1f).apply { leftMargin = dp(4) }
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                    selectedCategoryFilter = categories[pos]
                    renderFilteredList(ctx)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        spinnerRow.addView(spinnerDistrict)
        spinnerRow.addView(spinnerCategory)
        filterBox.addView(spinnerRow)

        etSearchCrop = EditText(ctx).apply {
            hint = "🔎 फसल या मंडी का नाम लिखें (उदा. Wheat, Soyabean, Garlic)..."
            textSize = 13f
            setTextColor(Color.parseColor("#212121"))
            setHintTextColor(Color.parseColor("#757575"))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    renderFilteredList(ctx)
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        filterBox.addView(etSearchCrop)
        filterCard.addView(filterBox)
        content.addView(filterCard)

        tvStatusBanner = TextView(ctx).apply {
            text = "⏳ लाइव मंडी डेटा जाँचा जा रहा है..."
            setTextColor(Color.parseColor("#1B5E20"))
            setBackgroundColor(Color.parseColor("#E8F5E9"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
        }
        content.addView(tvStatusBanner)

        ratesListContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }
        content.addView(ratesListContainer)

        scroll.addView(content)
        root.addView(scroll)

        return root
    }

    /**
     * Enforces dark readable text (#212121) on white background (#FFFFFF)
     * so dropdown options are never invisible white-on-white like in video 00:16-00:20.
     */
    private fun createHighContrastSpinnerAdapter(ctx: Context, items: Array<String>): ArrayAdapter<String> {
        return object : ArrayAdapter<String>(ctx, android.R.layout.simple_spinner_item, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val tv = super.getView(position, convertView, parent) as TextView
                tv.setTextColor(Color.parseColor("#1B5E20"))
                tv.setTypeface(tv.typeface, Typeface.BOLD)
                tv.textSize = 12f
                tv.setPadding(dp(8), dp(6), dp(8), dp(6))
                return tv
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val tv = super.getDropDownView(position, convertView, parent) as TextView
                tv.setBackgroundColor(Color.WHITE)
                tv.setTextColor(Color.parseColor("#212121"))
                tv.setTypeface(tv.typeface, Typeface.BOLD)
                tv.textSize = 14f
                tv.setPadding(dp(14), dp(12), dp(14), dp(12))
                return tv
            }
        }
    }

    private fun loadFromMobileCacheThenFetchLive(ctx: Context) {
        val rawDist = selectedDistrictFilter.substringBefore(" ").trim()
        val cacheKey = if (selectedDistrictFilter.startsWith("सभी")) "all" else rawDist
        val cachedArr = FeatureManager.getFrequentMandiCache(ctx, cacheKey)

        liveMandiItems.clear()
        if (cachedArr.length() > 0) {
            parseJsonArrayIntoList(cachedArr)
            dataSourceLabel = "📱 मोबाइल कैश से ($cacheKey)"
            renderFilteredList(ctx)
        }

        fetchLiveMandiRatesFromApi(ctx, false)
    }

    private fun fetchLiveMandiRatesFromApi(ctx: Context, userTriggered: Boolean) {
        val rawDist = selectedDistrictFilter.substringBefore(" ").trim()
        val cacheKey = if (selectedDistrictFilter.startsWith("सभी")) "all" else rawDist
        tvStatusBanner.text = "🔄 लाइव API (data.gov.in) से '$cacheKey' का ताज़ा भाव मंगाया जा रहा है..."

        CoroutineScope(Dispatchers.IO).launch {
            var serverRatesArray: JSONArray? = null
            var apiMessage = ""

            try {
                val distParam = if (selectedDistrictFilter.startsWith("सभी")) "" else URLEncoder.encode(rawDist, "UTF-8")
                val url = URL("https://mera-kisan-backend.vercel.app/api/v1/mandi-prices?district=$distParam")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 7000
                conn.readTimeout = 7000

                if (conn.responseCode in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val root = JSONObject(body)
                    val dataNode = root.opt("data")
                    if (dataNode is JSONObject) {
                        apiMessage = dataNode.optString("message", "")
                        val src = dataNode.optString("source", "")
                        // Reject any old backend fallback cache so dummy data never enters the app
                        if (src != "regional_fallback_cache") {
                            serverRatesArray = dataNode.optJSONArray("rates")
                        }
                    }
                }
            } catch (e: Throwable) {
                apiMessage = e.message ?: ""
            }

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext

                if (serverRatesArray != null && serverRatesArray.length() > 0) {
                    parseJsonArrayIntoList(serverRatesArray)
                    dataSourceLabel = "🟢 लाइव API (data.gov.in — बिना Firebase स्टोरेज)"
                    FeatureManager.saveFrequentMandiCache(ctx, cacheKey, serverRatesArray)
                    renderFilteredList(ctx)
                    if (userTriggered) {
                        Toast.makeText(ctx, "✅ ${liveMandiItems.size} लाइव मंडी भाव प्राप्त हुए!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    if (liveMandiItems.isEmpty()) {
                        tvStatusBanner.text = if (apiMessage.isNotEmpty()) {
                            "ℹ️ $apiMessage"
                        } else {
                            "ℹ️ लाइव मंडी भाव के लिए एडमिन पैनल (Tab 4 -> MANDI_API) में data.gov.in की API Key सेव करें।"
                        }
                    }
                    renderFilteredList(ctx)
                }
            }
        }
    }

    private fun parseJsonArrayIntoList(arr: JSONArray) {
        liveMandiItems.clear()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            liveMandiItems.add(
                MandiRateItem(
                    commodity = obj.optString("commodity", obj.optString("crop_name", "फसल")),
                    category = obj.optString("category", "कृषि उपज"),
                    mandi = obj.optString("mandi", obj.optString("market", "मंडी")),
                    district = obj.optString("district", ""),
                    minPrice = obj.optInt("min_price", obj.optInt("min", 0)),
                    modalPrice = obj.optInt("modal_price", obj.optInt("modal", 0)),
                    maxPrice = obj.optInt("max_price", obj.optInt("max", 0)),
                    dateLabel = obj.optString("arrival_date", obj.optString("date", "आज"))
                )
            )
        }
    }

    private fun renderFilteredList(ctx: Context) {
        ratesListContainer.removeAllViews()
        val query = etSearchCrop.text.toString().trim().lowercase()
        val catPrefix = selectedCategoryFilter.substringBefore("/").trim().lowercase()

        val filtered = liveMandiItems.filter { item ->
            val matchQuery = query.isEmpty() ||
                item.commodity.lowercase().contains(query) ||
                item.mandi.lowercase().contains(query) ||
                item.district.lowercase().contains(query)

            val matchCat = selectedCategoryFilter.startsWith("सभी") ||
                item.commodity.lowercase().contains(catPrefix) ||
                item.category.lowercase().contains(catPrefix)

            matchQuery && matchCat
        }

        if (filtered.isNotEmpty()) {
            tvStatusBanner.text = "$dataSourceLabel • कुल ${filtered.size} परिणाम"
        }

        if (filtered.isEmpty()) {
            ratesListContainer.addView(TextView(ctx).apply {
                text = "📭 अभी कोई मंडी भाव लोड नहीं है (डमी डेटा 100% बंद है)।\n\n" +
                       "• एडमिन पैनल के Tab 4 (MANDI_API) में अपनी data.gov.in की API Key पेस्ट करके सेव करें और यहाँ '🔄 ताज़ा करें' दबाएं।\n" +
                       "• जो मंडी आप सर्च करेंगे, केवल उसका डेटा आपके मोबाइल कैश में सेव होगा (हमारे Firebase पर 0 MB रहेगा)।"
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#616161"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(dp(18), dp(32), dp(18), dp(32))
            })
            return
        }

        for (item in filtered) {
            val mandiPerKg = String.format("%.1f", item.modalPrice / 100.0)
            val suggestedDirectQuintal = item.modalPrice + 550
            val suggestedDirectPerKg = String.format("%.1f", suggestedDirectQuintal / 100.0)

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
                text = "${item.commodity}  •  [${item.dateLabel}]"
                setTextColor(Color.parseColor("#212121"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setTypeface(typeface, Typeface.BOLD)
            })

            box.addView(TextView(ctx).apply {
                text = "📍 ${item.mandi} (${item.district})"
                setTextColor(Color.parseColor("#616161"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, dp(2), 0, dp(6))
            })

            box.addView(TextView(ctx).apply {
                text = "मॉडल भाव: ₹${item.modalPrice} / क्विंटल (₹$mandiPerKg/kg)\n" +
                       "न्यूनतम: ₹${item.minPrice}  •  अधिकतम: ₹${item.maxPrice} प्रति क्विंटल"
                setTextColor(Color.parseColor("#1B5E20"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTypeface(typeface, Typeface.BOLD)
            })

            box.addView(TextView(ctx).apply {
                text = "💡 सीधा विक्रय सुझाव: ~₹$suggestedDirectPerKg/kg (₹$suggestedDirectQuintal/क्विंटल)"
                setTextColor(Color.parseColor("#0D47A1"))
                setBackgroundColor(Color.parseColor("#E3F2FD"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(dp(10), dp(6), dp(10), dp(6))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(8) }
            })

            card.addView(box)
            ratesListContainer.addView(card)
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    data class MandiRateItem(
        val commodity: String,
        val category: String,
        val mandi: String,
        val district: String,
        val minPrice: Int,
        val modalPrice: Int,
        val maxPrice: Int,
        val dateLabel: String
    )
}
