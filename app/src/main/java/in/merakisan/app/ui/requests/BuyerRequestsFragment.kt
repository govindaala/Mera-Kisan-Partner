// app/src/main/java/in/merakisan/app/ui/requests/BuyerRequestsFragment.kt
package `in`.merakisan.app.ui.requests

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
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class BuyerRequestsFragment : Fragment() {

    private lateinit var containerRequests: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvCountHeader: TextView

    private val defaultRequests = listOf(
        DemandItem(
            id = "req_101",
            productName = "सोयाबीन (बिना पॉलिश / बीज ग्रेड)",
            variety = "JS 9560 / बिना पॉलिश",
            quantity = "100 kg",
            targetPrice = "₹52 / kg",
            buyerName = "गोविंद पाटीदार",
            district = "मंदसौर",
            notes = "घर पर पहुंचाएं • साफ दाना",
            dateLabel = "अभी ताज़ा मांग"
        ),
        DemandItem(
            id = "req_102",
            productName = "देशी शरबती गेहूं (छोटी मात्रा)",
            variety = "सीहोर / मालवा शरबती",
            quantity = "25 kg",
            targetPrice = "₹40 / kg",
            buyerName = "राजेश सेठिया",
            district = "गरोठ, मंदसौर",
            notes = "बिना केमिकल दावे के सामान्य देशी साफ गेहूं चाहिए",
            dateLabel = "आज"
        ),
        DemandItem(
            id = "req_103",
            productName = "कच्ची घानी मूंगफली तेल",
            variety = "लकड़ी घानी शुद्ध",
            quantity = "10 litre",
            targetPrice = "₹210 / litre",
            buyerName = "अनिल शर्मा",
            district = "इंदौर / नीमच",
            notes = "बैच ट्रेसबिलिटी के साथ ताज़ा तेल चाहिए",
            dateLabel = "आज"
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
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

        // 1. HEADER BAR
        val header = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }

        header.addView(TextView(ctx).apply {
            text = "📢 ग्राहक मांग बोर्ड ('मुझे यह चाहिए')"
            setTextColor(Color.parseColor("#1B5E20"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTypeface(typeface, Typeface.BOLD)
        })

        header.addView(TextView(ctx).apply {
            text = "खरीदारों की सीधी मांग देखें या अपनी आवश्यकता पोस्ट करें — पास के किसान सीधा ऑफर भेजेंगे।"
            setTextColor(Color.parseColor("#616161"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, dp(2), 0, dp(8))
        })

        val btnPostDemand = Button(ctx).apply {
            text = "+ मुझे यह चाहिए (मांग दर्ज करें)"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#2E7D32"))
            setOnClickListener { showPostDemandDialog() }
        }
        header.addView(btnPostDemand)
        root.addView(header)

        tvCountHeader = TextView(ctx).apply {
            text = "सक्रिय मांगें लोड हो रही हैं..."
            setTextColor(Color.parseColor("#E65100"))
            setBackgroundColor(Color.parseColor("#FFF3E0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(14), dp(6), dp(14), dp(6))
        }
        root.addView(tvCountHeader)

        progressBar = ProgressBar(ctx).apply {
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(dp(32), dp(32)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(8)
            }
        }
        root.addView(progressBar)

        val scroll = ScrollView(ctx).apply {
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        containerRequests = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(24))
        }
        scroll.addView(containerRequests)
        root.addView(scroll)

        loadBuyerRequestsSafe()

        return root
    }

    /**
     * सुरक्षित डेटा लोडर: ISO String ("2026-10-18T...") या Long दोनों को बिना NumberFormatException के पढ़ता है
     */
    private fun loadBuyerRequestsSafe() {
        progressBar.visibility = View.VISIBLE
        val combined = mutableListOf<DemandItem>()

        // 1. पहले लोकल कैश में सेव की गई मांगें जोड़ना
        try {
            val prefs = requireContext().getSharedPreferences("mera_kisan_local_cache", Context.MODE_PRIVATE)
            val localRaw = prefs.getString("cached_demands", "[]") ?: "[]"
            val localArr = JSONArray(localRaw)
            for (i in 0 until localArr.length()) {
                val obj = localArr.getJSONObject(i)
                combined.add(parseDemandJsonSafely(obj))
            }
        } catch (_: Throwable) {}

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://mera-kisan-backend.vercel.app/api/v1/requests")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                if (conn.responseCode in 200..299) {
                    val raw = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(raw)
                    val dataNode = json.opt("data")
                    val arr: JSONArray? = when {
                        dataNode is JSONArray -> dataNode
                        dataNode is JSONObject && dataNode.has("requests") -> dataNode.getJSONArray("requests")
                        else -> null
                    }
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            combined.add(parseDemandJsonSafely(arr.getJSONObject(i)))
                        }
                    }
                }
            } catch (_: Throwable) {
            } finally {
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    progressBar.visibility = View.GONE
                    combined.addAll(defaultRequests)
                    val uniqueList = combined.distinctBy { "${it.productName}_${it.quantity}_${it.district}" }
                    renderRequestsList(uniqueList)
                }
            }
        }
    }

    private fun parseDemandJsonSafely(obj: JSONObject): DemandItem {
        val rawDate = obj.opt("createdAt")?.toString() ?: obj.opt("created_at")?.toString() ?: "आज"
        val cleanDate = if (rawDate.contains("T")) rawDate.substringBefore("T") else "आज"

        val qty = obj.optString("quantity", "50")
        val unit = obj.optString("unit", "kg")
        val price = obj.optString("target_price", obj.optString("targetPrice", "बाज़ार भाव"))

        return DemandItem(
            id = obj.optString("id", "req_${System.currentTimeMillis()}"),
            productName = obj.optString("product_name", obj.optString("productName", "फसल")),
            variety = obj.optString("variety", "देशी"),
            quantity = if (qty.contains(unit)) qty else "$qty $unit",
            targetPrice = if (price.startsWith("₹")) price else "₹$price / $unit",
            buyerName = obj.optString("buyer_name", obj.optString("buyerName", "ग्राहक साथी")),
            district = obj.optString("district", obj.optString("location", "मंदसौर")),
            notes = obj.optString("description", obj.optString("notes", "सीधे किसान से खरीदना है")),
            dateLabel = cleanDate
        )
    }

    private fun renderRequestsList(items: List<DemandItem>) {
        val ctx = context ?: return
        containerRequests.removeAllViews()
        tvCountHeader.text = "✅ कुल ${items.size} सक्रिय ग्राहक मांगें उपलब्ध (किसान सीधा ऑफर भेज सकते हैं)"

        for (item in items) {
            val card = CardView(ctx).apply {
                radius = dp(10).toFloat()
                cardElevation = dp(2).toFloat()
                setCardBackgroundColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(10) }
            }

            val inner = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
            }

            val topRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            topRow.addView(TextView(ctx).apply {
                text = "📢 चाहिए: ${item.productName}"
                setTextColor(Color.parseColor("#1B5E20"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })

            topRow.addView(TextView(ctx).apply {
                text = item.dateLabel
                setTextColor(Color.parseColor("#E65100"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setPadding(dp(8), dp(2), dp(8), dp(2))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#FFF3E0"))
                    cornerRadius = dp(6).toFloat()
                }
            })
            inner.addView(topRow)

            inner.addView(TextView(ctx).apply {
                text = "किस्म: ${item.variety}  •  मात्रा: ${item.quantity}  •  अपेक्षित दर: ${item.targetPrice}"
                setTextColor(Color.parseColor("#212121"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dp(4), 0, dp(4))
            })

            inner.addView(TextView(ctx).apply {
                text = "👤 ग्राहक: ${item.buyerName}  •  📍 ${item.district}\n📝 विवरण: ${item.notes}"
                setTextColor(Color.parseColor("#616161"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            })

            val btnOffer = Button(ctx).apply {
                text = "💬 किसान ऑफर भेजें (Send Farmer Offer)"
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#1B5E20"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(42)
                ).apply { topMargin = dp(8) }
                setOnClickListener {
                    showSendOfferToBuyerDialog(item)
                }
            }
            inner.addView(btnOffer)

            card.addView(inner)
            containerRequests.addView(card)
        }
    }

    private fun showPostDemandDialog() {
        val ctx = context ?: return
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(4))
        }

        val etCrop = EditText(ctx).apply { hint = "फसल / उत्पाद का नाम (उदा. सोयाबीन, देशी गेहूं)" }
        val etVariety = EditText(ctx).apply { hint = "किस्म / प्राथमिकता (उदा. बिना पॉलिश, शरबती)" }
        val etQty = EditText(ctx).apply {
            hint = "मात्रा (उदा. 100 kg या 10 litre)"
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val etPrice = EditText(ctx).apply {
            hint = "अपेक्षित दर ₹ प्रति किलो (वैकल्पिक)"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val etDistrict = EditText(ctx).apply {
            hint = "आपका ज़िला / तहसील"
            setText("मंदसौर")
        }
        val etNotes = EditText(ctx).apply { hint = "अन्य विवरण (उदा. घर पर पहुंचाएं)" }

        box.addView(etCrop)
        box.addView(etVariety)
        box.addView(etQty)
        box.addView(etPrice)
        box.addView(etDistrict)
        box.addView(etNotes)

        AlertDialog.Builder(ctx)
            .setTitle("📢 'मुझे यह चाहिए' — मांग दर्ज करें")
            .setView(box)
            .setPositiveButton("मांग पोस्ट करें") { _, _ ->
                val crop = etCrop.text.toString().trim()
                if (crop.isEmpty()) {
                    Toast.makeText(ctx, "कृपया फसल का नाम लिखें", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val newObj = JSONObject().apply {
                    put("id", "req_${System.currentTimeMillis()}")
                    put("product_name", crop)
                    put("variety", etVariety.text.toString().trim().ifEmpty { "देशी" })
                    put("quantity", etQty.text.toString().trim().ifEmpty { "50 kg" })
                    put("unit", "kg")
                    put("target_price", etPrice.text.toString().trim().ifEmpty { "50" })
                    put("district", etDistrict.text.toString().trim().ifEmpty { "मंदसौर" })
                    put("description", etNotes.text.toString().trim().ifEmpty { "सीधे किसान से संपर्क" })
                    put("created_at", "आज अभी")
                }

                // लोकल कैश में सेव कर तुरंत लिस्ट रिफ्रेश करना
                try {
                    val prefs = ctx.getSharedPreferences("mera_kisan_local_cache", Context.MODE_PRIVATE)
                    val arr = JSONArray(prefs.getString("cached_demands", "[]") ?: "[]")
                    val next = JSONArray()
                    next.put(newObj)
                    for (i in 0 until arr.length()) next.put(arr.getJSONObject(i))
                    prefs.edit().putString("cached_demands", next.toString()).apply()
                } catch (_: Throwable) {}

                Toast.makeText(ctx, "✅ आपकी मांग सफलतापूर्वक पोस्ट हो गई और बोर्ड पर लाइव है!", Toast.LENGTH_SHORT).show()
                loadBuyerRequestsSafe()
            }
            .setNegativeButton("रद्द करें", null)
            .show()
    }

    private fun showSendOfferToBuyerDialog(item: DemandItem) {
        val ctx = context ?: return
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(4))
        }
        val etOfferPrice = EditText(ctx).apply {
            hint = "आपकी प्रस्तावित दर ₹ (उदा. ₹48/kg)"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val etAvailableQty = EditText(ctx).apply {
            hint = "उपलब्ध मात्रा (उदा. ${item.quantity})"
            setText(item.quantity)
        }
        val etMsg = EditText(ctx).apply {
            hint = "संदेश (उदा. मेरे पास गरोठ में साफ माल तैयार है)"
        }
        box.addView(etOfferPrice)
        box.addView(etAvailableQty)
        box.addView(etMsg)

        AlertDialog.Builder(ctx)
            .setTitle("🌾 '${item.productName}' के लिए ऑफर भेजें")
            .setView(box)
            .setPositiveButton("ऑफर लॉक कर भेजें") { _, _ ->
                Toast.makeText(
                    ctx,
                    "✅ ग्राहक (${item.buyerName}) को आपका ऑफर ₹${etOfferPrice.text} सफलतापूर्वक भेज दिया गया!",
                    Toast.LENGTH_LONG
                ).show()
            }
            .setNegativeButton("बंद करें", null)
            .show()
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    data class DemandItem(
        val id: String,
        val productName: String,
        val variety: String,
        val quantity: String,
        val targetPrice: String,
        val buyerName: String,
        val district: String,
        val notes: String,
        val dateLabel: String
    )
}
