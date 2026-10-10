// app/src/main/java/in/merakisan/app/data/local/AppDatabase.kt
package `in`.merakisan.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import `in`.merakisan.app.data.local.dao.ProductDao
import `in`.merakisan.app.data.local.entity.ProductEntity

/**
 * MERA KISAN Local Cache Database
 * ऑफ़लाइन-फ़र्स्ट आर्किटेक्चर एवं सुरक्षित टाइप कनवर्टर बाइंडिंग
 */
@Database(
    entities = [ProductEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mera_kisan_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
