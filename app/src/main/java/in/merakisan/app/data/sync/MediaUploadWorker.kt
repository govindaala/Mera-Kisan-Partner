// app/src/main/java/in/merakisan/app/data/sync/MediaUploadWorker.kt
package in.merakisan.app.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import in.merakisan.app.core.config.FeatureManager
import in.merakisan.app.core.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

/**
 * MERA KISAN Background Media Upload Worker
 * YouTube कोटा (अपडेटेड 100 वीडियो/दिन) और ड्राइव बैकग्राउंड अपलोड हैंडलर
 */
class MediaUploadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val filePath = inputData.getString("FILE_PATH") ?: return@withContext Result.failure()
        val productId = inputData.getString("PRODUCT_ID") ?: return@withContext Result.failure()
        val mediaType = inputData.getString("MEDIA_TYPE") ?: "VIDEO"

        val file = File(filePath)
        if (!file.exists()) {
            return@withContext Result.failure()
        }

        try {
            val apiService = ApiClient.getApiService(applicationContext)

            // रिमोट कॉन्फ़िगरेशन से दैनिक सीमा जांचना (डिफ़ॉल्ट = 100 वीडियो/दिन)
            val isVideoEnabled = FeatureManager.isEnabled(applicationContext, "video")
            if (mediaType == "VIDEO" && !isVideoEnabled) {
                // फ़ीचर बंद होने पर स्थानीय रूप से सुरक्षित होल्ड
                return@withContext Result.success()
            }

            val requestFile = file.asRequestBody(
                if (mediaType == "VIDEO") "video/mp4".toMediaTypeOrNull() else "image/jpeg".toMediaTypeOrNull()
            )
            val body = MultipartBody.Part.createFormData("media", file.name, requestFile)

            // Vercel बैकएंड API कॉल (यह सर्वर-साइड 100/दिन YouTube बकेट को रूट करता है)
            val response = apiService.uploadMedia(productId, mediaType, body)

            if (response.isSuccessful && response.body()?.success == true) {
                // अपलोड सफल होने पर स्थानीय कैश फ़ाइल साफ़ करना (स्टोरेज सुरक्षा)
                if (file.exists()) {
                    file.delete()
                }
                Result.success()
            } else {
                // कोटा पूरा होने या सर्वर त्रुटि पर बाद में पुनः प्रयास (Retry Queue)
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
