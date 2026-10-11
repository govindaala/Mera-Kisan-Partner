// app/src/main/java/in/merakisan/app/core/network/ApiService.kt
package `in`.merakisan.app.core.network

import `in`.merakisan.app.core.network.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // 1. रिमोट कॉन्फ़िग व इन-ऐप ऑटो-अपडेट
    @GET("v1/config")
    suspend fun getPublicConfig(): Response<ApiResponse<ConfigDto>>

    @GET("v1/app/check-update")
    suspend fun checkAppUpdate(@Query("version_code") versionCode: Int): Response<ApiResponse<AppUpdateDto>>

    // 2. ऑथेंटिकेशन
    @POST("v1/auth/verify-google")
    suspend fun verifyGoogleToken(@Body request: VerifyGoogleRequest): Response<ApiResponse<AuthResponseDto>>

    @GET("v1/auth/me")
    suspend fun getCurrentUser(): Response<ApiResponse<UserDto>>

    // 3. फ़सलें (Marketplace)
    @GET("v1/products")
    suspend fun getProducts(
        @Query("category") category: String? = null,
        @Query("max_quantity") maxQuantity: Double? = null,
        @Query("organic_only") organicOnly: Boolean? = null,
        @Query("limit") limit: Int = 50,
        @Query("page") page: Int = 1
    ): Response<ApiResponse<List<ProductDto>>>

    @GET("v1/products/{id}")
    suspend fun getProductDetail(@Path("id") productId: String): Response<ApiResponse<ProductDto>>

    @POST("v1/products")
    suspend fun createProduct(@Body product: ProductDto): Response<ApiResponse<ProductDto>>

    // 4. ऑर्डर्स
    @POST("v1/orders")
    suspend fun createOrder(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: CreateOrderRequest
    ): Response<ApiResponse<OrderDto>>

    @GET("v1/orders")
    suspend fun getOrders(): Response<ApiResponse<List<OrderDto>>>

    @GET("v1/orders/{id}")
    suspend fun getOrderDetail(@Path("id") orderId: String): Response<ApiResponse<OrderDto>>

    @PATCH("v1/orders/{id}/status")
    suspend fun updateOrderStatus(
        @Path("id") orderId: String,
        @Body request: UpdateOrderStatusRequest
    ): Response<ApiResponse<OrderDto>>

    // 5. मंडी भाव
    @GET("v1/mandi/prices")
    suspend fun getMandiPrices(@Query("district") district: String? = null): Response<ApiResponse<List<MandiPriceDto>>>

    // 6. खरीदार मांग बोर्ड ("मुझे चाहिए")
    @GET("v1/requests")
    suspend fun getBuyerRequests(): Response<ApiResponse<List<BuyerRequestDto>>>

    @POST("v1/requests")
    suspend fun createBuyerRequest(@Body request: BuyerRequestDto): Response<ApiResponse<BuyerRequestDto>>
}
