// app/src/main/java/in/merakisan/app/ui/modules/FarmBoxGroupBuyFragment.kt
package `in`.merakisan.app.ui.modules

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
import `in`.merakisan.app.core.config.FeatureManager

/**
 * merged.txt Sec 6, 7, 8, 38, 39, 40, 45, 46, 47:
 * 1. Village Fresh Farm Box (Multi-farmer small quantities combined)
 * 2. Group Buying (20 urban families pooling order)
 * 3. Harvest Pre-Order (Booking crop 15–30 days before harvest)
 */
class FarmBoxGroupBuyFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val boxPrice = FeatureManager.getFarmBoxPrice(ctx)

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
            setPadding(dp(6), dp(2), dp(14), dp(2))
            setOnClickListener {
                try { findNavController().popBackStack() } catch (_: Throwable) {}
            }
        })

        header.addView(TextView(ctx).apply {
            text = "🧺 फार्म बॉक्स, ग्रुप बाइंग व प्री-ऑर्डर"
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

        // SECTION 1: VILLAGE FRESH FARM BOX
        content.addView(createModuleCard(
            ctx = ctx,
            badge = "🧺 मल्टी-फार्मर कॉम्बो (5 छोटे किसानों की संयुक्त उपज)",
            title = "ग्राम ताज़ा फार्म बॉक्स (Village Fresh Box) — ₹$boxPrice",
            details = "• देशी टमाटर (2 kg — किसान मोहनलाल)\n" +
                      "• ताज़ा बैंगन व हरी पालक (2 kg — किसान रामलाल)\n" +
                      "• बिना पॉलिश तुअर दाल (1 kg — किसान गोविंद पाटीदार)\n" +
                      "• देशी मूंगफली दाना (500 g) + देशी गुड़ (500 g)\n" +
                      "📍 डिलीवरी: रविवार सुबह 10–12 बजे (सिटी कलेक्शन पॉइंट / होम डिलीवरी)",
            buttonText = "🛒 ₹$boxPrice में फार्म बॉक्स बुक करें",
            buttonColor = "#1B5E20"
        ) {
            Toast.makeText(ctx, "✅ आपका ग्राम ताज़ा फार्म बॉक्स (₹$boxPrice) बुक हो गया!", Toast.LENGTH_LONG).show()
        })

        // SECTION 2: GROUP BUYING (20 Families Collective Order)
        content.addView(createModuleCard(
            ctx = ctx,
            badge = "👨‍👩‍👧‍👦 सामूहिक खरीद (Group Buying — 20 शहरी परिवार मिलकर)",
            title = "देशी शरबती गेहूं व मूंगफली सामूहिक लॉट (₹34 / kg)",
            details = "• कुल लॉट लक्ष्य: 500 kg (सामान्य भाव: ₹38/kg → ग्रुप भाव: ₹34/kg)\n" +
                      "• वर्तमान प्रगति: 17 परिवारों ने 420 kg बुक कर लिया है\n" +
                      "• लक्ष्य पूरा होने में केवल 80 kg शेष! टारगेट पूरा होते ही खेत से सीधा पिकअप।",
            buttonText = "🤝 ग्रुप ऑर्डर में 20 kg जोड़ें",
            buttonColor = "#0D47A1"
        ) {
            Toast.makeText(ctx, "✅ आपका 20 kg हिस्सा सामूहिक खरीद लॉट में जुड़ गया (440/500 kg पूर्ण)!", Toast.LENGTH_LONG).show()
        })

        // SECTION 3: HARVEST PRE-ORDER (Book Before Harvest)
        content.addView(createModuleCard(
            ctx = ctx,
            badge = "🌱 हार्वेस्ट प्री-बुकिंग (कटाई से 15 दिन पहले रिजर्व करें)",
            title = "नई देशी मूंगफली व ताज़ा लहसुन (15 दिन बाद कटाई)",
            details = "• किसान: गोविंद पाटीदार (ग्राम गरोठ, मंदसौर)\n" +
                      "• कुल अनुमानित उपज: 200 kg • अपेक्षित दर: ₹56 / kg\n" +
                      "• कटाई होते ही आपको तुरंत नोटिफिकेशन मिलेगा।",
            buttonText = "🗓️ 10 kg अग्रिम रिजर्व करें (Pre-Book)",
            buttonColor = "#E65100"
        ) {
            Toast.makeText(ctx, "✅ आपकी 10 kg प्री-बुकिंग दर्ज हो गई! फसल कटते ही अलर्ट मिलेगा।", Toast.LENGTH_LONG).show()
        })

        scroll.addView(content)
        root.addView(scroll)
        return root
    }

    private fun createModuleCard(
        ctx: Context,
        badge: String,
        title: String,
        details: String,
        buttonText: String,
        buttonColor: String,
        onAction: () -> Unit
    ): CardView {
        return CardView(ctx).apply {
            radius = dp(12).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(14) }

            val box = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
            }

            box.addView(TextView(ctx).apply {
                text = badge
                setTextColor(Color.parseColor(buttonColor))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setTypeface(typeface, Typeface.BOLD)
            })

            box.addView(TextView(ctx).apply {
                text = title
                setTextColor(Color.parseColor("#212121"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dp(4), 0, dp(6))
            })

            box.addView(TextView(ctx).apply {
                text = details
                setTextColor(Color.parseColor("#424242"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(0, 0, 0, dp(10))
            })

            box.addView(Button(ctx).apply {
                text = buttonText
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor(buttonColor))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44))
                setOnClickListener { onAction() }
            })

            addView(box)
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
