package `in`.merakisan.app.core.network

import android.content.Context
import `in`.merakisan.app.BuildConfig
import `in`.merakisan.app.core.security.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val FALLBACK_BASE_URL = "https://mera-kisan-backend.vercel.app/"
    private var apiService: ApiService? = null

    fun getApiService(context: Context): ApiService {
        return apiService ?: synchronized(this) {
            apiService ?: buildRetrofit(context).create(ApiService::class.java).also {
                apiService = it
            }
        }
    }

    private fun buildRetrofit(context: Context): Retrofit {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("Accept", "application/json")
                    .header("X-Client-Platform", "Android")

                val token = SessionManager.getAuthToken(context)
                if (!token.isNullOrBlank()) {
                    requestBuilder.header("Authorization", "Bearer $token")
                }

                chain.proceed(requestBuilder.build())
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val resolvedBaseUrl = try {
            BuildConfig.BASE_URL.ifBlank { FALLBACK_BASE_URL }
        } catch (_: Exception) {
            FALLBACK_BASE_URL
        }

        return Retrofit.Builder()
            .baseUrl(resolvedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
