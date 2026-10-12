// app/src/main/java/in/merakisan/app/core/network/ApiService.kt
package `in`.merakisan.app.core.network

import `in`.merakisan.app.core.network.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

data class OrderDtoUpdate(
    val status: String = "",
    val notes: String = ""
)

data class UpdateOrderStatusRequest(
    val nextStatus: String = "",
    val status: String = nextStatus,
    val notes: String = ""
)

data class CreateOrderRequest(
    val productId: String = "",
    val sellerUid: String = "",
    val buyerUid: String = "",
    val quantity: Double = 0.0,
    val unit: String = "kg",
    val totalAmountPaise: Long = 0L,
    val paymentMethod: String = "cod",
    val deliveryAddress: String = "",
    val buyerPhone: String = ""
)

data class UserDto(
    val uid: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val role: String = "both"
)

interface ApiService {
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

    @GET("v1/products/{id}")
    suspend fun getProductDetails(@Path("id") productId: String): Response<ApiResponse<ProductDto>>

    @POST("v1/products")
    suspend fun createProduct(@Body product: ProductDto): Response<ApiResponse<ProductDto>>

    // MediaUploadWorker:51 के String पैरामीटर से 100% मेल
    @Multipart
    @POST("v1/media/upload")
    suspend fun uploadMedia(
        @Part("product_id") productId: String,
        @Part("media_type") mediaType: String,
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<MediaUploadDto>>

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

    @PATCH("v1/orders/{id}/status")
    suspend fun updateOrderStatus(
        @Path("id") orderId: String,
        @Body request: OrderDtoUpdate
    ): Response<ApiResponse<OrderDto>>

    @GET("v1/auth/me")
    suspend fun getCurrentUser(): Response<ApiResponse<UserDto>>

    @GET("v1/mandi/prices")
    suspend fun getMandiPrices(@Query("district") district: String? = null): Response<ApiResponse<List<MandiPriceDto>>>

    @GET("v1/requests")
    suspend fun getBuyerRequests(): Response<ApiResponse<List<BuyerRequestDto>>>

    @POST("v1/requests")
    suspend fun createBuyerRequest(@Body request: BuyerRequestDto): Response<ApiResponse<BuyerRequestDto>>
}
