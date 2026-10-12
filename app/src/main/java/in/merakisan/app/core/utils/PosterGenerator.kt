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
import java.io.File
import java.io.FileOutputStream
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

        val bgPaint = Paint().apply { color = Color.parseColor("#F4F7F4") }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val headerPaint = Paint().apply { color = Color.parseColor("#1B5E20") }
        canvas.drawRect(0f, 0f, width.toFloat(), 220f, headerPaint)

        val headerTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 64f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("MERA KISAN", (width / 2).toFloat(), 130f, headerTextPaint)

        if (productBitmap != null) {
            val destRect = RectF(60f, 260f, (width - 60).toFloat(), 960f)
            canvas.drawBitmap(productBitmap, null, destRect, null)
        }

        val titlePaint = Paint().apply {
            color = Color.parseColor("#1C1B1F")
            textSize = 58f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val displayTitle = if (!product.variety.isNullOrBlank()) "${product.name} (${product.variety})" else product.name
        canvas.drawText(displayTitle, 60f, 1050f, titlePaint)

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
        canvas.drawText("उपलब्ध स्टॉक: ${product.stockQuantity}${product.unit}", 60f, 1210f, stockPaint)

        val loc = if (!product.village.isNullOrBlank()) "${product.village},${product.district}" else product.district.orEmpty()
        // FIX: Koi \v ya \vert{} nahi, seedha standard pipe symbol
        canvas.drawText("किसान: ${product.sellerName} \vert{} क्षेत्र: $loc", 60f, 1430f, stockPaint)

        val badgePaint = Paint().apply { color = Color.parseColor("#E8F5E9") }
        val badgeRect = RectF(60f, 1260f, (width - 60).toFloat(), 1350f)
        canvas.drawRoundRect(badgeRect, 16f, 16f, badgePaint)

        val badgeTextPaint = Paint().apply {
            color = Color.parseColor("#1B5E20")
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("किसान द्वारा घोषित स्थानीय उपज", (width / 2).toFloat(), 1320f, badgeTextPaint)

        if (qrBitmap != null) {
            val qrRect = RectF(740f, 1490f, 1000f, 1750f)
            canvas.drawBitmap(qrBitmap, null, qrRect, null)
        }

        val footerPaint = Paint().apply { color = Color.parseColor("#2E7D32") }
        canvas.drawRect(0f, 1840f, width.toFloat(), height.toFloat(), footerPaint)

        return poster
    }

    fun generatePoster(context: Context, product: ProductDto, productBitmap: Bitmap?): Result<File> {
        return try {
            val bitmap = generateProductPoster(context, product, productBitmap, null)
            val file = File(context.cacheDir, "poster_${product.productId}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
