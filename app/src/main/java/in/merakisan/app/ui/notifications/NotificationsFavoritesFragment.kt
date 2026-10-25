// app/src/main/java/in/merakisan/app/ui/notifications/NotificationsFavoritesFragment.kt
package `in`.merakisan.app.ui.notifications

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.ui.main.MainActivity
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * MERA KISAN — CENTRAL NOTIFICATIONS & ALERTS HUB
 * Exact Repository Path: app/src/main/java/in/merakisan/app/ui/notifications/NotificationsFavoritesFragment.kt
 *
 * Implements (Master Spec Sec 21 & merged.txt Sec 21, 42):
 * - Displays Order updates, New buyer requests, Offer responses, Followed farmer alerts, and System announcements.
 * - Clicking a notification navigates directly to the target entity (Order, Product, Offer, Request).
 */
class NotificationsFavoritesFragment : Fragment() {

    private lateinit var progressBar: ProgressBar
    private lateinit var notificationsListContainer: LinearLayout

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
                text = "🔔 सूचनाएँ व अलर्ट्स (Notifications & Alerts)"
                setTextColor(Color.parseColor("#124116"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                setTypeface(typeface, Typeface.BOLD)
            }
        )

        headerCard.addView(
            TextView(ctx).apply {
                text = "आपके ऑर्डर्स, भाव प्रस्तावों, और पसंदीदा किसानों की नई फसलों के ताज़ा अपडेट्स:"
                setTextColor(Color.parseColor("#4A5A4A"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(0, dp(4), 0, dp(8))
            }
        )

        headerCard.addView(
            Button(ctx).apply {
                text = "🔄 सूचनाएँ रिफ्रेश करें"
                isAllCaps = false
                setTextColor(Color.WHITE)
                setTypeface(typeface, Typeface.BOLD)
                background = roundedBg("#1B5E20", "#1B5E20", dp(8))
                minHeight = dp(40)
                setOnClickListener { loadNotificationsFromServer() }
            }
        )
        root.addView(headerCard)

        progressBar = ProgressBar(ctx).apply {
            visibility = View.GONE
            setPadding(0, dp(12), 0, dp(12))
        }
        root.addView(progressBar)

        notificationsListContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        root.addView(notificationsListContainer)

        scroll.addView(root)
        return scroll
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadNotificationsFromServer()
    }

    private fun loadNotificationsFromServer() {
        val ctx = context ?: return
        progressBar.visibility = View.VISIBLE
        notificationsListContainer.removeAllViews()

        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.request(ctx, "/notifications", method = "GET")
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            val arr = res.dataArray ?: JSONArray()
            if (arr.length() == 0) {
                notificationsListContainer.addView(
                    TextView(ctx).apply {
                        text = "अभी आपके लिए कोई नई सूचना नहीं है।"
                        setTextColor(Color.parseColor("#555555"))
                        gravity = Gravity.CENTER
                        setPadding(dp(12), dp(24), dp(12), dp(24))
                    }
                )
                return@launch
            }

            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                notificationsListContainer.addView(buildNotificationCard(item))
            }
        }
    }

    private fun buildNotificationCard(item: JSONObject): View {
        val ctx = requireContext()
        val title = item.optString("title", "सूचना")
        val body = item.optString("body", item.optString("message", ""))
        val type = item.optString("type", "SYSTEM").uppercase()
        val targetId = item.optString("target_id", "")
        val createdAt = item.optString("created_at", "").take(16).replace("T", " ")
        val isRead = item.optBoolean("is_read", false)

        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg(
                if (isRead) "#FAFAFA" else "#FFFFFF",
                if (isRead) "#E0EAE0" else "#81C784",
                dp(12)
            )
            setPadding(dp(14), dp(12), dp(14), dp(12))
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(8)
            layoutParams = lp

            setOnClickListener {
                handleNotificationClick(type, targetId)
            }
        }

        val typeBadge = when (type) {
            "ORDER" -> "📦 ऑर्डर अपडेट"
            "OFFER" -> "🤝 भाव प्रस्ताव"
            "BUYER_REQUEST" -> "📢 ग्राहक मांग"
            "HARVEST", "NEW_PRODUCT" -> "🌾 नई फसल आवक"
            else -> "🔔 सिस्टम अलर्ट"
        }

        card.addView(
            TextView(ctx).apply {
                text = "$typeBadge • $createdAt"
                setTextColor(Color.parseColor("#2E7D32"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTypeface(typeface, Typeface.BOLD)
            }
        )

        card.addView(
            TextView(ctx).apply {
                text = title
                setTextColor(Color.parseColor("#124116"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dp(2), 0, dp(2))
            }
        )

        card.addView(
            TextView(ctx).apply {
                text = body
                setTextColor(Color.parseColor("#333333"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            }
        )

        return card
    }

    private fun handleNotificationClick(type: String, targetId: String) {
        val act = activity as? MainActivity ?: return
        when (type) {
            "ORDER" -> act.openOrdersScreen()
            "OFFER" -> act.openNegotiationOffersScreen()
            "BUYER_REQUEST" -> act.openBuyerRequestsScreen()
            "HARVEST", "NEW_PRODUCT" -> if (targetId.isNotBlank()) act.openProductDetailScreen(targetId)
            else -> act.openOrdersScreen()
        }
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
