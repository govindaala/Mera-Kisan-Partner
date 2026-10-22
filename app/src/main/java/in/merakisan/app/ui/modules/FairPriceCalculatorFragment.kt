// app/src/main/java/in/merakisan/app/ui/modules/FairPriceCalculatorFragment.kt
package `in`.merakisan.app.ui.modules

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
import android.widget.ScrollView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

/**
 * Fixes Video Bug #3 (00:31-00:34):
 * Adds explicit Hindi field labels above every input box in Fair Price Calculator
 * so the farmer clearly knows what each number represents (merged.txt Sec 16, 17, 46, 53).
 */
class FairPriceCalculatorFragment : Fragment() {

    private lateinit var etMandiModal: EditText
    private lateinit var etProductionCost: EditText
    private lateinit var etTransportCost: EditText
    private lateinit var etPackagingCost: EditText
    private lateinit var etDesiredMargin: EditText
    private lateinit var tvResultBox: TextView

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
            text = "🧮 उचित मूल्य कैलकुलेटर (Fair Price Engine)"
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
            setPadding(dp(12), dp(12), dp(12), dp(28))
        }

        val card = CardView(ctx).apply {
            radius = dp(12).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(16))
        }

        box.addView(TextView(ctx).apply {
            text = "मंडी थोक भाव बनाम सीधा ग्राहक विक्रय मूल्य गणना (प्रति क्विंटल / 100 kg):"
            setTextColor(Color.parseColor("#1B5E20"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(10))
        })

        etMandiModal = createLabeledNumberInput(
            ctx,
            box,
            "1. वर्तमान मंडी मॉडल भाव (₹ प्रति क्विंटल / 100 kg):",
            "उदा. 2680",
            "2680"
        )

        etProductionCost = createLabeledNumberInput(
            ctx,
            box,
            "2. आपकी खेती व उत्पादन लागत (₹ प्रति क्विंटल):",
            "उदा. 1950",
            "1950"
        )

        etTransportCost = createLabeledNumberInput(
            ctx,
            box,
            "3. सफाई, ग्रेडिंग व परिवहन खर्च (₹ प्रति क्विंटल):",
            "उदा. 120",
            "120"
        )

        etPackagingCost = createLabeledNumberInput(
            ctx,
            box,
            "4. छोटी पैकिंग / बैग खर्च (₹ प्रति क्विंटल):",
            "उदा. 100",
            "100"
        )

        etDesiredMargin = createLabeledNumberInput(
            ctx,
            box,
            "5. अपेक्षित अतिरिक्त किसान लाभांश (₹ प्रति क्विंटल):",
            "उदा. 450",
            "450"
        )

        val btnCalc = Button(ctx).apply {
            text = "🧮 उचित सीधा मूल्य व किसान शुद्ध आय निकालें"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1B5E20"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(46)
            ).apply { topMargin = dp(12); bottomMargin = dp(12) }
            setOnClickListener { calculateAndShowResult() }
        }
        box.addView(btnCalc)

        tvResultBox = TextView(ctx).apply {
            setTextColor(Color.parseColor("#1B5E20"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E8F5E9"))
                cornerRadius = dp(10).toFloat()
                setStroke(dp(1), Color.parseColor("#A5D6A7"))
            }
        }
        box.addView(tvResultBox)

        card.addView(box)
        content.addView(card)
        scroll.addView(content)
        root.addView(scroll)

        calculateAndShowResult()
        return root
    }

    private fun createLabeledNumberInput(
        ctx: Context,
        parent: LinearLayout,
        labelText: String,
        hintText: String,
        defaultVal: String
    ): EditText {
        parent.addView(TextView(ctx).apply {
            text = labelText
            setTextColor(Color.parseColor("#212121"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(4))
        })

        val input = EditText(ctx).apply {
            hint = hintText
            setText(defaultVal)
            setTextColor(Color.parseColor("#212121"))
            setHintTextColor(Color.parseColor("#757575"))
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FAFAFA"))
                cornerRadius = dp(8).toFloat()
                setStroke(dp(1), Color.parseColor("#BDBDBD"))
            }
        }
        parent.addView(input)
        return input
    }

    private fun calculateAndShowResult() {
        val mandiModal = etMandiModal.text.toString().toDoubleOrNull() ?: 2680.0
        val prodCost = etProductionCost.text.toString().toDoubleOrNull() ?: 1950.0
        val transport = etTransportCost.text.toString().toDoubleOrNull() ?: 120.0
        val packaging = etPackagingCost.text.toString().toDoubleOrNull() ?: 100.0
        val margin = etDesiredMargin.text.toString().toDoubleOrNull() ?: 450.0

        val minSustainableQuintal = prodCost + transport + packaging
        val suggestedDirectQuintal = maxOf(minSustainableQuintal + margin, mandiModal + transport + packaging + margin)
        val farmerNetQuintal = suggestedDirectQuintal - (transport + packaging)
        val extraGainOverMandi = (farmerNetQuintal - mandiModal).coerceAtLeast(0.0)

        val mandiPerKg = String.format("%.1f", mandiModal / 100.0)
        val minPerKg = String.format("%.1f", minSustainableQuintal / 100.0)
        val suggestedPerKg = String.format("%.1f", suggestedDirectQuintal / 100.0)

        tvResultBox.text =
            "📊 मूल्य तुलना परिणाम (Price Discovery):\n\n" +
            "• मंडी थोक भाव: ₹${mandiModal.toInt()}/क्विंटल (₹$mandiPerKg / kg)\n" +
            "• न्यूनतम लागत मूल्य: ₹${minSustainableQuintal.toInt()}/क्विंटल (₹$minPerKg / kg)\n" +
            "• सुझाया गया सीधा ग्राहक मूल्य: ₹${suggestedDirectQuintal.toInt()}/क्विंटल (₹$suggestedPerKg / kg)\n" +
            "• पैकिंग व भाड़ा घटाकर किसान को शुद्ध प्राप्ति: ₹${farmerNetQuintal.toInt()}/क्विंटल\n\n" +
            "✅ सीधा बेचने पर किसान को मंडी से +₹${extraGainOverMandi.toInt()} प्रति क्विंटल अधिक लाभ!"
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
