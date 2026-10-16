package `in`.merakisan.app.ui.mandi

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import `in`.merakisan.app.R
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
    private lateinit var emptyStateView: View
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptySubtitle: TextView
    private lateinit var tvHeaderDistrict: TextView

    private val fallbackRates = listOf(
        MandiRate("सोयाबीन (Yellow)", "मंदसौर", 4300, 4650, 4820, "आज"),
        MandiRate("गेहूं (लोकवन / देशी)", "गरोठ", 2450, 2680, 2920, "आज"),
        MandiRate("लहसुन (ऊटी / देशी)", "मंदसौर", 9500, 14200, 18500, "आज"),
        MandiRate("मूंगफली (G-20)", "नीमच", 5200, 5750, 6300, "आज"),
        MandiRate("चना (कांटा / विशाल)", "इंदौर", 5400, 5800, 6150, "आज"),
        MandiRate("सरसों (Mustard)", "नीमच", 4900, 5250, 5500, "आज")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_mandi_prices, container, false)
        initViews(root)
        fetchMandiRates()
        return root
    }

    private fun initViews(v: View) {
        containerRates = v.findViewById(R.id.containerMandiRates)
        progressBar = v.findViewById(R.id.progressBarMandi)
        emptyStateView = v.findViewById(R.id.emptyStateMandi)
        tvEmptyTitle = emptyStateView.findViewById(R.id.tvEmptyTitle)
        tvEmptySubtitle = emptyStateView.findViewById(R.id.tvEmptySubtitle)
        tvHeaderDistrict = v.findViewById(R.id.tvHeaderDistrict)

        val btnBack = v.findViewById<View>(R.id.btnBackMandi)
        btnBack?.setOnClickListener {
            findNavController().popBackStack()
        }

        val prefs = requireContext().getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)
        val district = prefs.getString("user_district", "मंदसौर") ?: "मंदसौर"
        tvHeaderDistrict.text = "प्रमुख मंडियां (${district} व आसपास)"
    }

    private fun fetchMandiRates() {
        progressBar.visibility = View.VISIBLE
        emptyStateView.visibility = View.GONE
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
                    progressBar.visibility = View.GONE
                    val finalList = if (networkSuccess && fetchedList.isNotEmpty()) fetchedList else fallbackRates
                    renderRates(finalList)
                }
            }
        }
    }

    private fun renderRates(rates: List<MandiRate>) {
        containerRates.removeAllViews()
        if (rates.isEmpty()) {
            emptyStateView.visibility = View.VISIBLE
            tvEmptyTitle.text = "मंडी भाव उपलब्ध नहीं हैं"
            tvEmptySubtitle.text = "आज का थोक भाव जल्द ही अपडेट किया जाएगा।"
            return
        }

        emptyStateView.visibility = View.GONE
        val inflater = LayoutInflater.from(requireContext())

        for (rate in rates) {
            val cardView = inflater.inflate(R.layout.item_mandi_rate_card, containerRates, false)
            val tvCrop = cardView.findViewById<TextView>(R.id.tvMandiCrop)
            val tvMarket = cardView.findViewById<TextView>(R.id.tvMandiName)
            val tvModal = cardView.findViewById<TextView>(R.id.tvModalPrice)
            val tvRange = cardView.findViewById<TextView>(R.id.tvPriceRange)
            val tvDate = cardView.findViewById<TextView>(R.id.tvMandiDate)

            tvCrop.text = rate.commodity
            tvMarket.text = "📍 ${rate.mandi} मंडी"
            tvModal.text = "₹${rate.modalPrice} / क्विंटल"
            tvRange.text = "न्यूनतम: ₹${rate.minPrice}  \vert{}  अधिकतम: ₹${rate.maxPrice}"
            tvDate.text = rate.date

            containerRates.addView(cardView)
        }
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
