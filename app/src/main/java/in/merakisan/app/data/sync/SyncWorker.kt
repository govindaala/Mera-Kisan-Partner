// app/src/main/java/in/merakisan/app/data/sync/SyncWorker.kt
package in.merakisan.app.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import in.merakisan.app.core.network.ApiClient
import in.merakisan.app.data.local.AppDatabase
import in.merakisan.app.data.local.entity.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MERA KISAN Background Offline Sync Worker
 * नेटवर्क कनेक्ट होते ही रूम डेटाबेस को Vercel REST बैकएंड से ऑटोमैटिक सिंक करता है
 */
class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val apiService = ApiClient.getApiService(applicationContext)
            val productDao = AppDatabase.getInstance(applicationContext).productDao()

            val response = apiService.getProducts(limit = 100, page = 1)
            if (response.isSuccessful && response.body()?.success == true) {
                val remoteList = response.body()?.data ?: emptyList()
                if (remoteList.isNotEmpty()) {
                    val entities = remoteList.map { it.toEntity() }
                    productDao.insertProducts(entities)
                }
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
