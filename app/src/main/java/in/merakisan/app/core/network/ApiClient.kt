package in.merakisan.app.core.network

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "https://mera-kisan-backend.vercel.app/api/"

    private var authTokenProvider: (() -> String?)? = null

    fun setAuthTokenProvider(provider: () -> String?) {
        authTokenProvider = provider
    }

    // Safe String Adapter to prevent null/type mismatch crashes
    private val safeStringAdapter = object : TypeAdapter<String>() {
        override fun write(out: JsonWriter, value: String?) {
            out.value(value ?: "")
        }
        override fun read(reader: JsonReader): String {
            return when (reader.peek()) {
                JsonToken.NULL -> { reader.nextNull(); "" }
                JsonToken.BOOLEAN -> reader.nextBoolean().toString()
                JsonToken.NUMBER -> reader.nextString()
                JsonToken.STRING -> reader.nextString()
                else -> { reader.skipValue(); "" }
            }
        }
    }

    // Safe Long Adapter
    private val safeLongAdapter = object : TypeAdapter<Long>() {
        override fun write(out: JsonWriter, value: Long?) {
            out.value(value ?: 0L)
        }
        override fun read(reader: JsonReader): Long {
            return when (reader.peek()) {
                JsonToken.NULL -> { reader.nextNull(); 0L }
                JsonToken.NUMBER -> reader.nextLong()
                JsonToken.STRING -> {
                    val str = reader.nextString()
                    str.toLongOrNull() ?: 0L
                }
                else -> { reader.skipValue(); 0L }
            }
        }
    }

    // Safe Double Adapter
    private val safeDoubleAdapter = object : TypeAdapter<Double>() {
        override fun write(out: JsonWriter, value: Double?) {
            out.value(value ?: 0.0)
        }
        override fun read(reader: JsonReader): Double {
            return when (reader.peek()) {
                JsonToken.NULL -> { reader.nextNull(); 0.0 }
                JsonToken.NUMBER -> reader.nextDouble()
                JsonToken.STRING -> {
                    val str = reader.nextString()
                    str.toDoubleOrNull() ?: 0.0
                }
                else -> { reader.skipValue(); 0.0 }
            }
        }
    }

    private val gson = GsonBuilder()
        .registerTypeAdapter(String::class.java, safeStringAdapter)
        .registerTypeAdapter(Long::class.java, safeLongAdapter)
        .registerTypeAdapter(Long.javaPrimitiveType, safeLongAdapter)
        .registerTypeAdapter(Double::class.java, safeDoubleAdapter)
        .registerTypeAdapter(Double.javaPrimitiveType, safeDoubleAdapter)
        .setLenient()
        .create()

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = authTokenProvider?.invoke()
        val requestBuilder = original.newBuilder()
        
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        
        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}
