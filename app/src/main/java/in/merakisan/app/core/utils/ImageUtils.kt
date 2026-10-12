// app/src/main/java/in/merakisan/app/core/utils/ImageUtils.kt
package `in`.merakisan.app.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

/**
 * MERA KISAN — Local Image Optimizer & Memory Manager
 * Master Spec v1.1 Section 12, 24 & 66: Free-First Client-Side Compression
 */
object ImageUtils {

    private const val MAX_WIDTH = 1280
    private const val MAX_HEIGHT = 1280
    private const val TARGET_MAX_BYTES = 350 * 1024L // 350 KB optimal size

    /**
     * URI se image ko memory-safely read karke compress karta hai aur local cache File return karta hai.
     */
    fun compressImageToFile(context: Context, imageUri: Uri): File? {
        return try {
            val sampledBitmap = decodeSampledBitmapFromUri(context, imageUri, MAX_WIDTH, MAX_HEIGHT) ?: return null
            val orientedBitmap = fixExifOrientation(context, imageUri, sampledBitmap)

            val tempFile = File(context.cacheDir, "crop_${UUID.randomUUID().toString().take(8)}.jpg")
            var quality = 85
            var streamLength: Int

            do {
                ByteArrayOutputStream().use { byteStream ->
                    orientedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteStream)
                    val bytes = byteStream.toByteArray()
                    streamLength = bytes.size
                    if (streamLength <= TARGET_MAX_BYTES || quality <= 45) {
                        FileOutputStream(tempFile).use { fileOut ->
                            fileOut.write(bytes)
                        }
                    }
                }
                quality -= 10
            } while (streamLength > TARGET_MAX_BYTES && quality >= 45)

            if (orientedBitmap != sampledBitmap) {
                sampledBitmap.recycle()
            }
            orientedBitmap.recycle()

            tempFile
        } catch (e: Exception) {
            null
        }
    }

    /**
     * InSampleSize calculate karke OutOfMemoryError se bachata hai.
     */
    fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565 // Memory usage 50% kam karta hai

            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }
        } catch (e: OutOfMemoryError) {
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Kisaan ke phone ki camera orientation (EXIF) check karke image ko seedha karta hai.
     */
    private fun fixExifOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val orientation = context.contentResolver.openInputStream(uri)?.use { input: InputStream ->
                val exif = ExifInterface(input)
                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return bitmap
            }

            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            rotated
        } catch (e: Exception) {
            bitmap
        }
    }

    fun compressBitmapToBytes(bitmap: Bitmap, quality: Int = 80): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }
}
