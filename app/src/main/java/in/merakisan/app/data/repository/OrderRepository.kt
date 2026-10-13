package `in`.merakisan.app.data.repository

import android.content.Context
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.network.CreateOrderRequest
import `in`.merakisan.app.core.network.UpdateOrderStatusRequest
import `in`.merakisan.app.core.network.model.OrderDto
import `in`.merakisan.app.core.security.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class OrderRepository(private val context: Context) {

    private val apiService = ApiClient.getApiService(context)
    private val sessionManager = SessionManager(context)

    suspend fun createOrder(
        productId: String,
        sellerUid: String,
        quantity: Double,
        unit: String,
        totalAmountPaise: Long,
        paymentMethod: String,
        deliveryAddress: String,
        buyerPhone: String
    ): Result<OrderDto> = withContext(Dispatchers.IO) {
        try {
            val user = sessionManager.getUserSession()
            val idempotencyKey = UUID.randomUUID().toString()

            val request = CreateOrderRequest(
                productId = productId,
                sellerUid = sellerUid,
                buyerUid = user.uid,
                quantity = quantity,
                unit = unit,
                totalAmountPaise = totalAmountPaise,
                paymentMethod = paymentMethod,
                deliveryAddress = deliveryAddress,
                buyerPhone = buyerPhone.ifBlank { user.phone },
                idempotencyKey = idempotencyKey
            )

            val response = apiService.createOrder(idempotencyKey, request)

            if (response.isSuccessful && response.body()?.success == true) {
                val order = response.body()?.data
                if (order != null) {
                    Result.success(order)
                } else {
                    Result.failure(Exception("ऑर्डर डेटा रिक्त प्राप्त हुआ।"))
                }
            } else {
                val errorMsg = response.body()?.error?.message ?: response.message()
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(
        orderId: String,
        nextStatus: String,
        notes: String = ""
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val req = UpdateOrderStatusRequest(
                nextStatus = nextStatus,
                status = nextStatus,
                notes = notes
            )
            val response = apiService.updateOrderStatus(orderId, req)

            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(true)
            } else {
                val errorMsg = response.body()?.error?.message ?: response.message()
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrders(): Result<List<OrderDto>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getOrders()
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception(response.body()?.error?.message ?: "ऑर्डर्स लोड नहीं हो सके"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
