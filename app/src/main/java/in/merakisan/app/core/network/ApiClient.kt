package `in`.merakisan.app.core.network

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import `in`.merakisan.app.core.security.AuthTokenProvider
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "https://mera-kisan-backend.vercel.app/api/"

    @Volatile
    private var appContext: Context? = null

    private var customTokenProvider: (() -> String?)? = null

    fun setAuthTokenProvider(provider: () -> String?) {
        customTokenProvider = provider
    }

    private val safeStringAdapter = object : TypeAdapter<String>() {
        override fun write(out: JsonWriter, value: String?) {
            out.value(value ?: "")
        }

        override fun read(reader: JsonReader): String {
            return when (reader.peek()) {
                JsonToken.NULL -> {
                    reader.nextNull()
                    ""
                }
                JsonToken.BOOLEAN -> reader.nextBoolean().toString()
                JsonToken.NUMBER -> reader.nextString()
                JsonToken.STRING -> reader.nextString()
                else -> {
                    reader.skipValue()
                    ""
                }
            }
        }
    }

    private val safeLongAdapter = object : TypeAdapter<Long>() {
        override fun write(out: JsonWriter, value: Long?) {
            out.value(value ?: 0L)
        }

        override fun read(reader: JsonReader): Long {
            return when (reader.peek()) {
                JsonToken.NULL -> {
                    reader.nextNull()
                    0L
                }
                JsonToken.NUMBER -> {
                    try {
                        reader.nextLong()
                    } catch (_: Throwable) {
                        reader.nextDouble().toLong()
                    }
                }
                JsonToken.STRING -> {
                    val str = reader.nextString()
                    str.toLongOrNull() ?: str.toDoubleOrNull()?.toLong() ?: 0L
                }
                else -> {
                    reader.skipValue()
                    0L
                }
            }
        }
    }

    private val safeDoubleAdapter = object : TypeAdapter<Double>() {
        override fun write(out: JsonWriter, value: Double?) {
            out.value(value ?: 0.0)
        }

        override fun read(reader: JsonReader): Double {
            return when (reader.peek()) {
                JsonToken.NULL -> {
                    reader.nextNull()
                    0.0
                }
                JsonToken.NUMBER -> reader.nextDouble()
                JsonToken.STRING -> {
                    val str = reader.nextString()
                    str.toDoubleOrNull() ?: 0.0
                }
                else -> {
                    reader.skipValue()
                    0.0
                }
            }
        }
    }

    private val gson = GsonBuilder()
        .registerTypeAdapter(String::class.java, safeStringAdapter)
        .registerTypeAdapter(Long::class.java, safeLongAdapter)
        .registerTypeAdapter(Long::class.javaPrimitiveType, safeLongAdapter)
        .registerTypeAdapter(Double::class.java, safeDoubleAdapter)
        .registerTypeAdapter(Double::class.javaPrimitiveType, safeDoubleAdapter)
        .setLenient()
        .create()

    private fun resolveAuthToken(): String? {
        customTokenProvider?.invoke()?.takeIf { it.isNotBlank() }?.let { return it }

        try {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                val tokenResult = Tasks.await(user.getIdToken(false), 5, TimeUnit.SECONDS)
                if (!tokenResult?.token.isNullOrBlank()) {
                    return tokenResult.token
                }
            }
        } catch (_: Throwable) {
        }

        return null
    }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()

        val token = resolveAuthToken()
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }

    fun getApiService(context: Context? = null): ApiService {
        if (context != null && appContext == null) {
            appContext = context.applicationContext
        }
        return apiService
    }
}
