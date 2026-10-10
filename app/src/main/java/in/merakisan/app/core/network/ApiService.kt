// app/src/main/java/in/merakisan/app/core/network/ApiService.kt
package `in`.merakisan.app.core.network

import `in`.merakisan.app.core.network.model.ApiResponse
import `in`.merakisan.app.core.network.model.AppConfigResponse
import `in`.merakisan.app.core.network.model.BuyerRequestDto
import `in`.merakisan.app.core.network.model.CreateOrderRequest
import `in`.merakisan.app.core.network.model.MandiPriceDto
import `in`.merakisan.app.core.network.model.MediaUploadResponse
import `in`.merakisan.app.core.network.model.OrderDto
import `in`.merakisan.app.core.network.model.ProductDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * MERA KISAN Unified REST API Contract
 */
interface ApiService {

    // 1. Marketplace & Products
    @GET("/v1/products")
    suspend fun getProducts(
        @Query("category") category: String? = null,
        @Query("query") query: String? = null,
        @Query("maxQuantity") maxQuantity: Double? = null,
        @Query("organicOnly") organicOnly: Boolean? = null,
        @Query("limit") limit: Int? = 50,
        @Query("page") page: Int = 1
    ): Response<ApiResponse<List<ProductDto>>>

    @GET("/v1/products/{id}")
    suspend fun getProductDetail(
        @Path("id") productId: String
    ): Response<ApiResponse<ProductDto>>

    @GET("/v1/products/{id}")
    suspend fun getProductDetails(
        @Path("id") productId: String
    ): Response<ApiResponse<ProductDto>>

    @POST("/v1/products")
    suspend fun createProduct(
        @Body product: ProductDto
    ): Response<ApiResponse<ProductDto>>

    // 2. Buyer Requests ("मुझे चाहिए")
    @GET("/v1/requests")
    suspend fun getBuyerRequests(): Response<ApiResponse<List<BuyerRequestDto>>>

    @POST("/v1/requests")
    suspend fun createBuyerRequest(
        @Body request: BuyerRequestDto
    ): Response<ApiResponse<BuyerRequestDto>>

    // 3. Orders & State Transitions
    @GET("/v1/orders")
    suspend fun getOrders(): Response<ApiResponse<List<OrderDto>>>

    @GET("/v1/orders")
    suspend fun getUserOrders(
        @Query("role") role: String? = null
    ): Response<ApiResponse<List<OrderDto>>>

    @GET("/v1/orders/{id}")
    suspend fun getOrderDetail(
        @Path("id") orderId: String
    ): Response<ApiResponse<OrderDto>>

    @POST("/v1/orders")
    suspend fun createOrder(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: CreateOrderRequest
    ): Response<ApiResponse<OrderDto>>

    @PATCH("/v1/orders/{id}/status")
    suspend fun updateOrderStatus(
        @Path("id") orderId: String,
        @Query("status") nextStatus: String
    ): Response<ApiResponse<OrderDto>>

    // 4. Free-First Media Pipeline
    @Multipart
    @POST("/v1/media/upload")
    suspend fun uploadMedia(
        @Query("product_id") productId: String,
        @Query("media_type") mediaType: String,
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<MediaUploadResponse>>

    // 5. Mandi Price Discovery
    @GET("/v1/mandi/prices")
    suspend fun getMandiPrices(
        @Query("state") state: String,
        @Query("district") district: String
    ): Response<ApiResponse<List<MandiPriceDto>>>

    // 6. Central Config & Remote Flags
    @GET("/v1/config")
    suspend fun getAppConfig(): Response<ApiResponse<AppConfigResponse>>

    @GET("/v1/config")
    suspend fun getPublicConfig(): Response<ApiResponse<Map<String, Any>>>
}
