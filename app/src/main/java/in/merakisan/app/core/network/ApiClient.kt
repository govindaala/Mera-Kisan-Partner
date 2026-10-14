// app/src/main/java/in/merakisan/app/core/network/ApiClient.kt
package `in`.merakisan.app.core.network

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
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

    fun consumeLastCrashSummary(context: Context): String? {
        val prefs = context.applicationContext.getSharedPreferences(CRASH_PREFS, Context.MODE_PRIVATE)
        val trace = prefs.getString(KEY_LAST_CRASH, null)
        if (!trace.isNullOrBlank()) {
            prefs.edit().remove(KEY_LAST_CRASH).apply()
        }
        return trace
    }

    fun getApiService(context: Context): ApiService {
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

            val safeStringAdapter = object : TypeAdapter<String?>() {
                override fun write(out: JsonWriter, value: String?) {
                    if (value == null) out.nullValue() else out.value(value)
                }

                override fun read(reader: JsonReader): String? {
                    return when (reader.peek()) {
                        JsonToken.NULL -> {
                            reader.nextNull()
                            ""
                        }
                        JsonToken.STRING -> reader.nextString()
                        JsonToken.NUMBER -> reader.nextString()
                        JsonToken.BOOLEAN -> reader.nextBoolean().toString()
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
                            if (seconds > 0L) (seconds * 1000L).toString() else ""
                        }
                        else -> {
                            reader.skipValue()
                            ""
                        }
                    }
                }
            }

            val gson = GsonBuilder()
                .setLenient()
                .registerTypeAdapter(String::class.java, safeStringAdapter)
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
