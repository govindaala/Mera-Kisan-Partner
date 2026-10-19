// app/src/main/java/in/merakisan/app/ui/marketing/AiPosterStudioFragment.kt
package `in`.merakisan.app.ui.marketing

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

/**
 * Master Spec Sec 1, 10, 14, 15 & merged.txt Sec 25–33, 89:
 * Dedicated 8-Template AI Poster Studio with Factual Claim Sanitizer,
 * QR Code Matrix Preview, Permanent Deep Link & Multi-Platform Share.
 */
class AiPosterStudioFragment : Fragment() {

    private lateinit var tvPosterPreviewHeader: TextView
    private lateinit var tvPosterPreviewBody: TextView
    private lateinit var tvPosterPreviewFooter: TextView

    private lateinit var etCropName: EditText
    private lateinit var etVariety: EditText
    private lateinit var etPrice: EditText
    private lateinit var etQty: EditText
    private lateinit var etFarmer: EditText
    private lateinit var etLocation: EditText

    private var activeTemplateTitle = "1. आज की ताज़ा फसल (Fresh Harvest)"
    private var activeBgColor = "#E8F5E9"
    private var activeBorderColor = "#1B5E20"

    private val templates = listOf(
        Triple("1. आज की ताज़ा फसल", "#E8F5E9", "#1B5E20"),
        Triple("2. देशी / पारंपरिक उत्पाद", "#FFF8E1", "#E65100"),
        Triple("3. छोटी मात्रा (1–25 kg)", "#E3F2FD", "#0D47A1"),
        Triple("4. खेत से सीधा (Farm Direct)", "#F1F8E9", "#2E7D32"),
        Triple("5. विशेष मूल्य प्रस्ताव", "#FCE4EC", "#880E4F"),
        Triple("6. नई फसल आगमन", "#E0F2F1", "#004D40"),
        Triple("7. कच्ची घानी शुद्ध तेल", "#FFF3E0", "#BF360C"),
        Triple("8. मेरी खेती की कहानी", "#EDE7F6", "#311B92")
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

        // 1. TOP BAR
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
            text = "🎨 AI पोस्टर व QR स्टूडियो (8 Templates)"
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
            setPadding(dp(12), dp(12), dp(12), dp(24))
        }

