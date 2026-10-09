// app/src/main/java/in/merakisan/app/core/network/ApiService.kt
package in.merakisan.app.core.network

import in.merakisan.app.core.network.model.ApiResponse
import in.merakisan.app.core.network.model.AppConfigResponse
import in.merakisan.app.core.network.model.CreateOrderRequest
import in.merakisan.app.core.network.model.OrderDto
import in.merakisan.app.core.network.model.ProductDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * MERA KISAN Backend REST API Contracts (Vercel Endpoint Definitions)
 */
interface ApiService {

    // 1. Remote Config & 40 Feature Flags (/v1/config)
    @GET("config")
    suspend fun getAppConfig(): Response<ApiResponse<AppConfigResponse>>

    // 2. Marketplace: Get All Products with Filters & Small Quantity Bounds
    @GET("products")
    suspend fun getProducts(
        @Query("category") category: String? = null,
        @Query("district") district: String? = null,
        @Query("max_quantity") maxQuantity: Double? = null, // 1-25 kg small quantity filter
        @Query("organic_only") organicOnly: Boolean? = null,
        @Query("limit") limit: Int = 20,
        @Query("page") page: Int = 1
    ): Response<ApiResponse<List<ProductDto>>>

    // 3. Marketplace: Get Single Product Details
    @GET("products/{id}")
    suspend fun getProductDetails(
        @Path("id") productId: String
    ): Response<ApiResponse<ProductDto>>

    // 4. Farmer: Create / List Produce
    @POST("products")
    suspend fun createProduct(
        @Body product: ProductDto
    ): Response<ApiResponse<ProductDto>>

    // 5. Orders: Create Order with Idempotency Key (No duplicate charges on network retry)
    @POST("orders")
    suspend fun createOrder(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: CreateOrderRequest
    ): Response<ApiResponse<OrderDto>>

    // 6. Orders: Get User Orders (Farmer or Buyer)
    @GET("orders")
    suspend fun getUserOrders(
        @Query("role") role: String // "farmer" or "buyer"
    ): Response<ApiResponse<List<OrderDto>>>

    // 7. Orders: Get Order Status Timeline
    @GET("orders/{id}")
    suspend fun getOrderById(
        @Path("id") orderId: String
    ): Response<ApiResponse<OrderDto>>
}
