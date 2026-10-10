// app/src/main/java/in/merakisan/app/core/utils/ImageUtils.kt
package in.merakisan.app.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/**
 * MERA KISAN Local Image Optimization Engine
 * Free-First Baseline: डिवाइस पर ही इमेज रीसाइज़ और कम्प्रेस करना ताकि नेटवर्क और स्टोरेज लागत न्यूनतम रहे
 */
object ImageUtils {

    private const val MAX_DIMENSION = 1280
    private const val JPEG_QUALITY = 80

    suspend fun compressImage(context: Context, imageUri: Uri): Result<File> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return@withContext Result.failure(Exception("इमेज फ़ाइल नहीं पढ़ी जा सकी"))

            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) {
                return@withContext Result.failure(Exception("अमान्य इमेज फ़ाइल"))
            }

            // आनुपातिक रीसाइज़िंग (Max 1280px)
            val width = originalBitmap.width
            val height = originalBitmap.height
            val maxSide = max(width, height)

            val scaledBitmap = if (maxSide > MAX_DIMENSION) {
                val scaleFactor = MAX_DIMENSION.toFloat() / maxSide
                val targetWidth = (width * scaleFactor).toInt()
                val targetHeight = (height * scaleFactor).toInt()
                Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
            } else {
                originalBitmap
            }

            // 80% JPEG कम्प्रेशन (फ़ाइल साइज़ 150-300 KB के बीच सुरक्षित रहता है)
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)

            val tempFile = File(context.cacheDir, "crop_${System.currentTimeMillis()}.jpg")
            val fileOutputStream = FileOutputStream(tempFile)
            fileOutputStream.write(outputStream.toByteArray())
            fileOutputStream.flush()
            fileOutputStream.close()

            if (scaledBitmap != originalBitmap) {
                scaledBitmap.recycle()
            }
            originalBitmap.recycle()

            Result.success(tempFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
