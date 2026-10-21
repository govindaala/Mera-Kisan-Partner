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
 * 100% Zero Dummy Data + Stateless API Pass-Through + Mobile Local Cache Only:
 * - Fetches live rates on-demand from /api/v1/mandi-prices (data.gov.in).
 * - Stores nothing in Firebase Firestore.
 * - Caches only the user's top 5 most-searched Mandis locally on the user's phone.
 */
class MandiPricesFragment : Fragment() {

    private lateinit var spinnerDistrict: Spinner
    private lateinit var etSearchCrop: EditText
    private lateinit var tvStatusBanner: TextView
    private lateinit var ratesListContainer: LinearLayout

    private var selectedDistrictFilter = "सभी ज़िले (All)"
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
            text = "ऑन-डिमांड API • मोबाइल स्मार्ट कैश (Firebase स्टोरेज मुक्त)"
            setTextColor(Color.parseColor("#C8E6C9"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        })
        header.addView(titleCol)

        header.addView(Button(ctx).apply {
            text = "🔄 लाइव बुलाएं"
            textSize = 11f
            setTextColor(Color.parseColor("#1B5E20"))
            setBackgroundColor(Color.WHITE)
            setOnClickListener { fetchLiveMandiRatesFromApi(ctx, true) }
        })
        root.addView(header)

        if (!FeatureManager.isFeatureEnabled(ctx, "mandi_price")) {
            root.addView(TextView(ctx).apply {
                text = "⚙️ मंडी भाव मॉड्यूल वर्तमान में एडमिन द्वारा बंद (OFF) रखा गया है।"
                gravity = Gravity.CENTER
                setPadding(dp(20), dp(40), dp(20), dp(40))
            })
            return root
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
            setPadding(dp(12), dp(10), dp(12), dp(24))
        }

        // 2. FILTER & SEARCH BAR
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
            text = "🔍 मंडी / ज़िला चुनें (केवल सर्च की गई मंडी आपके मोबाइल कैश में सेव होगी):"
            setTextColor(Color.parseColor("#1B5E20"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(typeface, Typeface.BOLD)
        })

        val districts = arrayOf(
            "सभी ज़िले (All)",
            "Mandsaur",
            "Neemuch",
            "Indore",
            "Ratlam",
            "Ujjain",
            "Sehore",
            "Dewas",
            "Bhopal",
            "Kota"
        )
        spinnerDistrict = Spinner(ctx).apply {
            adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, districts)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                    selectedDistrictFilter = districts[pos]
                    loadFromMobileCacheThenFetchLive(ctx)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
        filterBox.addView(spinnerDistrict)

        etSearchCrop = EditText(ctx).apply {
            hint = "🔎 फसल या मंडी खोजें (उदा. Wheat, Soyabean, Garlic, Mandsaur)..."
            textSize = 13f
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
            text = "⏳ मंडी डेटा जाँचा जा रहा है..."
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

    private fun loadFromMobileCacheThenFetchLive(ctx: Context) {
        val cacheKey = if (selectedDistrictFilter.startsWith("सभी")) "all" else selectedDistrictFilter
        val cachedArr = FeatureManager.getFrequentMandiCache(ctx, cacheKey)

        if (cachedArr.length() > 0) {
            parseJsonArrayIntoList(cachedArr)
            dataSourceLabel = "📱 आपके मोबाइल कैश से (अंतिम सर्च)"
            renderFilteredList(ctx)
        }

        // Now call live API on-demand
        fetchLiveMandiRatesFromApi(ctx, false)
    }

    private fun fetchLiveMandiRatesFromApi(ctx: Context, userTriggered: Boolean) {
        val cacheKey = if (selectedDistrictFilter.startsWith("सभी")) "all" else selectedDistrictFilter
        tvStatusBanner.text = "🔄 लाइव API (data.gov.in) से '$cacheKey' का ताज़ा भाव मंगाया जा रहा है..."

        CoroutineScope(Dispatchers.IO).launch {
            var serverRatesArray: JSONArray? = null
            var apiStatusMessage = ""

            try {
                val distParam = if (selectedDistrictFilter.startsWith("सभी")) "" else URLEncoder.encode(selectedDistrictFilter, "UTF-8")
                val url = URL("https://mera-kisan-backend.vercel.app/api/v1/mandi-prices?district=$distParam")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 7000
                conn.readTimeout = 7000

                if (conn.responseCode in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val root = JSONObject(body)
                    val dataNode = root.opt("data")
                    if (dataNode is JSONObject) {
                        apiStatusMessage = dataNode.optString("message", "")
                        serverRatesArray = dataNode.optJSONArray("rates")
                    } else if (dataNode is JSONArray) {
                        serverRatesArray = dataNode
                    }
                }
            } catch (e: Throwable) {
                apiStatusMessage = e.message ?: "नेटवर्क त्रुटि"
            }

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext

                if (serverRatesArray != null && serverRatesArray.length() > 0) {
                    parseJsonArrayIntoList(serverRatesArray)
                    dataSourceLabel = "🟢 लाइव API (data.gov.in — बिना Firebase स्टोरेज)"
                    // Save only this searched Mandi's compact result in mobile local cache
                    FeatureManager.saveFrequentMandiCache(ctx, cacheKey, serverRatesArray)
                    renderFilteredList(ctx)
                    if (userTriggered) {
                        Toast.makeText(ctx, "✅ ${liveMandiItems.size} लाइव मंडी भाव प्राप्त हुए और मोबाइल कैश में अपडेट हुए!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    if (liveMandiItems.isEmpty()) {
                        tvStatusBanner.text = if (apiStatusMessage.isNotEmpty()) {
                            "ℹ️ $apiStatusMessage"
                        } else {
                            "ℹ️ अभी इस मंडी का कोई लाइव डेटा नहीं मिला (0 डमी डेटा नियम लागू)।"
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

        val filtered = liveMandiItems.filter { item ->
            query.isEmpty() ||
                item.commodity.lowercase().contains(query) ||
                item.mandi.lowercase().contains(query) ||
                item.district.lowercase().contains(query)
        }

        tvStatusBanner.text = "$dataSourceLabel • कुल ${filtered.size} परिणाम"

        if (filtered.isEmpty()) {
            ratesListContainer.addView(TextView(ctx).apply {
                text = "📭 अभी कोई मंडी भाव उपलब्ध नहीं है (डमी डेटा 100% बंद है)।\n\n" +
                       "• यदि आपने एडमिन पैनल में data.gov.in की API Key डाल दी है, तो ऊपर '🔄 लाइव बुलाएं' दबाएं।\n" +
                       "• जो मंडी आप सर्च करेंगे, उसका डेटा आपके मोबाइल के लोकल कैश में सेव हो जाएगा।"
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#616161"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(dp(18), dp(36), dp(18), dp(36))
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

            val topRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            topRow.addView(TextView(ctx).apply {
                text = item.commodity
                setTextColor(Color.parseColor("#212121"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })

            topRow.addView(TextView(ctx).apply {
                text = item.dateLabel
                setTextColor(Color.parseColor("#1B5E20"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(dp(10), dp(4), dp(10), dp(4))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#E8F5E9"))
                    cornerRadius = dp(8).toFloat()
                }
            })
            box.addView(topRow)

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
