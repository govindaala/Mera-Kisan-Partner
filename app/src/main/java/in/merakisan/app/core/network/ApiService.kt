// app/src/main/java/in/merakisan/app/core/network/ApiService.kt
package `in`.merakisan.app.core.network

import `in`.merakisan.app.core.network.model.ApiResponse
import `in`.merakisan.app.core.network.model.BuyerRequestDto
import `in`.merakisan.app.core.network.model.CreateBuyerRequestPayload
import `in`.merakisan.app.core.network.model.CreateOrderRequest
import `in`.merakisan.app.core.network.model.CreateReviewRequest
import `in`.merakisan.app.core.network.model.MandiPriceDto
import `in`.merakisan.app.core.network.model.OrderDto
import `in`.merakisan.app.core.network.model.ProductDto
import `in`.merakisan.app.core.network.model.ReviewDto
import `in`.merakisan.app.core.network.model.UpdateOrderStatusRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * MERA KISAN — एकीकृत REST API सर्विस
 * Vercel बैकएंड और Master Spec v1.1 के सभी एंडपॉइंट्स का केंद्रीय अनुबंध
 */
interface ApiService {

    // 1. प्रोडक्ट्स एवं होम डैशबोर्ड (Safe ApiResponse Wrapper)
    @GET("api/crops")
    suspend fun getProducts(
        @Query("category") category: String? = null,
        @Query("district") district: String? = null,
        @Query("search") search: String? = null
    ): Response<ApiResponse<List<ProductDto>>>

    @GET("api/crops/{id}")
    suspend fun getProductDetail(
        @Path("id") productId: String
    ): Response<ApiResponse<ProductDto>>

    @POST("api/add-crop")
    suspend fun createProduct(
        @Body product: ProductDto
    ): Response<ApiResponse<ProductDto>>

    // 2. ऑर्डर्स स्टेट मशीन एवं प्रबंधन
    @GET("api/orders")
    suspend fun getOrders(): Response<ApiResponse<List<OrderDto>>>

    @GET("api/orders/{id}")
    suspend fun getOrderDetail(
        @Path("id") orderId: String
    ): Response<ApiResponse<OrderDto>>

    @POST("api/orders")
    suspend fun createOrder(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: CreateOrderRequest
    ): Response<ApiResponse<OrderDto>>

    @POST("api/orders/{id}/status")
    suspend fun updateOrderStatus(
        @Path("id") orderId: String,
        @Body request: UpdateOrderStatusRequest
    ): Response<ApiResponse<Boolean>>

    // 3. ग्राहक मांग बोर्ड ("मुझे यह चाहिए")
    @GET("api/buyer-requests")
    suspend fun getBuyerRequests(
        @Query("category") category: String? = null
    ): Response<ApiResponse<List<BuyerRequestDto>>>

    @POST("api/buyer-requests")
    suspend fun createBuyerRequest(
        @Body request: BuyerRequestDto
    ): Response<ApiResponse<BuyerRequestDto>>

    // 4. समीक्षा एवं 5-स्टार रेटिंग्स (Verified Delivery Reviews)
    @GET("api/reviews")
    suspend fun getProductReviews(
        @Query("product_id") productId: String
    ): Response<ApiResponse<List<ReviewDto>>>

    @POST("api/reviews")
    suspend fun createReview(
        @Body request: CreateReviewRequest
    ): Response<ApiResponse<Boolean>>

    // 5. मंडी भाव ट्रैकर
    @GET("api/mandi-prices")
    suspend fun getMandiPrices(
        @Query("district") district: String? = null
    ): Response<ApiResponse<List<MandiPriceDto>>>
}
