// app/src/main/java/in/merakisan/app/ui/orders/OrdersFragment.kt
package `in`.merakisan.app.ui.orders

import android.app.AlertDialog
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
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import `in`.merakisan.app.core.state.RoleStateManager

class OrdersFragment : Fragment() {

    private lateinit var tabBuyerOrders: TextView
    private lateinit var tabFarmerOrders: TextView
    private lateinit var containerOrders: LinearLayout

    private var currentTab: String = "BUYER"

    private val sampleOrders = mutableListOf(
        OrderRecord(
            orderId = "MK-ORD-9021",
            cropName = "देशी शरबती गेहूं (25 kg छोटी मात्रा)",
            amount = "₹950 (₹38/kg)",
            counterparty = "किसान: रामलाल पाटीदार (गरोठ)",
            status = "SHIPPED (रास्ते में — आज डिलीवरी)",
            paymentMode = "10% टोकन पेड (Escrow सुरक्षित)",
            type = "BUYER"
        ),
        OrderRecord(
            orderId = "MK-ORD-8894",
            cropName = "कच्ची घानी मूंगफली तेल (5 Litre)",
            amount = "₹1,050",
            counterparty = "किसान घाणी केंद्र (नीमच)",
            status = "COMPLETED (डिलीवर व पूर्ण)",
            paymentMode = "UPI सत्यापित",
            type = "BUYER"
        ),
        OrderRecord(
            orderId = "MK-SALE-4012",
            cropName = "सोयाबीन बिना पॉलिश (50 kg)",
            amount = "₹2,400 (₹48/kg)",
            counterparty = "ग्राहक: राजेश सेठिया (मंदसौर)",
            status = "PLACED (नया ऑर्डर — स्वीकार करें)",
            paymentMode = "खेत से पिकअप / COD",
            type = "FARMER"
        ),
        OrderRecord(
            orderId = "MK-SALE-3980",
            cropName = "देशी रियावन लहसुन (10 kg)",
            amount = "₹1,400",
            counterparty = "ग्राहक: अनिल शर्मा (इंदौर)",
            status = "DELIVERED (भुगतान सेटलमेंट पूर्ण)",
            paymentMode = "ऑनलाइन एस्क्रो पेआउट",
            type = "FARMER"
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()

        currentTab = if (RoleStateManager.getCurrentRole() == RoleStateManager.ROLE_FARMER) "FARMER" else "BUYER"

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
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(14), dp(12), dp(14), dp(8))
        }

        header.addView(TextView(ctx).apply {
            text = "📦 मेरे ऑर्डर्स, लेन-देन व डिलीवरी स्थिति"
            setTextColor(Color.parseColor("#1B5E20"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTypeface(typeface, Typeface.BOLD)
        })

        // 2. DUAL TABS (Buyer Orders vs Farmer Sales Orders)
        val tabRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 2f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(44)
            ).apply { topMargin = dp(10) }
        }

        tabBuyerOrders = TextView(ctx).apply {
            text = "🛒 खरीदे गए (BUYER)"
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            setOnClickListener {
                currentTab = "BUYER"
                renderOrders()
            }
        }

        tabFarmerOrders = TextView(ctx).apply {
            text = "🌾 बिक्री ऑर्डर्स (FARMER)"
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            setOnClickListener {
                currentTab = "FARMER"
                renderOrders()
            }
        }

        tabRow.addView(tabBuyerOrders)
        tabRow.addView(tabFarmerOrders)
        header.addView(tabRow)
        root.addView(header)

