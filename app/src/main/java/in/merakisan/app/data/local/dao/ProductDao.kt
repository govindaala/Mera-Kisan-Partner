// app/src/main/java/in/merakisan/app/data/local/dao/ProductDao.kt
package `in`.merakisan.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import `in`.merakisan.app.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query(
        "SELECT * FROM cached_products ORDER BY createdAt DESC"
    )
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query(
        "SELECT * FROM cached_products WHERE productId = :id LIMIT 1"
    )
    fun getProductById(id: String): Flow<ProductEntity?>

    @Query(
        "SELECT * FROM cached_products WHERE category = :category ORDER BY createdAt DESC"
    )
    fun getProductsByCategory(
        category: String
    ): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(
        products: List<ProductEntity>
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(
        product: ProductEntity
    )

    @Delete
    suspend fun deleteProduct(
        product: ProductEntity
    )

    @Query("DELETE FROM cached_products")
    suspend fun clearProducts()
}
