// app/src/main/java/in/merakisan/app/data/local/dao/ProductDao.kt
package in.merakisan.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import in.merakisan.app.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM cached_products WHERE status = 'active' ORDER BY is_featured DESC, cached_at DESC")
    fun getAllCachedProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM cached_products WHERE category = :category AND status = 'active' ORDER BY cached_at DESC")
    fun getProductsByCategory(category: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM cached_products WHERE stock_quantity <= :maxQty OR min_order_quantity <= :maxQty AND status = 'active' ORDER BY cached_at DESC")
    fun getSmallQuantityProducts(maxQty: Double = 25.0): Flow<List<ProductEntity>>

    @Query("""
        SELECT * FROM cached_products 
        WHERE status = 'active' AND (
            name LIKE '%' || :query || '%' OR 
            variety LIKE '%' || :query || '%' OR 
            seller_name LIKE '%' || :query || '%' OR 
            district LIKE '%' || :query || '%'
        )
        ORDER BY cached_at DESC
    """)
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM cached_products WHERE product_id = :productId LIMIT 1")
    suspend fun getProductById(productId: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Query("DELETE FROM cached_products WHERE cached_at < :thresholdTimestamp")
    suspend fun evictOldCache(thresholdTimestamp: Long)

    @Query("DELETE FROM cached_products")
    suspend fun clearAll()
}
