// app/src/main/java/in/merakisan/app/core/network/ApiClient.kt
package in.merakisan.app.core.network

import android.content.Context
import in.merakisan.app.BuildConfig
import in.merakisan.app.core.security.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit and OkHttp Engine with Encrypted Token Injection
 */
object ApiClient {

    private var retrofit: Retrofit? = null

    fun getApiService(context: Context): ApiService {
        if (retrofit == null) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }

            // Auth Header Interceptor: Firebase ID Token injection
            val authInterceptor = Interceptor { chain ->
                val originalRequest = chain.request()
                val requestBuilder = originalRequest.newBuilder()
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")

                // SessionManager से Keystore-backed टोकन प्राप्त करना
                val token = SessionManager.getAuthToken(context)
                if (!token.isNullOrBlank()) {
                    requestBuilder.header("Authorization", "Bearer $token")
                }

                chain.proceed(requestBuilder.build())
            }

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .retryOnConnectionFailure(true)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        return retrofit!!.create(ApiService::class.java)
    }
}
