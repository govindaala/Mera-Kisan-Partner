// app/src/main/java/in/merakisan/app/core/utils/SafeDateParser.kt
package `in`.merakisan.app.core.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Prevents NumberFormatException when backend sends ISO-8601 strings ("2026-10-19T...")
 * or epoch milliseconds (Long/String).
 */
object SafeDateParser {

    private val isoFormats = arrayOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd",
        "dd/MM/yyyy"
    )

    fun toEpochMillis(raw: Any?): Long {
        if (raw == null) return System.currentTimeMillis()
        if (raw is Number) return raw.toLong()

        val str = raw.toString().trim()
        if (str.isEmpty()) return System.currentTimeMillis()

        // 1. Check if pure numeric epoch string
        str.toLongOrNull()?.let { return it }

        // 2. Parse ISO-8601 or standard date formats
        for (pattern in isoFormats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val parsed = sdf.parse(str)
                if (parsed != null) return parsed.time
            } catch (_: Throwable) {}
        }

        return System.currentTimeMillis()
    }

    fun toDisplayDate(raw: Any?): String {
        if (raw == null) return "आज"
        val str = raw.toString().trim()
        if (str.isEmpty() || str == "आज" || str == "अभी") return str

        if (str.contains("T")) {
            return str.substringBefore("T")
        }

        str.toLongOrNull()?.let { millis ->
            return try {
                SimpleDateFormat("dd MMM yyyy", Locale("hi", "IN")).format(Date(millis))
            } catch (_: Throwable) {
                "आज"
            }
        }

        return str
    }
}
