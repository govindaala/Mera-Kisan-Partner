// app/src/main/java/in/merakisan/app/core/network/ApiClient.kt
package `in`.merakisan.app.core.network

import android.content.Context
import `in`.merakisan.app.BuildConfig
import `in`.merakisan.app.core.security.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * MERA KISAN Network Client Provider
 * सुरक्षित टोकन ऑथराइजेशन एवं टाइमआउट कॉन्फ़िगरेशन
 */
object ApiClient {

    private const val TIMEOUT_SECONDS = 30L
    private var retrofit: Retrofit? = null
    private var apiService: ApiService? = null

    private fun createOkHttpClient(context: Context): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val token = SessionManager.getAuthToken(context)

            val requestBuilder = originalRequest.newBuilder()
                .header("Accept", "application/json")
                .header("X-Client-Platform", "Android")

            if (!token.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }

            chain.proceed(requestBuilder.build())
        }

        return OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    fun getApiService(context: Context): ApiService {
        return apiService ?: synchronized(this) {
            val client = createOkHttpClient(context.applicationContext)
            val newRetrofit = Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            retrofit = newRetrofit
            val service = newRetrofit.create(ApiService::class.java)
            apiService = service
            service
        }
    }
}
