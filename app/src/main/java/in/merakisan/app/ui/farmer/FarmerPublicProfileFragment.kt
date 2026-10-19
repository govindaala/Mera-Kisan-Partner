// app/src/main/java/in/merakisan/app/ui/farmer/FarmerPublicProfileFragment.kt
package `in`.merakisan.app.ui.farmer

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
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

/**
 * merged.txt Sec 6, 34, 35 & 93:
 * Public Farmer Profile with Farm Story, Verification Badges, Privacy-Safe Location,
 * Follow Farmer (Instagram-lite notification trigger), WhatsApp & Call.
 */
class FarmerPublicProfileFragment : Fragment() {

    private var isFollowing = false

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

        // 1. HEADER BAR
        val appBar = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#1B5E20"))
            setPadding(dp(12), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(58))
        }

        appBar.addView(TextView(ctx).apply {
            text = "←"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(6), dp(2), dp(14), dp(2))
            setOnClickListener {
                try { findNavController().popBackStack() } catch (_: Throwable) {}
            }
        })

        appBar.addView(TextView(ctx).apply {
            text = "👨‍🌾 किसान प्रोफ़ाइल व मेरी खेती (Farm Story)"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
        })
        root.addView(appBar)

        val scroll = ScrollView(ctx).apply {
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        val content = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(24))
        }

        // 2. FARMER IDENTITY & TRUST CARD
        val profileCard = CardView(ctx).apply {
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
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        box.addView(TextView(ctx).apply {
            text = "👨‍🌾 रामलाल पाटीदार"
            setTextColor(Color.parseColor("#1B5E20"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTypeface(typeface, Typeface.BOLD)
        })

        box.addView(TextView(ctx).apply {
            text = "📍 ग्राम: गरोठ, ज़िला: मंदसौर (मध्य प्रदेश) — लगभग 7 km दूर\n" +
                   "🏅 सत्यापन: ✔ Mobile Verified  •  ✔ Mera Kisan Verified Farmer\n" +
                   "⭐ रेटिंग: 4.8 / 5.0 (32 सत्यापित ऑर्डर्स)  •  👥 फॉलोअर्स: 148"
            setTextColor(Color.parseColor("#424242"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(6), 0, dp(10))
        })

        val btnFollow = Button(ctx).apply {
            text = "➕ किसान को फॉलो करें (Follow for New Harvest Alerts)"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1B5E20"))
            setOnClickListener {
                isFollowing = !isFollowing
                text = if (isFollowing) "✔ फॉलो किया गया (नई फसल आने पर अलर्ट मिलेगा)" else "➕ किसान को फॉलो करें"
                Toast.makeText(
                    ctx,
                    if (isFollowing) "आपने किसान रामलाल पाटीदार को फॉलो किया!" else "अनफॉलो किया गया",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        box.addView(btnFollow)

        val contactRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 2f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        }

        contactRow.addView(Button(ctx).apply {
            text = "🟢 WhatsApp संपर्क"
            setTextColor(Color.parseColor("#1B5E20"))
            setBackgroundColor(Color.parseColor("#E8F5E9"))
            layoutParams = LinearLayout.LayoutParams(0, dp(42), 1f).apply { rightMargin = dp(4) }
            setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/919876543210"))
                try { startActivity(intent) } catch (_: Throwable) {}
            }
        })

        contactRow.addView(Button(ctx).apply {
            text = "📞 सीधा कॉल करें"
            setTextColor(Color.parseColor("#0D47A1"))
            setBackgroundColor(Color.parseColor("#E3F2FD"))
            layoutParams = LinearLayout.LayoutParams(0, dp(42), 1f).apply { leftMargin = dp(4) }
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:9876543210")))
            }
        })

        box.addView(contactRow)
        profileCard.addView(box)
        content.addView(profileCard)

        // 3. FARM STORY CARD ("मेरी खेती की कहानी")
        val storyCard = CardView(ctx).apply {
            radius = dp(12).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
        }

        val storyBox = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        storyBox.addView(TextView(ctx).apply {
            text = "📖 मेरी खेती की कहानी (Farm Story & Transparency)"
            setTextColor(Color.parseColor("#1B5E20"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
        })

        storyBox.addView(TextView(ctx).apply {
            text = "“मैं रामलाल पाटीदार, मंदसौर के गरोठ क्षेत्र का किसान हूँ। हमारे परिवार में पिछले 18 वर्षों से पारंपरिक देशी शरबती गेहूं, बिना पॉलिश के सोयाबीन और देशी मूंगफली की खेती की जाती है। मंडी में छोटी मात्रा बेचने के बजाय हम सीधे शहरी परिवारों को ताज़ा और साफ अनाज उपलब्ध कराते हैं।”\n\n" +
                   "• मुख्य फसलें: देशी शरबती गेहूं, JS-9560 सोयाबीन, देशी मूंगफली\n" +
                   "• खेती पद्धति: पारंपरिक देशी खाद व नियंत्रित सिंचाई (Farmer Declared)"
            setTextColor(Color.parseColor("#424242"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(6), 0, 0)
        })

        storyCard.addView(storyBox)
        content.addView(storyCard)

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
