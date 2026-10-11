// app/src/main/java/in/merakisan/app/data/repository/ProductRepository.kt
package `in`.merakisan.app.data.repository

import android.content.Context
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.network.model.ApiResponse
import `in`.merakisan.app.core.network.model.ProductDto
import `in`.merakisan.app.data.local.AppDatabase
import `in`.merakisan.app.data.local.entity.toDto
import `in`.merakisan.app.data.local.entity.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class ProductRepository(context: Context) {

    private val apiService = ApiClient.getApiService(context)
    private val productDao = AppDatabase.getInstance(context).productDao()

    fun getProducts(
        category: String? = null,
        isSmallQuantityOnly: Boolean = false,
        isOrganicOnly: Boolean = false
    ): Flow<List<ProductDto>> = flow {
        // 1. स्थानीय कैश से तुरंत डेटा दिखाएं
        val cached = productDao.getAllProducts().firstOrNull()?.map { it.toDto() } ?: emptyList()
        if (cached.isNotEmpty()) {
            emit(cached)
        }

        // 2. नेटवर्क से ताज़ा डेटा लाएं
        try {
            val response = apiService.getProducts(
                category = category,
                maxQuantity = if (isSmallQuantityOnly) 25.0 else null,
                organicOnly = if (isOrganicOnly) true else null,
                limit = 50,
                page = 1
            )
            if (response.isSuccessful && response.body()?.success == true) {
                val remoteList = response.body()?.data ?: emptyList()
                productDao.insertProducts(remoteList.map { it.toEntity() })
                emit(remoteList)
            }
        } catch (_: Exception) {
            // नेटवर्क विफलता पर स्थानीय कैश डेटा ही बना रहेगा
        }
    }.flowOn(Dispatchers.IO)

    // लाइन 91 टाइप सेफ़्टी: Flow<ProductEntity?> को Flow<ProductDto?> में सेफ़ली मैप करना
    fun getProductDetail(productId: String): Flow<ProductDto?> {
        return productDao.getProductById(productId).map { entity ->
            entity?.toDto()
        }.flowOn(Dispatchers.IO)
    }

    fun getProductDetails(productId: String): Flow<ProductDto?> = getProductDetail(productId)

    suspend fun createProduct(product: ProductDto): ApiResponse<ProductDto>? {
        return try {
            val response = apiService.createProduct(product)
            if (response.isSuccessful) {
                response.body()?.data?.let { saved ->
                    productDao.insertProduct(saved.toEntity())
                }
                response.body()
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
