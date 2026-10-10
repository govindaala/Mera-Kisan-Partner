// app/src/main/java/in/merakisan/app/data/local/Converters.kt
package `in`.merakisan.app.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * MERA KISAN Room Type Converters
 * List<String> को SQLite डेटाबेस में सुरक्षित JSON स्ट्रिंग में बदलने हेतु
 */
class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return gson.toJson(value ?: emptyList<String>())
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        val listType = object : TypeToken<List<String>>() {}.type
        return try {
            gson.fromJson(value, listType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
