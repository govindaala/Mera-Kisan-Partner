// app/src/main/java/in/merakisan/app/core/utils/PosterGenerator.kt
package `in`.merakisan.app.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import `in`.merakisan.app.core.network.model.ProductDto
import java.util.Locale

object PosterGenerator {

    fun generateProductPoster(
        context: Context,
        product: ProductDto,
        productBitmap: Bitmap?,
        qrBitmap: Bitmap?
    ): Bitmap {
        val width = 1080
        val height = 1920
        val poster = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(poster)

        // 1. बैकग्राउंड
        val bgPaint = Paint().apply { color = Color.parseColor("#F4F7F4") }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. हेडर बैनर
        val headerPaint = Paint().apply { color = Color.parseColor("#1B5E20") }
        canvas.drawRect(0f, 0f, width.toFloat(), 220f, headerPaint)

        val headerTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 64f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("MERA KISAN — खेत से सीधे घर तक", (width / 2).toFloat(), 130f, headerTextPaint)

        // 3. प्रोडक्ट इमेज
        if (productBitmap != null) {
            val destRect = RectF(60f, 260f, (width - 60).toFloat(), 960f)
            canvas.drawBitmap(productBitmap, null, destRect, null)
        }

        // 4. प्रोडक्ट टाइटल व वैरायटी
        val titlePaint = Paint().apply {
            color = Color.parseColor("#1C1B1F")
            textSize = 58f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val displayTitle = if (!product.variety.isNullOrBlank()) "${product.name} (${product.variety})" else product.name
        canvas.drawText(displayTitle, 60f, 1050f, titlePaint)

        // 5. मूल्य व स्टॉक (IntegerPaise से सुरक्षित गणना)
        val priceRupees = product.pricePaise / 100.0
        val pricePaint = Paint().apply {
            color = Color.parseColor("#2E7D32")
            textSize = 54f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(String.format(Locale.getDefault(), "भाव: ₹%.0f / %s", priceRupees, product.unit), 60f, 1140f, pricePaint)

        val stockPaint = Paint().apply {
            color = Color.parseColor("#49454F")
            textSize = 42f
        }
        canvas.drawText("उपलब्ध मात्रा: ${product.stockQuantity}${product.unit}", 60f, 1210f, stockPaint)

        // 6. साक्ष्य-आधारित सत्यापन बैज (Unsupported claims पूर्णतः समाप्त)
        val badgeText = when (product.verificationStatus) {
            "organic_certified" -> "प्रमाणित जैविक (Organic Certified)"
            "verified_farmer" -> "सत्यापित किसान साथी"
            "lab_tested" -> "प्रयोगशाला परीक्षित (Lab Tested)"
            else -> "किसान द्वारा घोषित स्थानीय उपज (Farmer Declared)"
        }
        val badgePaint = Paint().apply {
            color = Color.parseColor("#E8F5E9")
        }
        val badgeRect = RectF(60f, 1260f, (width - 60).toFloat(), 1350f)
        canvas.drawRoundRect(badgeRect, 16f, 16f, badgePaint)

        val badgeTextPaint = Paint().apply {
            color = Color.parseColor("#1B5E20")
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(badgeText, (width / 2).toFloat(), 1320f, badgeTextPaint)

        // 7. किसान व स्थान
        val loc = if (!product.village.isNullOrBlank()) "${product.village},${product.district}" else product.district.orEmpty()
        canvas.drawText("किसान: ${product.sellerName} \vert{} क्षेत्र: $loc", 60f, 1430f, stockPaint)

        // 8. QR कोड व स्कैन कॉल-टू-एक्शन
        if (qrBitmap != null) {
            val qrRect = RectF(740f, 1490f, 1000f, 1750f)
            canvas.drawBitmap(qrBitmap, null, qrRect, null)
        }

        val ctaPaint = Paint().apply {
            color = Color.parseColor("#1B5E20")
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("QR स्कैन करें या संपर्क करें:", 60f, 1570f, ctaPaint)
        canvas.drawText("Mera Kisan ऐप पर ऑर्डर करें", 60f, 1640f, stockPaint)

        // 9. फुटर
        val footerPaint = Paint().apply { color = Color.parseColor("#2E7D32") }
        canvas.drawRect(0f, 1840f, width.toFloat(), height.toFloat(), footerPaint)

        return poster
    }
}
