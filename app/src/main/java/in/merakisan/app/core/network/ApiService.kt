// app/src/main/java/in/merakisan/app/core/network/ApiService.kt
package `in`.merakisan.app.core.network

import com.google.gson.annotations.SerializedName
import `in`.merakisan.app.core.network.model.ApiResponse
import `in`.merakisan.app.core.network.model.BuyerRequestDto
import `in`.merakisan.app.core.network.model.CreateOrderRequest as ModelCreateOrderRequest
import `in`.merakisan.app.core.network.model.CreateReviewRequest
import `in`.merakisan.app.core.network.model.MandiPriceDto
import `in`.merakisan.app.core.network.model.OrderDto
import `in`.merakisan.app.core.network.model.ProductDto
import `in`.merakisan.app.core.network.model.ReviewDto
import `in`.merakisan.app.core.network.model.UpdateOrderStatusRequest as ModelUpdateOrderStatusRequest
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

// पुराने कॉलर्स के लिए टाइप-एलियास
typealias CreateOrderRequest = ModelCreateOrderRequest
typealias UpdateOrderStatusRequest = ModelUpdateOrderStatusRequest

data class UserDto(
    @SerializedName("uid") val uid: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("phone") val phone: String = "",
    @SerializedName("email") val email: String = "",
    @SerializedName("role") val role: String = "both"
)

interface ApiService {

    @GET("api/crops")
    suspend fun getProducts(
        @Query("category") category: String? = null,
        @Query("district") district: String? = null,
        @Query("search") search: String? = null,
        @Query("maxQuantity") maxQuantity: Double? = null,
        @Query("organicOnly") organicOnly: Boolean? = null,
        @Query("limit") limit: Int? = null,
        @Query("page") page: Int? = null
    ): Response<ApiResponse<List<ProductDto>>>

    @GET("api/crops/{id}")
    suspend fun getProductDetail(
        @Path("id") productId: String
    ): Response<ApiResponse<ProductDto>>

    @GET("api/crops/{id}")
    suspend fun getProductDetails(
        @Path("id") productId: String
    ): Response<ApiResponse<ProductDto>>

    @POST("api/add-crop")
    suspend fun createProduct(
        @Body product: ProductDto
    ): Response<ApiResponse<ProductDto>>

    @Multipart
    @POST("api/media/upload")
    suspend fun uploadMedia(
        @Query("productId") productId: String,
        @Query("mediaType") mediaType: String,
        @Part body: MultipartBody.Part
    ): Response<ApiResponse<Any>>

    @GET("api/me")
    suspend fun getCurrentUser(): Response<ApiResponse<UserDto>>

    @GET("api/orders")
    suspend fun getOrders(): Response<ApiResponse<List<OrderDto>>>

    @GET("api/orders/{id}")
    suspend fun getOrderDetail(
        @Path("id") orderId: String
    ): Response<ApiResponse<OrderDto>>

    @POST("api/orders")
    suspend fun createOrder(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: ModelCreateOrderRequest
    ): Response<ApiResponse<OrderDto>>

    @POST("api/orders/{id}/status")
    suspend fun updateOrderStatus(
        @Path("id") orderId: String,
        @Body request: ModelUpdateOrderStatusRequest
    ): Response<ApiResponse<Boolean>>

    @GET("api/buyer-requests")
    suspend fun getBuyerRequests(
        @Query("category") category: String? = null
    ): Response<ApiResponse<List<BuyerRequestDto>>>

    @POST("api/buyer-requests")
    suspend fun createBuyerRequest(
        @Body request: BuyerRequestDto
    ): Response<ApiResponse<BuyerRequestDto>>

    @GET("api/reviews")
    suspend fun getProductReviews(
        @Query("product_id") productId: String
    ): Response<ApiResponse<List<ReviewDto>>>

    @POST("api/reviews")
    suspend fun createReview(
        @Body request: CreateReviewRequest
    ): Response<ApiResponse<Boolean>>

    @GET("api/mandi-prices")
    suspend fun getMandiPrices(
        @Query("district") district: String? = null
    ): Response<ApiResponse<List<MandiPriceDto>>>
}
