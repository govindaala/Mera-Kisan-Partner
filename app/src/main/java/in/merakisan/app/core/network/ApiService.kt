package `in`.merakisan.app.core.network

import com.google.gson.annotations.SerializedName
import `in`.merakisan.app.core.network.model.ApiResponse
import `in`.merakisan.app.core.network.model.AppConfigDto
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
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

// पुराने कॉलर्स के लिए बैकवर्ड-कम्पैटिबल टाइप-एलियास
typealias CreateOrderRequest = ModelCreateOrderRequest
typealias UpdateOrderStatusRequest = ModelUpdateOrderStatusRequest

data class UserDto(
    @SerializedName("uid") val uid: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("phone") val phone: String = "",
    @SerializedName("email") val email: String = "",
    @SerializedName("role") val role: String = "both",
    @SerializedName("village") val village: String = "",
    @SerializedName("district") val district: String = "",
    @SerializedName("photo_url") val photoUrl: String = ""
)

/**
 * MERA KISAN — केंद्रीकृत REST API सेवा
 * Canonical Vercel Backend Contract (/v1)
 */
interface ApiService {

    // 1. उत्पाद एवं फ़सलें (/v1/products)
    @GET("v1/products")
    suspend fun getProducts(
        @Query("category") category: String? = null,
        @Query("district") district: String? = null,
        @Query("search") search: String? = null,
        @Query("maxQuantity") maxQuantity: Double? = null,
        @Query("organicOnly") organicOnly: Boolean? = null,
        @Query("limit") limit: Int? = null,
        @Query("page") page: Int? = null
    ): Response<ApiResponse<List<ProductDto>>>

    @GET("v1/products/{id}")
    suspend fun getProductDetail(
        @Path("id") productId: String
    ): Response<ApiResponse<ProductDto>>

    @GET("v1/products/{id}")
    suspend fun getProductDetails(
        @Path("id") productId: String
    ): Response<ApiResponse<ProductDto>>

    @POST("v1/products")
    suspend fun createProduct(
        @Body product: ProductDto
    ): Response<ApiResponse<ProductDto>>

    // 2. मीडिया एवं वीडियो जॉब क्यू
    @Multipart
    @POST("v1/products/media/upload")
    suspend fun uploadMedia(
        @Query("productId") productId: String,
        @Query("mediaType") mediaType: String,
        @Part body: MultipartBody.Part
    ): Response<ApiResponse<Any>>

    // 3. प्रमाणीकरण एवं प्रोफ़ाइल (/v1/auth)
    @GET("v1/auth/me")
    suspend fun getCurrentUser(): Response<ApiResponse<UserDto>>

    // 4. ऑर्डर्स स्टेट मशीन (/v1/orders)
    @GET("v1/orders")
    suspend fun getOrders(): Response<ApiResponse<List<OrderDto>>>

    @GET("v1/orders/{id}")
    suspend fun getOrderDetail(
        @Path("id") orderId: String
    ): Response<ApiResponse<OrderDto>>

    @POST("v1/orders")
    suspend fun createOrder(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: ModelCreateOrderRequest
    ): Response<ApiResponse<OrderDto>>

    @PATCH("v1/orders/{id}/status")
    suspend fun updateOrderStatus(
        @Path("id") orderId: String,
        @Body request: ModelUpdateOrderStatusRequest
    ): Response<ApiResponse<Boolean>>

    // 5. खरीदार मांग बोर्ड (/v1/buyer-requests)
    @GET("v1/buyer-requests")
    suspend fun getBuyerRequests(
        @Query("category") category: String? = null
    ): Response<ApiResponse<List<BuyerRequestDto>>>

    @POST("v1/buyer-requests")
    suspend fun createBuyerRequest(
        @Body request: BuyerRequestDto
    ): Response<ApiResponse<BuyerRequestDto>>

    // 6. सत्यापित समीक्षाएं (/v1/products/{id}/reviews)
    @GET("v1/products/{id}/reviews")
    suspend fun getProductReviews(
        @Path("id") productId: String
    ): Response<ApiResponse<List<ReviewDto>>>

    @POST("v1/products/{id}/reviews")
    suspend fun createReview(
        @Path("id") productId: String,
        @Body request: CreateReviewRequest
    ): Response<ApiResponse<Boolean>>

    // 7. रिमोट फ़ीचर कॉन्फ़िगरेशन
    @GET("v1/config")
    suspend fun getRemoteConfig(): Response<ApiResponse<AppConfigDto>>

    // 8. मंडी भाव ट्रैकर
    @GET("v1/mandi-prices")
    suspend fun getMandiPrices(
        @Query("district") district: String? = null
    ): Response<ApiResponse<List<MandiPriceDto>>>
}
