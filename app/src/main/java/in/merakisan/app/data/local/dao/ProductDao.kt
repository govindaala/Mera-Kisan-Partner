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

    /**
     * सभी cached products प्राप्त करें।
     * वास्तविक Room table: cached_products
     */
    @Query("SELECT * FROM cached_products ORDER BY createdAt DESC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    /**
     * Product की Primary Key productId के आधार पर product प्राप्त करें।
     */
    @Query("SELECT * FROM cached_products WHERE productId = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?

    /**
     * Products की सूची स्थानीय डेटाबेस में सेव करें।
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    /**
     * एक product स्थानीय डेटाबेस में सेव करें।
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    /**
     * दिए गए product को स्थानीय डेटाबेस से हटाएँ।
     */
    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    /**
     * स्थानीय product cache खाली करें।
     */
    @Query("DELETE FROM cached_products")
    suspend fun clearProducts()
}
