// app/src/main/java/in/merakisan/app/data/local/dao/ProductDao.kt
package `in`.merakisan.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import `in`.merakisan.app.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

/**
 * MERA KISAN Room Database Data Access Object (DAO)
 * ऑफ़लाइन-फ़र्स्ट आर्किटेक्चर एवं सुरक्षित कैश प्रबंधन
 */
@Dao
interface ProductDao {

    @Query("SELECT * FROM cached_products ORDER BY createdAt DESC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM cached_products WHERE category = :category ORDER BY createdAt DESC")
    fun getProductsByCategory(category: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM cached_products WHERE sellerUid = :sellerUid ORDER BY createdAt DESC")
    fun getProductsBySeller(sellerUid: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM cached_products WHERE isFeatured = 1 OR boosted = 1 ORDER BY createdAt DESC")
    fun getFeaturedProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM cached_products WHERE productId = :productId LIMIT 1")
    fun getProductById(productId: String): Flow<ProductEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Query("DELETE FROM cached_products WHERE productId = :productId")
    suspend fun deleteProductById(productId: String)

    @Query("DELETE FROM cached_products")
    suspend fun clearAllProducts()
}
