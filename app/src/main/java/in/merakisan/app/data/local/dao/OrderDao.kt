// app/src/main/java/in/merakisan/app/data/local/dao/OrderDao.kt

package `in`.merakisan.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import `in`.merakisan.app.data.local.entity.OrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    /**
     * सभी स्थानीय orders प्राप्त करें।
     * वास्तविक Room table: orders
     */
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    /**
     * Order की वास्तविक Primary Key orderId के आधार पर order प्राप्त करें।
     */
    @Query("SELECT * FROM orders WHERE orderId = :id LIMIT 1")
    suspend fun getOrderById(id: String): OrderEntity?

    /**
     * Orders की सूची स्थानीय डेटाबेस में सेव करें।
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<OrderEntity>)

    /**
     * एक order स्थानीय डेटाबेस में सेव करें।
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    /**
     * दिए गए order को स्थानीय डेटाबेस से हटाएँ।
     */
    @Delete
    suspend fun deleteOrder(order: OrderEntity)

    /**
     * स्थानीय orders cache खाली करें।
     */
    @Query("DELETE FROM orders")
    suspend fun clearOrders()
}
