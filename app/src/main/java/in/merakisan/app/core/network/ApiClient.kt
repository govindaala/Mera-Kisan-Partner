// app/src/main/java/in/merakisan/app/core/network/ApiClient.kt
package `in`.merakisan.app.core.network

import `in`.merakisan.app.core.config.FeatureManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "https://mera-kisan-backend.vercel.app/api/v1/"

    private val diagnosticsInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()
            .header("Accept", "application/json")
            .header("X-Request-ID", UUID.randomUUID().toString())

        val lastCrash = FeatureManager.consumeLastCrashSummary()
        if (!lastCrash.isNullOrBlank()) {
            requestBuilder.header("X-Client-Diagnostic", lastCrash.replace("\n", " ").take(180))
        }

        try {
            chain.proceed(requestBuilder.build())
        } catch (t: Throwable) {
            FeatureManager.recordCrashSummary("${original.method} ${original.url.encodedPath}:${t.javaClass.simpleName}")
            throw t
        }
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(diagnosticsInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun <T> createService(serviceClass: Class<T>): T {
        return retrofit.create(serviceClass)
    }
}