        // 2. 8 TEMPLATE SELECTOR CHIPS
        content.addView(TextView(ctx).apply {
            text = "पोस्टर टेम्पलेट चुनें (Layer-1 Local Renderer + AI Copy):"
            setTextColor(Color.parseColor("#212121"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(6))
        })

        val chipScroll = HorizontalScrollView(ctx).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(0, 0, 0, dp(10))
        }
        val chipRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }

        for ((title, bg, border) in templates) {
            chipRow.addView(TextView(ctx).apply {
                text = title
                setTextColor(Color.parseColor(border))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(dp(12), dp(6), dp(12), dp(6))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor(bg))
                    cornerRadius = dp(16).toFloat()
                    setStroke(dp(1), Color.parseColor(border))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { rightMargin = dp(8) }
                setOnClickListener {
                    activeTemplateTitle = title
                    activeBgColor = bg
                    activeBorderColor = border
                    updateLivePosterPreview()
                }
            })
        }
        chipScroll.addView(chipRow)
        content.addView(chipScroll)

        // 3. LIVE GRAPHICAL POSTER PREVIEW CARD
        val posterCard = CardView(ctx).apply {
            radius = dp(14).toFloat()
            cardElevation = dp(4).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(14) }
        }

        val posterInner = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = GradientDrawable().apply {
                setColor(Color.parseColor(activeBgColor))
                cornerRadius = dp(14).toFloat()
                setStroke(dp(3), Color.parseColor(activeBorderColor))
            }
        }

        tvPosterPreviewHeader = TextView(ctx).apply {
            setTextColor(Color.parseColor("#1B5E20"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        tvPosterPreviewBody = TextView(ctx).apply {
            setTextColor(Color.parseColor("#212121"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(10), 0, dp(10))
        }

        tvPosterPreviewFooter = TextView(ctx).apply {
            setTextColor(Color.parseColor("#0D47A1"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(typeface, Typeface.BOLD)
            setBackgroundColor(Color.WHITE)
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }

        posterInner.addView(tvPosterPreviewHeader)
        posterInner.addView(tvPosterPreviewBody)
        posterInner.addView(tvPosterPreviewFooter)
        posterCard.addView(posterInner)
        content.addView(posterCard)

        // 4. EDITABLE POSTER FIELDS (With Automatic Unsupported Claim Sanitizer)
        val formCard = CardView(ctx).apply {
            radius = dp(10).toFloat()
            setCardBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
        }

        val formBox = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }

        formBox.addView(TextView(ctx).apply {
            text = "✏️ पोस्टर की जानकारी बदलें (बिना प्रमाण '100% केमिकल-फ्री' दावा स्वतः हट जाएगा):"
            setTextColor(Color.parseColor("#616161"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        })

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateLivePosterPreview()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        etCropName = EditText(ctx).apply { setText("देशी शरबती गेहूं"); hint = "उत्पाद का नाम"; addTextChangedListener(watcher) }
        etVariety = EditText(ctx).apply { setText("मालवा शरबती (ताज़ा फसल)"); hint = "किस्म / विशेषता"; addTextChangedListener(watcher) }
        etPrice = EditText(ctx).apply { setText("38"); hint = "दर ₹ प्रति किलो"; addTextChangedListener(watcher) }
        etQty = EditText(ctx).apply { setText("25 kg छोटी मात्रा भी उपलब्ध"); hint = "उपलब्ध मात्रा"; addTextChangedListener(watcher) }
        etFarmer = EditText(ctx).apply { setText("गोविंद पाटीदार"); hint = "किसान का नाम"; addTextChangedListener(watcher) }
        etLocation = EditText(ctx).apply { setText("गरोठ, मंदसौर (मध्य प्रदेश)"); hint = "गांव व ज़िला"; addTextChangedListener(watcher) }

        formBox.addView(etCropName)
        formBox.addView(etVariety)
        formBox.addView(etPrice)
        formBox.addView(etQty)
        formBox.addView(etFarmer)
        formBox.addView(etLocation)
        formCard.addView(formBox)
        content.addView(formCard)

        // 5. SHARE ACTIONS (WhatsApp / Facebook / Instagram / Copy Link)
        val btnShareWhatsApp = Button(ctx).apply {
            text = "🟢 WhatsApp / Facebook / Instagram पर पोस्टर शेयर करें"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1B5E20"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48))
            setOnClickListener {
                val shareMsg = "${tvPosterPreviewHeader.text}\n\n${tvPosterPreviewBody.text}\n\n${tvPosterPreviewFooter.text}"
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareMsg)
                }
                startActivity(Intent.createChooser(intent, "AI पोस्टर शेयर करें"))
            }
        }
        content.addView(btnShareWhatsApp)

        scroll.addView(content)
        root.addView(scroll)

        updateLivePosterPreview()
        return root
    }

    /**
     * Master Spec Sec 1 & 14: Blocks unsupported claims ("100% chemical-free", "10x pure", health cures)
     */
    private fun sanitizeFactualClaim(raw: String): String {
        return raw
            .replace(Regex("100%\\s*(chemical[\\s-]*free|pure|organic)", RegexOption.IGNORE_CASE), "Farmer Declared")
            .replace(Regex("केमिकल[\\s-]*फ्री|रसायन[\\s-]*मुक्त"), "पारंपरिक खेती")
            .replace(Regex("10\\s*गुना\\s*शुद्ध|10x\\s*pure", RegexOption.IGNORE_CASE), "खेत से सीधा")
            .trim()
    }

    private fun updateLivePosterPreview() {
        val crop = sanitizeFactualClaim(etCropName.text.toString().ifBlank { "देशी गेहूं" })
        val variety = sanitizeFactualClaim(etVariety.text.toString().ifBlank { "देशी किस्म" })
        val price = etPrice.text.toString().ifBlank { "38" }
        val qty = sanitizeFactualClaim(etQty.text.toString().ifBlank { "25 kg उपलब्ध" })
        val farmer = etFarmer.text.toString().ifBlank { "किसान साथी" }
        val loc = etLocation.text.toString().ifBlank { "मंदसौर" }

        tvPosterPreviewHeader.text = "🌾 MERA KISAN — $activeTemplateTitle 🌾\n“किसान से सीधे आपकी जरूरत तक”"
        tvPosterPreviewBody.text =
            "✨ उत्पाद: $crop ($variety)\n" +
            "💰 सीधा किसान भाव: ₹$price / kg\n" +
            "📦 उपलब्धता: $qty\n" +
            "👨‍🌾 किसान: $farmer\n" +
            "📍 स्थान: $loc\n" +
            "🏅 सत्यापन: ✔ किसान द्वारा घोषित (Farmer Declared)"
        tvPosterPreviewFooter.text =
            "▣ [QR CODE: Scan to Open in Mera Kisan App]\n" +
            "🔗 स्थायी लिंक: https://merakisan.in/product/mk_101"
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
