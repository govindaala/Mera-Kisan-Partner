// app/src/main/java/in/merakisan/app/data/local/AppDatabase.kt
package `in`.merakisan.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import `in`.merakisan.app.data.local.dao.OrderDao
import `in`.merakisan.app.data.local.dao.ProductDao
import `in`.merakisan.app.data.local.entity.OrderEntity
import `in`.merakisan.app.data.local.entity.ProductEntity

@Database(
    entities = [
        ProductEntity::class,
        OrderEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao

    abstract fun orderDao(): OrderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return getDatabase(context)
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mera_kisan_database"
                ).build().also {
                    INSTANCE = it
                }
            }
        }
    }
}
