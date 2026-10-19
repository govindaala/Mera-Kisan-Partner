// app/src/main/java/in/merakisan/app/ui/offers/NegotiationHistoryFragment.kt
package `in`.merakisan.app.ui.offers

import android.app.AlertDialog
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
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import `in`.merakisan.app.data.model.OfferModel

/**
 * Master Spec Sec 8 & merged.txt Sec 14, 18, 39:
 * Controlled Negotiation History & State Machine (PENDING -> COUNTERED -> ACCEPTED / REJECTED / EXPIRED).
 */
class NegotiationHistoryFragment : Fragment() {

    private lateinit var containerOffers: LinearLayout

    private val offersList = mutableListOf(
        OfferModel(
            id = "off_501",
            productId = "prod_101",
            productName = "देशी शरबती गेहूं",
            buyerId = "usr_rajesh_03",
            buyerName = "राजेश सेठिया",
            sellerId = "usr_ramlal_02",
            sellerName = "रामलाल पाटीदार",
            originalPricePaise = 3800L,
            offeredPricePaise = 3500L,
            quantity = 25.0,
            unit = "kg",
            message = "मैं 25 किलो आज ही गरोठ से पिकअप कर लूंगा।",
            status = "PENDING",
            createdAtDisplay = "आज अभी"
        ),
        OfferModel(
            id = "off_502",
            productId = "prod_102",
            productName = "सोयाबीन (बिना पॉलिश / JS 9560)",
            buyerId = "usr_anil_04",
            buyerName = "अनिल शर्मा",
            sellerId = "usr_govind_01",
            sellerName = "गोविंद पाटीदार",
            originalPricePaise = 4800L,
            offeredPricePaise = 4600L,
            quantity = 50.0,
            unit = "kg",
            message = "काउंटर-ऑफर: ₹46/kg में 50 किलो फाइनल करें।",
            status = "COUNTERED",
            createdAtDisplay = "आज"
        )
    )

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
            text = "🤝 भाव प्रस्ताव व मोलभाव (Offers & Counter-Offers)"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
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

        containerOffers = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(24))
        }

        scroll.addView(containerOffers)
        root.addView(scroll)

        renderOffers()
        return root
    }

    private fun renderOffers() {
        val ctx = context ?: return
        containerOffers.removeAllViews()

        for (off in offersList) {
            val card = CardView(ctx).apply {
                radius = dp(10).toFloat()
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
                text = "🌾 ${off.productName}  •  स्थिति: [${off.status}]"
                setTextColor(Color.parseColor("#1B5E20"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setTypeface(typeface, Typeface.BOLD)
            })

            box.addView(TextView(ctx).apply {
                text = "सूचीबद्ध भाव: ₹${off.originalPriceRupees}/${off.unit}  →  प्रस्तावित भाव: ₹${off.offeredPriceRupees}/${off.unit}\n" +
                       "मात्रा: ${off.quantity.toInt()} ${off.unit} (कुल: ₹${(off.offeredPriceRupees * off.quantity).toInt()})\n" +
                       "👤 खरीदार: ${off.buyerName}  •  👨‍🌾 किसान: ${off.sellerName}\n" +
                       "💬 संदेश: \"${off.message}\""
                setTextColor(Color.parseColor("#424242"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, dp(4), 0, dp(8))
            })

            if (off.status == "PENDING" || off.status == "COUNTERED") {
                val actionRow = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    weightSum = 3f
                }

                actionRow.addView(Button(ctx).apply {
                    text = "✅ स्वीकार (Accept)"
                    textSize = 10f
                    setTextColor(Color.WHITE)
                    setBackgroundColor(Color.parseColor("#1B5E20"))
                    layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply { rightMargin = dp(4) }
                    setOnClickListener {
                        off.status = "ACCEPTED"
                        renderOffers()
                        Toast.makeText(ctx, "✅ सौदा ₹${off.offeredPriceRupees}/${off.unit} पर लॉक हो गया!", Toast.LENGTH_LONG).show()
                    }
                })

                actionRow.addView(Button(ctx).apply {
                    text = "🔄 काउंटर भाव दें"
                    textSize = 10f
                    setTextColor(Color.WHITE)
                    setBackgroundColor(Color.parseColor("#E65100"))
                    layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply { leftMargin = dp(4); rightMargin = dp(4) }
                    setOnClickListener { showCounterOfferDialog(off) }
                })

                actionRow.addView(Button(ctx).apply {
                    text = "✖ अस्वीकार"
                    textSize = 10f
                    setTextColor(Color.parseColor("#C62828"))
                    setBackgroundColor(Color.parseColor("#FFEBEE"))
                    layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply { leftMargin = dp(4) }
                    setOnClickListener {
                        off.status = "REJECTED"
                        renderOffers()
                    }
                })

                box.addView(actionRow)
            }

            card.addView(box)
            containerOffers.addView(card)
        }
    }

    private fun showCounterOfferDialog(off: OfferModel) {
        val ctx = context ?: return
        val etCounter = EditText(ctx).apply {
            hint = "नया काउंटर भाव ₹/${off.unit} दर्ज करें"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText((off.offeredPriceRupees + 1).toString())
        }

        AlertDialog.Builder(ctx)
            .setTitle("🔄 काउंटर-ऑफर भेजें: ${off.productName}")
            .setView(etCounter)
            .setPositiveButton("काउंटर भाव भेजें") { _, _ ->
                val nextRupees = etCounter.text.toString().toLongOrNull() ?: off.offeredPriceRupees.toLong()
                off.offeredPricePaise = nextRupees * 100L
                off.status = "COUNTERED"
                renderOffers()
                Toast.makeText(ctx, "🔄 नया काउंटर भाव ₹$nextRupees/${off.unit} भेज दिया गया!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("रद्द करें", null)
            .show()
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
