// app/src/main/java/in/merakisan/app/core/utils/QrCodeGenerator.kt
package in.merakisan.app.core.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.EnumMap

/**
 * MERA KISAN Local QR Code Engine
 * डिवाइस पर ही तेज़ और मुफ़्त QR कोड बिटमैप जनरेट करने वाला इंजन
 */
object QrCodeGenerator {

    suspend fun generateQrBitmap(
        content: String,
        sizePx: Int = 400
    ): Result<Bitmap> = withContext(Dispatchers.Default) {
        try {
            if (content.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("सामग्री रिक्त नहीं हो सकती"))
            }

            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
                put(EncodeHintType.MARGIN, 1)
            }

            val bitMatrix = QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                hints
            )

            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)

            val colorDark = Color.parseColor("#1B5E20") // एग्रो-ग्रीन ब्रांड कलर
            val colorLight = Color.WHITE

            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) colorDark else colorLight
                }
            }

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

            Result.success(bitmap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
