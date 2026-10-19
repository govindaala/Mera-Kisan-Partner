// app/src/main/java/in/merakisan/app/ui/notifications/NotificationsFavoritesFragment.kt
package `in`.merakisan.app.ui.notifications

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

/**
 * Master Spec Sec 9 & merged.txt Sec 15, 16, 21, 40, 42:
 * Central Notification Center, Favorite Products & Followed Farmers.
 */
class NotificationsFavoritesFragment : Fragment() {

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
            setPadding(dp(12), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(58))
        }

        header.addView(TextView(ctx).apply {
            text = "←"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(6), dp(2), dp(14), dp(2))
            setOnClickListener {
                try { findNavController().popBackStack() } catch (_: Throwable) {}
            }
        })

        header.addView(TextView(ctx).apply {
            text = "🔔 सूचनाएं, पसंदीदा व फॉलो किए किसान"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
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
            setPadding(dp(12), dp(12), dp(12), dp(24))
        }

        val notifications = listOf(
            "📢 नई ग्राहक मांग (Buyer Request): मंदसौर में 100 kg बिना पॉलिश सोयाबीन की मांग पोस्ट हुई है।",
            "🌾 फॉलो किए किसान अपडेट: किसान रामलाल पाटीदार (गरोठ) ने नई ताज़ा शरबती गेहूं की फसल डाली है।",
            "🤝 भाव प्रस्ताव (Offer Update): अनिल शर्मा ने सोयाबीन लॉट पर ₹46/kg का काउंटर-ऑफर स्वीकार किया।",
            "🔔 भाव अलर्ट (Price Alert): आपकी सेव्ड सर्च के अनुसार देशी गेहूं ₹38/kg पर उपलब्ध है।"
        )

        for (msg in notifications) {
            val card = CardView(ctx).apply {
                radius = dp(10).toFloat()
                cardElevation = dp(2).toFloat()
                setCardBackgroundColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(10) }
            }
            card.addView(TextView(ctx).apply {
                text = msg
                setTextColor(Color.parseColor("#212121"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(dp(14), dp(12), dp(14), dp(12))
            })
            content.addView(card)
        }

        scroll.addView(content)
        root.addView(scroll)
        return root
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
