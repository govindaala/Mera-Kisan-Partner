// app/src/main/java/in/merakisan/app/data/local/dao/OrderDao.kt
package `in`.merakisan.app.data.local.dao

import androidx.room.*
import `in`.merakisan.app.data.local.entity.OrderEntity
import kotlinx.coroutines.flow.Flow

/**
 * MERA KISAN — ऑर्डर रूम DAO
 * किसान और खरीदार दोनों भूमिकाओं के लिए रिएक्टिव ऑफ़लाइन डेटा स्ट्रीम
 */
@Dao
interface OrderDao {

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrdersFlow(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE buyerUid = :buyerUid ORDER BY createdAt DESC")
    fun getOrdersByBuyer(buyerUid: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE sellerUid = :sellerUid ORDER BY createdAt DESC")
    fun getOrdersBySeller(sellerUid: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE orderId = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: String): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<OrderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Query("UPDATE orders SET orderStatus = :newStatus, updatedAt = :timestamp WHERE orderId = :orderId")
    suspend fun updateOrderStatus(orderId: String, newStatus: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM orders WHERE orderId = :orderId")
    suspend fun deleteOrderById(orderId: String)

    @Query("DELETE FROM orders")
    suspend fun clearAllOrders()
}
