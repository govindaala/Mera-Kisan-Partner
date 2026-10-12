// app/src/main/java/in/merakisan/app/core/utils/PosterGenerator.kt

package `in`.merakisan.app.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import `in`.merakisan.app.core.network.model.ProductDto
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object PosterGenerator {

    private const val POSTER_WIDTH = 1080
    private const val POSTER_HEIGHT = 1920

    private const val LEFT_MARGIN = 60f
    private const val RIGHT_MARGIN = 60f

    private const val GREEN_DARK = "#1B5E20"
    private const val GREEN = "#2E7D32"
    private const val GREEN_LIGHT = "#E8F5E9"
    private const val BACKGROUND = "#F4F7F4"
    private const val TEXT_DARK = "#1C1B1F"
    private const val TEXT_SECONDARY = "#49454F"

    /**
     * उत्पाद का पूरा पोस्टर Bitmap के रूप में तैयार करता है।
     *
     * productBitmap: उत्पाद की फोटो; null होने पर placeholder बनेगा।
     * qrBitmap: QR कोड; null होने पर QR कोड नहीं दिखेगा।
     */
    fun generateProductPoster(
        context: Context,
        product: ProductDto,
        productBitmap: Bitmap?,
        qrBitmap: Bitmap?
    ): Bitmap {

        val poster = Bitmap.createBitmap(
            POSTER_WIDTH,
            POSTER_HEIGHT,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(poster)

        // सभी Paint ऑब्जेक्ट का टेक्स्ट रेंडरिंग कॉन्फ़िगरेशन।
        fun createTextPaint(
            color: Int,
            size: Float,
            bold: Boolean = false,
            align: Paint.Align = Paint.Align.LEFT
        ): Paint {
            return Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                textSize = size
                textAlign = align
                typeface = Typeface.create(
                    Typeface.DEFAULT,
                    if (bold) Typeface.BOLD else Typeface.NORMAL
                )
                isSubpixelText = true
            }
        }

        val backgroundColor = Color.parseColor(BACKGROUND)
        val darkGreen = Color.parseColor(GREEN_DARK)
        val green = Color.parseColor(GREEN)
        val lightGreen = Color.parseColor(GREEN_LIGHT)
        val darkText = Color.parseColor(TEXT_DARK)
        val secondaryText = Color.parseColor(TEXT_SECONDARY)

        // -------------------------------------------------
        // 1. बैकग्राउंड
        // -------------------------------------------------

        canvas.drawColor(backgroundColor)

        // ऊपर का हेडर।
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkGreen
        }

        canvas.drawRect(
            0f,
            0f,
            POSTER_WIDTH.toFloat(),
            220f,
            headerPaint
        )

        // ब्रांड नाम।
        val brandPaint = createTextPaint(
            color = Color.WHITE,
            size = 64f,
            bold = true,
            align = Paint.Align.CENTER
        )

        canvas.drawText(
            "MERA KISAN",
            POSTER_WIDTH / 2f,
            130f,
            brandPaint
        )

        val taglinePaint = createTextPaint(
            color = Color.WHITE,
            size = 27f,
            align = Paint.Align.CENTER
        )

        canvas.drawText(
            "किसान से सीधे आपके घर तक",
            POSTER_WIDTH / 2f,
            182f,
            taglinePaint
        )

        // -------------------------------------------------
        // 2. उत्पाद की फोटो
        // -------------------------------------------------

        val imageRect = RectF(
            LEFT_MARGIN,
            260f,
            POSTER_WIDTH - RIGHT_MARGIN,
            960f
        )

        val imageBackground = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        }

        canvas.drawRoundRect(
            imageRect,
            24f,
            24f,
            imageBackground
        )

        if (productBitmap != null && !productBitmap.isRecycled) {

            // फोटो को अनुपात बनाए रखते हुए फिट करें।
            val bitmapWidth = productBitmap.width.toFloat()
            val bitmapHeight = productBitmap.height.toFloat()

            if (bitmapWidth > 0f && bitmapHeight > 0f) {

                val scale = minOf(
                    imageRect.width() / bitmapWidth,
                    imageRect.height() / bitmapHeight
                )

                val scaledWidth = bitmapWidth * scale
                val scaledHeight = bitmapHeight * scale

                val left = imageRect.centerX() - scaledWidth / 2f
                val top = imageRect.centerY() - scaledHeight / 2f

                val destination = RectF(
                    left,
                    top,
                    left + scaledWidth,
                    top + scaledHeight
                )

                val photoPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

                canvas.save()

                canvas.clipRect(
                    imageRect.left,
                    imageRect.top,
                    imageRect.right,
                    imageRect.bottom
                )

                canvas.drawBitmap(
                    productBitmap,
                    null,
                    destination,
                    photoPaint
                )

                canvas.restore()
            }

        } else {

            // फोटो उपलब्ध न होने पर placeholder।
            val placeholderPaint = createTextPaint(
                color = green,
                size = 42f,
                bold = true,
                align = Paint.Align.CENTER
            )

            canvas.drawText(
                "उत्पाद की फोटो उपलब्ध नहीं है",
                imageRect.centerX(),
                imageRect.centerY(),
                placeholderPaint
            )
        }

        // -------------------------------------------------
        // 3. उत्पाद का नाम
        // -------------------------------------------------

        val displayTitle = if (!product.variety.isNullOrBlank()) {
            "${product.name} (${product.variety})"
        } else {
            product.name
        }

        val titlePaint = createTextPaint(
            color = darkText,
            size = 58f,
            bold = true
        )

        val titleMaxWidth = POSTER_WIDTH - LEFT_MARGIN - RIGHT_MARGIN
        val titleLines = wrapText(
            text = displayTitle,
            paint = titlePaint,
            maxWidth = titleMaxWidth
        )

        var titleBaseline = 1040f

        for (line in titleLines.take(2)) {

            val fittedLine = fitText(
                text = line,
                paint = titlePaint,
                maxWidth = titleMaxWidth,
                minTextSize = 32f
            )

            canvas.drawText(
                fittedLine,
                LEFT_MARGIN,
                titleBaseline,
                titlePaint
            )

            titleBaseline += 66f
        }

        // -------------------------------------------------
        // 4. उत्पाद का भाव
        // -------------------------------------------------

        val priceRupees = product.pricePaise / 100.0

        val formattedPrice = String.format(
            Locale.getDefault(),
            "%.2f",
            priceRupees
        )

        val priceText = "भाव: ₹$formattedPrice / ${product.unit}"

        val pricePaint = createTextPaint(
            color = green,
            size = 54f,
            bold = true
        )

        val fittedPrice = fitText(
            text = priceText,
            paint = pricePaint,
            maxWidth = titleMaxWidth,
            minTextSize = 30f
        )

        canvas.drawText(
            fittedPrice,
            LEFT_MARGIN,
            1190f,
            pricePaint
        )

        // -------------------------------------------------
        // 5. स्टॉक की जानकारी
        // -------------------------------------------------

        val stockText =
            "उपलब्ध स्टॉक: ${product.stockQuantity} ${product.unit}"

        val stockPaint = createTextPaint(
            color = secondaryText,
            size = 42f
        )

        val fittedStock = fitText(
            text = stockText,
            paint = stockPaint,
            maxWidth = titleMaxWidth,
            minTextSize = 28f
        )

        canvas.drawText(
            fittedStock,
            LEFT_MARGIN,
            1260f,
            stockPaint
        )

        // -------------------------------------------------
        // 6. उत्पाद संबंधी बैज
        // -------------------------------------------------

        val badgeRect = RectF(
            LEFT_MARGIN,
            1290f,
            POSTER_WIDTH - RIGHT_MARGIN,
            1380f
        )

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lightGreen
        }

        canvas.drawRoundRect(
            badgeRect,
            20f,
            20f,
            badgePaint
        )

        val badgeTextPaint = createTextPaint(
            color = darkGreen,
            size = 36f,
            bold = true,
            align = Paint.Align.CENTER
        )

        canvas.drawText(
            "किसान द्वारा घोषित स्थानीय उपज",
            badgeRect.centerX(),
            1347f,
            badgeTextPaint
        )

        // -------------------------------------------------
        // 7. किसान और क्षेत्र की जानकारी
        // -------------------------------------------------

        val location = if (!product.village.isNullOrBlank()) {
            listOfNotNull(
                product.village,
                product.district?.takeIf { it.isNotBlank() }
            ).joinToString(", ")
        } else {
            product.district.orEmpty()
        }

        val sellerName = product.sellerName.orEmpty()

        val sellerText = "किसान: $sellerName"

        val locationText = if (location.isNotBlank()) {
            "क्षेत्र: $location"
        } else {
            "क्षेत्र: उपलब्ध नहीं"
        }

        val sellerPaint = createTextPaint(
            color = darkText,
            size = 36f
        )

        val locationPaint = createTextPaint(
            color = secondaryText,
            size = 34f
        )

        canvas.drawText(
            fitText(sellerText, sellerPaint, 600f, 26f),
            LEFT_MARGIN,
            1460f,
            sellerPaint
        )

        canvas.drawText(
            fitText(locationText, locationPaint, 600f, 26f),
            LEFT_MARGIN,
            1520f,
            locationPaint
        )

        // -------------------------------------------------
        // 8. QR कोड
        // -------------------------------------------------

        if (qrBitmap != null && !qrBitmap.isRecycled) {

            val qrSize = 240f

            val qrRect = RectF(
                POSTER_WIDTH - RIGHT_MARGIN - qrSize,
                1550f,
                POSTER_WIDTH - RIGHT_MARGIN,
                1550f + qrSize
            )

            val qrBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
            }

            canvas.drawRoundRect(
                RectF(
                    qrRect.left - 12f,
                    qrRect.top - 12f,
                    qrRect.right + 12f,
                    qrRect.bottom + 12f
                ),
                12f,
                12f,
                qrBackgroundPaint
            )

            canvas.drawBitmap(
                qrBitmap,
                null,
                qrRect,
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            )
        }

        // -------------------------------------------------
        // 9. नीचे का फुटर
        // -------------------------------------------------

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = green
        }

        canvas.drawRect(
            0f,
            1840f,
            POSTER_WIDTH.toFloat(),
            POSTER_HEIGHT.toFloat(),
            footerPaint
        )

        val footerTextPaint = createTextPaint(
            color = Color.WHITE,
            size = 34f,
            bold = true,
            align = Paint.Align.CENTER
        )

        canvas.drawText(
            "MERA KISAN",
            POSTER_WIDTH / 2f,
            1890f,
            footerTextPaint
        )

        return poster
    }

    /**
     * उत्पाद का पोस्टर बनाकर cache directory में PNG के रूप में सेव करता है।
     */
    fun generatePoster(
        context: Context,
        product: ProductDto,
        productBitmap: Bitmap?
    ): Result<File> {

        var bitmap: Bitmap? = null

        return try {

            bitmap = generateProductPoster(
                context = context,
                product = product,
                productBitmap = productBitmap,
                qrBitmap = null
            )

            val file = File(
                context.cacheDir,
                "poster_${product.productId}.png"
            )

            FileOutputStream(file).use { output ->

                val success = bitmap.compress(
                    Bitmap.CompressFormat.PNG,
                    100,
                    output
                )

                if (!success) {
                    throw IllegalStateException(
                        "पोस्टर की PNG फ़ाइल तैयार नहीं हो सकी।"
                    )
                }

                output.flush()
            }

            Result.success(file)

        } catch (e: Exception) {

            Result.failure(e)

        } finally {

            // केवल इस फ़ंक्शन द्वारा बनाई गई Bitmap को मुक्त करें।
            bitmap?.takeIf { !it.isRecycled }?.recycle()
        }
    }

    /**
     * टेक्स्ट को उपलब्ध चौड़ाई में फिट करता है।
     * आवश्यकता होने पर Paint का textSize घटाता है।
     */
    private fun fitText(
        text: String,
        paint: Paint,
        maxWidth: Float,
        minTextSize: Float
    ): String {

        if (text.isBlank()) return text

        while (
            paint.measureText(text) > maxWidth &&
            paint.textSize > minTextSize
        ) {
            paint.textSize = maxOf(
                minTextSize,
                paint.textSize - 2f
            )
        }

        return text
    }

    /**
     * टेक्स्ट को शब्दों के अनुसार अलग-अलग लाइनों में बाँटता है।
     * यह हिंदी और अंग्रेज़ी दोनों टेक्स्ट के लिए उपयोगी है।
     */
    private fun wrapText(
        text: String,
        paint: Paint,
        maxWidth: Float
    ): List<String> {

        if (text.isBlank()) return listOf("")

        val words = text.trim().split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {

            val candidate = if (currentLine.isEmpty()) {
                word
            } else {
                "$currentLine $word"
            }

            if (
                currentLine.isEmpty() ||
                paint.measureText(candidate) <= maxWidth
            ) {
                currentLine = candidate
            } else {
                lines.add(currentLine)
                currentLine = word
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines.ifEmpty { listOf(text) }
    }
}
