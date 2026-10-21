// app/src/main/java/in/merakisan/app/ui/orders/OrdersFragment.kt
package `in`.merakisan.app.ui.orders

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import `in`.merakisan.app.core.utils.SafeDateParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * 100% Zero Dummy Data Orders Screen:
 * - Loads real orders from /api/v1/orders and caches only real orders in mobile local cache.
 */
class OrdersFragment : Fragment() {

    private lateinit var ordersContainer: LinearLayout
    private var activeTab = "BUYER_ORDERS"
    private val liveOrdersList = mutableListOf<OrderUiItem>()

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

        val header = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#1B5E20"))
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(58))
        }

        header.addView(TextView(ctx).apply {
            text = "📦 मेरे ऑर्डर्स (Live Orders Only)"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        header.addView(Button(ctx).apply {
            text = "🔄 रिफ्रेश"
            textSize = 11f
            setTextColor(Color.parseColor("#1B5E20"))
            setBackgroundColor(Color.WHITE)
            setOnClickListener { fetchOrdersFromServer(ctx) }
        })
        root.addView(header)

        val tabRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 2f
            setBackgroundColor(Color.WHITE)
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        tabRow.addView(Button(ctx).apply {
            text = "🛒 मेरी खरीद (Buyer Orders)"
            textSize = 11f
            setTextColor(Color.parseColor("#1B5E20"))
            setBackgroundColor(Color.parseColor("#E8F5E9"))
            layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f).apply { rightMargin = dp(4) }
            setOnClickListener {
                activeTab = "BUYER_ORDERS"
                renderOrders(ctx)
            }
        })

        tabRow.addView(Button(ctx).apply {
            text = "👨‍🌾 मेरी बिक्री (Farmer Sales)"
            textSize = 11f
            setTextColor(Color.parseColor("#0D47A1"))
            setBackgroundColor(Color.parseColor("#E3F2FD"))
            layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f).apply { leftMargin = dp(4) }
            setOnClickListener {
                activeTab = "FARMER_ORDERS"
                renderOrders(ctx)
            }
        })
        root.addView(tabRow)

        val scroll = ScrollView(ctx).apply {
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        ordersContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(24))
        }

        scroll.addView(ordersContainer)
        root.addView(scroll)

        loadMobileCachedOrders(ctx)
        fetchOrdersFromServer(ctx)
        return root
    }

    private fun loadMobileCachedOrders(ctx: Context) {
        try {
            val raw = ctx.getSharedPreferences("mera_kisan_orders_cache", Context.MODE_PRIVATE)
                .getString("real_orders_json", null)
            if (!raw.isNullOrBlank()) {
                parseOrdersArray(JSONArray(raw))
                renderOrders(ctx)
            }
        } catch (_: Throwable) {}
    }

    private fun fetchOrdersFromServer(ctx: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            var serverArr: JSONArray? = null
            try {
                val url = URL("https://mera-kisan-backend.vercel.app/api/v1/orders?limit=40")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                if (conn.responseCode in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    serverArr = JSONObject(body).optJSONArray("data")
                }
            } catch (_: Throwable) {}

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                if (serverArr != null) {
                    ctx.getSharedPreferences("mera_kisan_orders_cache", Context.MODE_PRIVATE)
                        .edit()
                        .putString("real_orders_json", serverArr.toString())
                        .apply()
                    parseOrdersArray(serverArr)
                }
                renderOrders(ctx)
            }
        }
    }

    private fun parseOrdersArray(arr: JSONArray) {
        liveOrdersList.clear()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            liveOrdersList.add(
                OrderUiItem(
                    id = o.optString("id", ""),
                    productName = o.optString("product_name", "कृषि उत्पाद"),
                    quantity = o.optDouble("quantity", 1.0),
                    unit = o.optString("unit", "kg"),
                    totalRupees = o.optDouble("total_rupees", 0.0).toInt(),
                    paymentStatus = o.optString("payment_status", "PENDING"),
                    orderStatus = o.optString("order_status", "PLACED").uppercase(),
                    createdAtDisplay = SafeDateParser.toDisplayDate(o.opt("created_at"))
                )
            )
        }
    }

    private fun renderOrders(ctx: Context) {
        ordersContainer.removeAllViews()

        if (liveOrdersList.isEmpty()) {
            ordersContainer.addView(TextView(ctx).apply {
                text = "📭 अभी आपका कोई वास्तविक ऑर्डर नहीं है (डमी ऑर्डर्स 100% हटा दिए गए हैं)।"
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#616161"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setPadding(dp(20), dp(40), dp(20), dp(40))
            })
            return
        }

        for (ord in liveOrdersList) {
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
                text = "ऑर्डर #${ord.id}  •  तिथि: ${ord.createdAtDisplay}"
                setTextColor(Color.parseColor("#616161"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setTypeface(typeface, Typeface.BOLD)
            })

            box.addView(TextView(ctx).apply {
                text = "🌾 ${ord.productName}"
                setTextColor(Color.parseColor("#212121"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dp(4), 0, dp(2))
            })

            box.addView(TextView(ctx).apply {
                text = "मात्रा: ${ord.quantity.toInt()} ${ord.unit}  •  कुल राशि: ₹${ord.totalRupees}\n" +
                       "पेमेंट: ${ord.paymentStatus}  •  ऑर्डर स्थिति: [${ord.orderStatus}]"
                setTextColor(Color.parseColor("#1B5E20"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dp(2), 0, dp(8))
            })

            val nextAllowedStatus = when (ord.orderStatus) {
                "PLACED" -> "ACCEPTED"
                "ACCEPTED" -> "SHIPPED"
                "SHIPPED" -> "DELIVERED"
                "DELIVERED" -> "COMPLETED"
                else -> null
            }

            val actionRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                weightSum = 2f
            }

            if (nextAllowedStatus != null) {
                actionRow.addView(Button(ctx).apply {
                    text = "✅ स्थिति अपडेट: $nextAllowedStatus"
                    textSize = 10f
                    setTextColor(Color.WHITE)
                    setBackgroundColor(Color.parseColor("#1B5E20"))
                    layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f).apply { rightMargin = dp(4) }
                    setOnClickListener {
                        updateOrderStatusOnServer(ctx, ord, nextAllowedStatus)
                    }
                })
            }

            actionRow.addView(Button(ctx).apply {
                text = if (ord.orderStatus == "COMPLETED" || ord.orderStatus == "DELIVERED") {
                    "⭐ रेटिंग दें / विवाद"
                } else {
                    "⚠️ सहायता / विवाद"
                }
                textSize = 10f
                setTextColor(Color.parseColor("#0D47A1"))
                setBackgroundColor(Color.parseColor("#E3F2FD"))
                layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f).apply { leftMargin = dp(4) }
                setOnClickListener {
                    navigateSafe("disputeAndRatingFragment")
                }
            })

            box.addView(actionRow)
            card.addView(box)
            ordersContainer.addView(card)
        }
    }

    private fun updateOrderStatusOnServer(ctx: Context, ord: OrderUiItem, nextStatus: String) {
        CoroutineScope(Dispatchers.IO).launch {
            var ok = false
            try {
                val url = URL("https://mera-kisan-backend.vercel.app/api/v1/orders/${ord.id}/status")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "PATCH"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                val body = JSONObject().apply { put("status", nextStatus) }
                OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
                ok = conn.responseCode in 200..299
            } catch (_: Throwable) {}

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                if (ok) {
                    ord.orderStatus = nextStatus
                    renderOrders(ctx)
                    Toast.makeText(ctx, "✅ ऑर्डर #${ord.id} '$nextStatus' पर अपडेट हो गया!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(ctx, "⚠️ सर्वर से संपर्क नहीं हो सका।", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun navigateSafe(vararg names: String) {
        val ctx = context ?: return
        val pkg = ctx.packageName
        for (n in names) {
            val id = ctx.resources.getIdentifier(n, "id", pkg)
            if (id != 0) {
                try {
                    findNavController().navigate(id)
                    return
                } catch (_: Throwable) {}
            }
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    data class OrderUiItem(
        val id: String,
        val productName: String,
        val quantity: Double,
        val unit: String,
        val totalRupees: Int,
        val paymentStatus: String,
        var orderStatus: String,
        val createdAtDisplay: String
    )
}