        val scroll = ScrollView(ctx).apply {
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        containerOrders = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(24))
        }
        scroll.addView(containerOrders)
        root.addView(scroll)

        renderOrders()

        return root
    }

    private fun renderOrders() {
        val ctx = context ?: return
        containerOrders.removeAllViews()

        val isBuyerActive = currentTab == "BUYER"
        tabBuyerOrders.setBackgroundColor(if (isBuyerActive) Color.parseColor("#E8F5E9") else Color.WHITE)
        tabBuyerOrders.setTextColor(if (isBuyerActive) Color.parseColor("#1B5E20") else Color.parseColor("#757575"))

        tabFarmerOrders.setBackgroundColor(if (!isBuyerActive) Color.parseColor("#E8F5E9") else Color.WHITE)
        tabFarmerOrders.setTextColor(if (!isBuyerActive) Color.parseColor("#1B5E20") else Color.parseColor("#757575"))

        val filtered = sampleOrders.filter { it.type == currentTab }

        for (ord in filtered) {
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
                text = "ऑर्डर #${ord.orderId}  •${ord.status}"
                setTextColor(Color.parseColor("#0D47A1"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTypeface(typeface, Typeface.BOLD)
            })

            box.addView(TextView(ctx).apply {
                text = "🌾 ${ord.cropName}"
                setTextColor(Color.parseColor("#212121"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dp(4), 0, dp(2))
            })

            box.addView(TextView(ctx).apply {
                text = "💰 कुल राशि: ${ord.amount}  •  ${ord.paymentMode}\n👤 ${ord.counterparty}"
                setTextColor(Color.parseColor("#424242"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, 0, 0, dp(8))
            })

            // Controlled Order State Actions (Confirm Delivery | 5-Star Rating | Dispute)
            val actionRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                weightSum = 3f
            }

            val btnStatusAction = Button(ctx).apply {
                text = if (currentTab == "FARMER") "✅ स्वीकार/डिस्पैच" else "✅ डिलीवरी पुष्टि"
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#1B5E20"))
                textSize = 10f
                layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply { rightMargin = dp(4) }
                setOnClickListener {
                    ord.status = "COMPLETED (डिलीवर व सत्यापित)"
                    renderOrders()
                    Toast.makeText(ctx, "ऑर्डर स्थिति अपडेट: COMPLETED", Toast.LENGTH_SHORT).show()
                }
            }

            val btnRate = Button(ctx).apply {
                text = "⭐ 5-स्टार रेटिंग"
                setTextColor(Color.parseColor("#1B5E20"))
                setBackgroundColor(Color.parseColor("#E8F5E9"))
                textSize = 10f
                layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply { leftMargin = dp(4); rightMargin = dp(4) }
                setOnClickListener { showMultiParamRatingDialog(ord) }
            }

            val btnDispute = Button(ctx).apply {
                text = "⚠️ विवाद (Dispute)"
                setTextColor(Color.parseColor("#C62828"))
                setBackgroundColor(Color.parseColor("#FFEBEE"))
                textSize = 10f
                layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply { leftMargin = dp(4) }
                setOnClickListener { showRaiseDisputeDialog(ord) }
            }

            actionRow.addView(btnStatusAction)
            actionRow.addView(btnRate)
            actionRow.addView(btnDispute)
            box.addView(actionRow)

            card.addView(box)
            containerOrders.addView(card)
        }
    }

    private fun showMultiParamRatingDialog(ord: OrderRecord) {
        val ctx = context ?: return
        val params = arrayOf(
            "⭐⭐⭐⭐⭐ (5/5) — गुणवत्ता, ताज़गी, पैकिंग व व्यवहार उत्कृष्ट",
            "⭐⭐⭐⭐ (4/5) — बहुत अच्छा अनुभव",
            "⭐⭐⭐ (3/5) — संतोषजनक"
        )
        AlertDialog.Builder(ctx)
            .setTitle("⭐ ऑर्डर #${ord.orderId} की रेटिंग दें (Master Spec Sec 17)")
            .setItems(params) { _, _ ->
                Toast.makeText(ctx, "✅ आपकी सत्यापित रेटिंग व रिव्यू दर्ज हो गया!", Toast.LENGTH_LONG).show()
            }
            .show()
    }

    private fun showRaiseDisputeDialog(ord: OrderRecord) {
        val ctx = context ?: return
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(4))
        }
        val etReason = EditText(ctx).apply {
            hint = "समस्या लिखें (उदा. गुणवत्ता अंतर, वजन कम, या डिलीवरी देरी)"
        }
        box.addView(etReason)

        AlertDialog.Builder(ctx)
            .setTitle("⚠️ विवाद दर्ज करें (ऑर्डर #${ord.orderId})")
            .setView(box)
            .setPositiveButton("प्रमाण सहित विवाद भेजें") { _, _ ->
                ord.status = "DISPUTED (सहायता टीम द्वारा समीक्षाधीन)"
                renderOrders()
                Toast.makeText(ctx, "विवाद टिकट दर्ज किया गया। एस्क्रो भुगतान सुरक्षित होल्ड पर है।", Toast.LENGTH_LONG).show()
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

    data class OrderRecord(
        val orderId: String,
        val cropName: String,
        val amount: String,
        val counterparty: String,
        var status: String,
        val paymentMode: String,
        val type: String
    )
}
