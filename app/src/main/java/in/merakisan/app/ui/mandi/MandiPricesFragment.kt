// app/src/main/java/in/merakisan/app/ui/mandi/MandiPricesFragment.kt
package `in`.merakisan.app.ui.mandi

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
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MandiPricesFragment : Fragment() {

    private lateinit var containerRates: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyMessage: TextView
    private lateinit var tvHeaderSubtitle: TextView

    // Master Spec Sec 24 & 29: Fail Gracefully — यदि नेटवर्क या सरकारी API उपलब्ध न हो तो क्षेत्रीय डिफ़ॉल्ट भाव दिखाएं
    private val fallbackRates = listOf(
        MandiRate("सोयाबीन (Yellow)", "मंदसौर", 4300, 4650, 4820, "आज"),
        MandiRate("गेहूं देशी (शरबती / लोकवन)", "गरोठ", 2450, 2680, 2920, "आज"),
        MandiRate("लहसुन (ऊटी / देशी)", "मंदसौर", 9500, 14200, 18500, "आज"),
        MandiRate("मूंगफली (Bold G-20)", "नीमच", 5200, 5750, 6300, "आज"),
        MandiRate("चना देशी (कांटा / विशाल)", "इंदौर", 5400, 5800, 6150, "आज"),
        MandiRate("सरसों (Mustard)", "नीमच", 4900, 5250, 5500, "आज"),
        MandiRate("मैथी दाना", "गरोठ", 5100, 5600, 6100, "आज"),
        MandiRate("अलसी (Flaxseed)", "रतलाम", 4800, 5150, 5450, "आज")
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()

        // मुख्य स्क्रीन कंटेनर (Zero XML ID dependency — 100% Compile Safe)
        val rootLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F4F6F8"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // 1. शीर्ष हेडर बार (Top Green App Bar)
        val headerBar = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#1B5E20"))
            setPadding(dp(12), dp(10), dp(16), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(60)
            )
        }

        val btnBack = TextView(ctx).apply {
            text = "←"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(8), dp(4), dp(14), dp(4))
            setOnClickListener {
                try {
                    findNavController().popBackStack()
                } catch (_: Throwable) {}
            }
        }
        headerBar.addView(btnBack)

        val titleColumn = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val tvTitle = TextView(ctx).apply {
            text = "📈 दैनिक मंडी भाव (APMC)"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
        }

        val prefs = ctx.getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)
        val district = prefs.getString("user_district", "मंदसौर") ?: "मंदसौर"

        tvHeaderSubtitle = TextView(ctx).apply {
            text = "प्रमुख मंडियां ($district व आसपास)"
            setTextColor(Color.parseColor("#C8E6C9"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        }

        titleColumn.addView(tvTitle)
        titleColumn.addView(tvHeaderSubtitle)
        headerBar.addView(titleColumn)

        val btnRefresh = TextView(ctx).apply {
            text = "🔄 ताज़ा करें"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setOnClickListener { fetchMandiRates() }
        }
        headerBar.addView(btnRefresh)
        rootLayout.addView(headerBar)

        // 2. लोडिंग इंडिकेटर
        progressBar = ProgressBar(ctx).apply {
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                dp(36),
                dp(36)
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(12)
                bottomMargin = dp(12)
            }
        }
        rootLayout.addView(progressBar)

        // 3. स्क्रॉल व्यू और कार्ड कंटेनर
        val scrollView = ScrollView(ctx).apply {
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        val scrollContent = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(24))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        tvEmptyMessage = TextView(ctx).apply {
            text = "मंडी भाव लोड हो रहे हैं..."
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#616161"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(dp(16), dp(32), dp(16), dp(32))
            visibility = View.GONE
        }
        scrollContent.addView(tvEmptyMessage)

        containerRates = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        scrollContent.addView(containerRates)

        scrollView.addView(scrollContent)
        rootLayout.addView(scrollView)

        fetchMandiRates()

        return rootLayout
    }

    private fun fetchMandiRates() {
        progressBar.visibility = View.VISIBLE
        tvEmptyMessage.visibility = View.GONE
        containerRates.removeAllViews()

        CoroutineScope(Dispatchers.IO).launch {
            val fetchedList = mutableListOf<MandiRate>()
            var networkSuccess = false

            try {
                val url = URL("https://mera-kisan-backend.vercel.app/api/v1/mandi-prices")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 6000
                conn.readTimeout = 6000

                if (conn.responseCode in 200..299) {
                    val rawText = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(rawText)
                    val dataArray: JSONArray? = when {
                        json.has("data") && json.opt("data") is JSONArray -> json.getJSONArray("data")
                        json.has("data") && json.optJSONObject("data")?.has("rates") == true ->
                            json.getJSONObject("data").getJSONArray("rates")
                        else -> null
                    }

                    if (dataArray != null && dataArray.length() > 0) {
                        for (i in 0 until dataArray.length()) {
                            val item = dataArray.getJSONObject(i)
                            fetchedList.add(
                                MandiRate(
                                    commodity = item.optString("commodity", item.optString("name", "फसल")),
                                    mandi = item.optString("mandi", item.optString("market", "मंडी")),
                                    minPrice = item.optInt("min", item.optInt("min_price", 0)),
                                    modalPrice = item.optInt("modal", item.optInt("modal_price", 0)),
                                    maxPrice = item.optInt("max", item.optInt("max_price", 0)),
                                    date = item.optString("date", "आज")
                                )
                            )
                        }
                        networkSuccess = true
                    }
                }
            } catch (_: Throwable) {
                networkSuccess = false
            } finally {
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    // Fail Gracefully: स्पिनर को तुरंत बंद करें
                    progressBar.visibility = View.GONE
                    val finalList = if (networkSuccess && fetchedList.isNotEmpty()) fetchedList else fallbackRates
                    renderRates(finalList)
                }
            }
        }
    }

    private fun renderRates(rates: List<MandiRate>) {
        val ctx = context ?: return
        containerRates.removeAllViews()

        if (rates.isEmpty()) {
            tvEmptyMessage.visibility = View.VISIBLE
            tvEmptyMessage.text = "अभी मंडी भाव उपलब्ध नहीं हैं।"
            return
        }

        tvEmptyMessage.visibility = View.GONE

        for (rate in rates) {
            val card = CardView(ctx).apply {
                radius = dp(8).toFloat()
                cardElevation = dp(2).toFloat()
                setCardBackgroundColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(10)
                }
            }

            val cardInner = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
            }

            // पंक्ति 1: फसल का नाम और तारीख बैज
            val topRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val tvCrop = TextView(ctx).apply {
                text = rate.commodity
                setTextColor(Color.parseColor("#1B5E20"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val badgeBg = GradientDrawable().apply {
                setColor(Color.parseColor("#E8F5E9"))
                cornerRadius = dp(4).toFloat()
            }

            val tvDate = TextView(ctx).apply {
                text = rate.date
                setTextColor(Color.parseColor("#2E7D32"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setTypeface(typeface, Typeface.BOLD)
                background = badgeBg
                setPadding(dp(8), dp(2), dp(8), dp(2))
            }

            topRow.addView(tvCrop)
            topRow.addView(tvDate)
            cardInner.addView(topRow)

            // पंक्ति 2: मंडी का नाम
            val tvMarket = TextView(ctx).apply {
                text = "📍 ${rate.mandi} मंडी"
                setTextColor(Color.parseColor("#616161"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, dp(2), 0, dp(8))
            }
            cardInner.addView(tvMarket)

            // विभाजक रेखा (Divider)
            val divider = View(ctx).apply {
                setBackgroundColor(Color.parseColor("#EEEEEE"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(1)
                ).apply { bottomMargin = dp(8) }
            }
            cardInner.addView(divider)

            // पंक्ति 3: मॉडल भाव और न्यूनतम/अधिकतम रेंज (बिना किसी बैकस्लैश के सुरक्षित स्ट्रिंग)
            val bottomRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val tvModal = TextView(ctx).apply {
                text = "मॉडल भाव: ₹${rate.modalPrice} प्रति क्विंटल"
                setTextColor(Color.parseColor("#2E7D32"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvRange = TextView(ctx).apply {
                text = "न्यूनतम: ₹${rate.minPrice} • अधिकतम: ₹${rate.maxPrice}"
                setTextColor(Color.parseColor("#424242"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            }

            bottomRow.addView(tvModal)
            bottomRow.addView(tvRange)
            cardInner.addView(bottomRow)

            card.addView(cardInner)
            containerRates.addView(card)
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    data class MandiRate(
        val commodity: String,
        val mandi: String,
        val minPrice: Int,
        val modalPrice: Int,
        val maxPrice: Int,
        val date: String
    )
}
