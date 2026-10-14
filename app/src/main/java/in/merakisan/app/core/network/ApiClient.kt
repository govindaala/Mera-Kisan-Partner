// app/src/main/java/in/merakisan/app/core/network/ApiClient.kt
package `in`.merakisan.app.core.network

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import `in`.merakisan.app.core.network.model.ProductDto
import `in`.merakisan.app.core.security.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "https://mera-kisan-backend.vercel.app/"
    private const val CRASH_PREFS = "mera_kisan_crash_diagnostics"
    private const val KEY_LAST_CRASH = "last_crash_trace"

    @Volatile
    private var apiService: ApiService? = null

    @Volatile
    private var crashHandlerInstalled = false

    fun installCrashDiagnosticHandler(context: Context) {
        if (crashHandlerInstalled) return
        synchronized(this) {
            if (crashHandlerInstalled) return
            val appCtx = context.applicationContext
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                try {
                    val topFrame = throwable.stackTrace.firstOrNull {
                        it.className.contains("merakisan")
                    } ?: throwable.stackTrace.firstOrNull()

                    val summary = "${throwable.javaClass.simpleName}:${throwable.localizedMessage ?: ""} " +
                            "at ${topFrame?.fileName}:${topFrame?.lineNumber}"

                    appCtx.getSharedPreferences(CRASH_PREFS, Context.MODE_PRIVATE)
                        .edit()
                        .putString(KEY_LAST_CRASH, summary)
                        .commit()
                } catch (_: Throwable) {
                }
                defaultHandler?.uncaughtException(thread, throwable)
            }
            crashHandlerInstalled = true
        }
    }

    fun consumeLastCrashSummary(context: Context): String? {
        val prefs = context.applicationContext.getSharedPreferences(CRASH_PREFS, Context.MODE_PRIVATE)
        val trace = prefs.getString(KEY_LAST_CRASH, null)
        if (!trace.isNullOrBlank()) {
            prefs.edit().remove(KEY_LAST_CRASH).apply()
        }
        return trace
    }

    fun getApiService(context: Context): ApiService {
        installCrashDiagnosticHandler(context)
        return apiService ?: synchronized(this) {
            val sessionManager = SessionManager(context.applicationContext)

            val authInterceptor = Interceptor { chain ->
                val originalRequest = chain.request()
                val token = try {
                    sessionManager.getAuthToken()
                } catch (_: Throwable) {
                    null
                }

                val requestBuilder = originalRequest.newBuilder()
                    .header("Accept", "application/json")
                    .header("X-App-Platform", "Android")

                if (!token.isNullOrBlank() && token != "guest_session" && !token.startsWith("guest_")) {
                    requestBuilder.header("Authorization", "Bearer $token")
                }

                chain.proceed(requestBuilder.build())
            }

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(25, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .writeTimeout(25, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()

            val safeLongAdapter = object : TypeAdapter<Long>() {
                override fun write(out: JsonWriter, value: Long?) {
                    if (value == null) out.nullValue() else out.value(value)
                }

                override fun read(reader: JsonReader): Long {
                    return when (reader.peek()) {
                        JsonToken.NULL -> {
                            reader.nextNull()
                            0L
                        }
                        JsonToken.NUMBER -> {
                            try {
                                reader.nextDouble().toLong()
                            } catch (_: Throwable) {
                                0L
                            }
                        }
                        JsonToken.STRING -> {
                            val str = reader.nextString()
                            str.toLongOrNull() ?: str.toDoubleOrNull()?.toLong() ?: System.currentTimeMillis()
                        }
                        JsonToken.BEGIN_OBJECT -> {
                            var seconds = 0L
                            reader.beginObject()
                            while (reader.hasNext()) {
                                val name = reader.nextName()
                                if (name == "_seconds" || name == "seconds") {
                                    seconds = try { reader.nextLong() } catch (_: Throwable) { reader.skipValue(); 0L }
                                } else {
                                    reader.skipValue()
                                }
                            }
                            reader.endObject()
                            if (seconds > 0L) seconds * 1000L else System.currentTimeMillis()
                        }
                        else -> {
                            reader.skipValue()
                            0L
                        }
                    }
                }
            }

            val safeDoubleAdapter = object : TypeAdapter<Double>() {
                override fun write(out: JsonWriter, value: Double?) {
                    if (value == null) out.nullValue() else out.value(value)
                }

                override fun read(reader: JsonReader): Double {
                    return when (reader.peek()) {
                        JsonToken.NULL -> {
                            reader.nextNull()
                            0.0
                        }
                        JsonToken.NUMBER -> {
                            try { reader.nextDouble() } catch (_: Throwable) { 0.0 }
                        }
                        JsonToken.STRING -> {
                            reader.nextString().toDoubleOrNull() ?: 0.0
                        }
                        else -> {
                            reader.skipValue()
                            0.0
                        }
                    }
                }
            }

            val postSanitizeFactory = object : TypeAdapterFactory {
                override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T> {
                    val delegate = gson.getDelegateAdapter(this, type)
                    return object : TypeAdapter<T>() {
                        override fun write(out: JsonWriter, value: T) {
                            delegate.write(out, value)
                        }

                        @Suppress("UNCHECKED_CAST")
                        override fun read(reader: JsonReader): T? {
                            val result = try {
                                delegate.read(reader)
                            } catch (_: Throwable) {
                                if (reader.peek() != JsonToken.END_DOCUMENT) {
                                    try { reader.skipValue() } catch (_: Throwable) {}
                                }
                                null
                            }
                            return if (result is ProductDto) {
                                result.sanitize() as T
                            } else {
                                result
                            }
                        }
                    }
                }
            }

            val gson = GsonBuilder()
                .setLenient()
                .registerTypeAdapter(Long::class.java, safeLongAdapter)
                .registerTypeAdapter(java.lang.Long::class.java, safeLongAdapter)
                .registerTypeAdapter(Double::class.java, safeDoubleAdapter)
                .registerTypeAdapter(java.lang.Double::class.java, safeDoubleAdapter)
                .registerTypeAdapterFactory(postSanitizeFactory)
                .create()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()

            val service = retrofit.create(ApiService::class.java)
            apiService = service
            service
        }
    }
}
