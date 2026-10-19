// app/src/main/java/in/merakisan/app/ui/modules/FairPriceCalculatorFragment.kt
package `in`.merakisan.app.ui.modules

import android.graphics.Color
import android.graphics.Typeface
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
 * merged.txt Sec 16, 17, 45, 46, 53:
 * Fair Price Calculator & Price Discovery Screen:
 * Compares Mandi Price vs Direct Selling Price with Packaging, Transport & Farmer Net Margin.
 */
class FairPriceCalculatorFragment : Fragment() {

    private lateinit var etMandiPrice: EditText
    private lateinit var etProductionCost: EditText
    private lateinit var etPackagingCost: EditText
    private lateinit var etTransportCost: EditText
    private lateinit var etDesiredMargin: EditText
    private lateinit var tvCalculationOutput: TextView

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
            setPadding(dp(14), dp(14), dp(14), dp(24))
        }

        val card = CardView(ctx).apply {
            radius = dp(12).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(Color.WHITE)
        }

        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        box.addView(TextView(ctx).apply {
            text = "मंडी थोक भाव बनाम सीधा ग्राहक विक्रय मूल्य गणना (प्रति क्विंटल / 100 kg):"
            setTextColor(Color.parseColor("#1B5E20"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(8))
        })

        etMandiPrice = createNumberField(ctx, "1. वर्तमान मंडी थोक भाव (₹/क्विंटल)", "2680")
        etProductionCost = createNumberField(ctx, "2. किसान की उत्पादन लागत (₹/क्विंटल)", "1950")
        etPackagingCost = createNumberField(ctx, "3. साफ-सफाई व छोटी पैकिंग लागत (₹/क्विंटल)", "120")
        etTransportCost = createNumberField(ctx, "4. स्थानीय परिवहन / भाड़ा (₹/क्विंटल)", "100")
        etDesiredMargin = createNumberField(ctx, "5. मंडी से अतिरिक्त अपेक्षित लाभ (₹/क्विंटल)", "450")

        box.addView(etMandiPrice)
        box.addView(etProductionCost)
        box.addView(etPackagingCost)
        box.addView(etTransportCost)
        box.addView(etDesiredMargin)

        val btnCalculate = Button(ctx).apply {
            text = "🧮 उचित सीधा मूल्य व किसान शुद्ध आय निकालें"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1B5E20"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(46)
            ).apply { topMargin = dp(10); bottomMargin = dp(12) }
            setOnClickListener { computeFairPrice() }
        }
        box.addView(btnCalculate)

        tvCalculationOutput = TextView(ctx).apply {
            setTextColor(Color.parseColor("#1B5E20"))
            setBackgroundColor(Color.parseColor("#E8F5E9"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        box.addView(tvCalculationOutput)

        card.addView(box)
        content.addView(card)
        scroll.addView(content)
        root.addView(scroll)

        computeFairPrice()
        return root
    }

    private fun createNumberField(ctx: android.content.Context, label: String, defaultVal: String): EditText {
        return EditText(ctx).apply {
            hint = label
            setText(defaultVal)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            textSize = 13f
        }
    }

    private fun computeFairPrice() {
        val mandi = etMandiPrice.text.toString().toDoubleOrNull() ?: 2680.0
        val prodCost = etProductionCost.text.toString().toDoubleOrNull() ?: 1950.0
        val pack = etPackagingCost.text.toString().toDoubleOrNull() ?: 120.0
        val trans = etTransportCost.text.toString().toDoubleOrNull() ?: 100.0
        val extraMargin = etDesiredMargin.text.toString().toDoubleOrNull() ?: 450.0

        val minSustainableQuintal = prodCost + pack + trans
        val directCustomerQuintal = mandi + pack + trans + extraMargin
        val estimatedFarmerNetQuintal = directCustomerQuintal - (pack + trans)

        val mandiPerKg = String.format("%.1f", mandi / 100.0)
        val directPerKg = String.format("%.1f", directCustomerQuintal / 100.0)
        val minSustainablePerKg = String.format("%.1f", minSustainableQuintal / 100.0)

        tvCalculationOutput.text =
            "📊 मूल्य तुलना परिणाम (Price Discovery):\n\n" +
            "• मंडी थोक भाव: ₹${mandi.toInt()}/क्विंटल (₹$mandiPerKg / kg)\n" +
            "• न्यूनतम लागत मूल्य: ₹${minSustainableQuintal.toInt()}/क्विंटल (₹$minSustainablePerKg / kg)\n" +
            "• सुझाया गया सीधा ग्राहक मूल्य: ₹${directCustomerQuintal.toInt()}/क्विंटल (₹$directPerKg / kg)\n" +
            "• पैकिंग व भाड़ा घटाकर किसान को शुद्ध प्राप्ति: ₹${estimatedFarmerNetQuintal.toInt()}/क्विंटल\n" +
            "✅ सीधा बेचने पर किसान को मंडी से +₹${extraMargin.toInt()} प्रति क्विंटल अधिक लाभ!"
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
