// app/src/main/java/in/merakisan/app/core/utils/PosterGenerator.kt
package in.merakisan.app.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import in.merakisan.app.core.network.model.ProductDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * MERA KISAN Evidence-Based Agro Poster Engine
 * कानूनी और तथ्य-सम्मत नियमों के तहत स्थानीय डिवाइस पर पोस्टर रेंडरर
 */
object PosterGenerator {

    private const val POSTER_WIDTH = 1080
    private const val POSTER_HEIGHT = 1440

    suspend fun generatePoster(
        context: Context,
        product: ProductDto,
        productBitmap: Bitmap?
    ): Result<File> = withContext(Dispatchers.Default) {
        try {
            val bitmap = Bitmap.createBitmap(POSTER_WIDTH, POSTER_HEIGHT, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 1. बैकग्राउंड (सफेद व एग्रो-पैटर्न)
            canvas.drawColor(Color.parseColor("#F8FAF8"))

            // 2. टॉप हेडर बैनर
            val headerPaint = Paint().apply {
                color = Color.parseColor("#1B5E20")
                isAntiAlias = true
            }
            canvas.drawRect(0f, 0f, POSTER_WIDTH.toFloat(), 180f, headerPaint)

            val appTitlePaint = Paint().apply {
                color = Color.WHITE
                textSize = 58f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("MERA KISAN", 50f, 110f, appTitlePaint)

            val taglinePaint = Paint().apply {
                color = Color.parseColor("#C8E6C9")
                textSize = 28f
                isAntiAlias = true
            }
            canvas.drawText("किसान से सीधे आपकी जरूरत तक", 50f, 155f, taglinePaint)

            // 3. प्रोडक्ट फ़ोटो फ्रेम
            val imageRect = RectF(50f, 220f, (POSTER_WIDTH - 50).toFloat(), 720f)
            val framePaint = Paint().apply {
                color = Color.parseColor("#E8F5E9")
                isAntiAlias = true
            }
            canvas.drawRoundRect(imageRect, 24f, 24f, framePaint)

            if (productBitmap != null) {
                val srcRect = Rect(0, 0, productBitmap.width, productBitmap.height)
                canvas.drawBitmap(productBitmap, srcRect, imageRect, null)
            } else {
                val placeholderPaint = Paint().apply {
                    color = Color.parseColor("#66BB6A")
                    textSize = 48f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText("🌾 ताज़ी देशी उपज", imageRect.centerX(), imageRect.centerY(), placeholderPaint)
            }

            // 4. फ़सल का नाम व दर
            val titlePaint = Paint().apply {
                color = Color.parseColor("#1B5E20")
                textSize = 62f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val displayName = if (!product.variety.isNullOrBlank()) "${product.name} (${product.variety})" else product.name
            canvas.drawText(displayName, 50f, 800f, titlePaint)

            val priceRupees = product.pricePaise / 100.0
            val pricePaint = Paint().apply {
                color = Color.parseColor("#2E7D32")
                textSize = 52f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val priceText = String.format(Locale.getDefault(), "दर: ₹%.0f / %s", priceRupees, product.unit)
            canvas.drawText(priceText, 50f, 875f, pricePaint)

            val stockPaint = Paint().apply {
                color = Color.parseColor("#424242")
                textSize = 34f
                isAntiAlias = true
            }
            canvas.drawText("उपलब्ध मात्रा: ${product.stockQuantity} ${product.unit}", 50f, 930f, stockPaint)

            // 5. सत्यापन स्थिति (सख्त तथ्य-आधारित लेबल)
            val badgeText = when (product.verificationStatus) {
                "organic_certified" -> "🌿 Organic Certified (जैविक प्रमाणित)"
                "verified_farmer" -> "✔ Mera Kisan Verified Farmer"
                else -> "🌾 किसान द्वारा स्व-घोषित (Farmer Declared)"
            }
            val badgePaint = Paint().apply {
                color = Color.parseColor("#1B5E20")
                textSize = 32f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText(badgeText, 50f, 990f, badgePaint)

            // 6. किसान व लोकेशन (सटीक GPS कभी नहीं, केवल गांव व ज़िला)
            val locPaint = Paint().apply {
                color = Color.parseColor("#616161")
                textSize = 34f
                isAntiAlias = true
            }
            val loc = if (!product.village.isNullOrBlank()) {
                "किसान: ${product.sellerName} • ${product.village}, ${product.district}"
            } else {
                "किसान: ${product.sellerName} • ज़िला: ${product.district}"
            }
            canvas.drawText(loc, 50f, 1050f, locPaint)

            // 7. QR कोड जेनरेशन और प्लेसमेंट
            val productDeepLink = "https://merakisan.in/product/${product.productId}"
            val qrResult = QrCodeGenerator.generateQrBitmap(productDeepLink, 280)
            if (qrResult.isSuccess) {
                val qrBitmap = qrResult.getOrNull()
                if (qrBitmap != null) {
                    canvas.drawBitmap(qrBitmap, (POSTER_WIDTH - 340).toFloat(), 1100f, null)

                    val scanPaint = Paint().apply {
                        color = Color.parseColor("#1B5E20")
                        textSize = 26f
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        isAntiAlias = true
                    }
                    canvas.drawText("स्कैन करके खरीदें", (POSTER_WIDTH - 200).toFloat(), 1410f, scanPaint)
                }
            }

            // 8. संपर्क व कॉल टू एक्शन
            val callPaint = Paint().apply {
                color = Color.parseColor("#E65100")
                textSize = 36f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("सीधे किसान से खरीदने हेतु संपर्क करें", 50f, 1170f, callPaint)

            val footerLinkPaint = Paint().apply {
                color = Color.parseColor("#757575")
                textSize = 30f
                isAntiAlias = true
            }
            canvas.drawText(productDeepLink, 50f, 1220f, footerLinkPaint)

            // 9. बॉटम स्ट्रिप
            val footerPaint = Paint().apply {
                color = Color.parseColor("#1B5E20")
            }
            canvas.drawRect(0f, 1420f, POSTER_WIDTH.toFloat(), POSTER_HEIGHT.toFloat(), footerPaint)

            // 10. इमेज फ़ाइल में सेव करना (कम्प्रैस्ड JPEG)
            val cacheFile = File(context.cacheDir, "poster_${product.productId}.jpg")
            val fos = FileOutputStream(cacheFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            fos.flush()
            fos.close()
            bitmap.recycle()

            Result.success(cacheFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
