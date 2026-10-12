// app/src/main/java/in/merakisan/app/data/repository/OrderRepository.kt
package `in`.merakisan.app.data.repository

import android.content.Context
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.network.CreateOrderRequest
import `in`.merakisan.app.core.network.UpdateOrderStatusRequest
import `in`.merakisan.app.core.network.model.OrderDto
import `in`.merakisan.app.core.security.SessionManager
import `in`.merakisan.app.data.local.AppDatabase
import `in`.merakisan.app.data.local.entity.OrderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * MERA KISAN — सेंट्रल ऑर्डर रिपॉजिटरी
 * नेटवर्क + लोकल रूम डेटाबेस को सिंक्रनाइज़ करता है (Offline-First Architecture)
 */
class OrderRepository(private val context: Context) {

    private val apiService = ApiClient.getApiService(context)
    private val orderDao = AppDatabase.getInstance(context).orderDao()
    private val sessionManager = SessionManager(context)

    fun getLocalOrders(role: String): Flow<List<OrderDto>> {
        val userSession = sessionManager.getUserSession()
        val flow = if (role.equals("farmer", ignoreCase = true)) {
            orderDao.getOrdersBySeller(userSession.uid)
        } else {
            orderDao.getOrdersByBuyer(userSession.uid)
        }
        return flow.map { entities -> entities.map { it.toDto() } }
    }

    suspend fun refreshOrders(): Result<List<OrderDto>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getOrders()
            if (response.isSuccessful && response.body()?.success == true) {
                val dtoList = response.body()?.data ?: emptyList()
                val entities = dtoList.map { OrderEntity.fromDto(it, isSynced = true) }
                orderDao.insertOrders(entities)
                Result.success(dtoList)
            } else {
                Result.failure(Exception("सर्वर से ऑर्डर लोड नहीं हो सके"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createOrder(
        productId: String,
        sellerUid: String,
        quantity: Double,
        unit: String,
        totalAmountPaise: Long,
        paymentMethod: String = "cod",
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
                buyerPhone = buyerPhone.ifBlank { user.phone }
            )

            val response = apiService.createOrder(idempotencyKey, request)
            if (response.isSuccessful && response.body()?.success == true) {
                val createdDto = response.body()?.data
                if (createdDto != null) {
                    orderDao.insertOrder(OrderEntity.fromDto(createdDto, isSynced = true))
                    Result.success(createdDto)
                } else {
                    Result.failure(Exception("ऑर्डर डेटा रिक्त प्राप्त हुआ"))
                }
            } else {
                Result.failure(Exception("ऑर्डर निर्माण विफल: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(orderId: String, nextStatus: String, notes: String = ""): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val req = UpdateOrderStatusRequest(nextStatus = nextStatus, status = nextStatus, notes = notes)
            val response = apiService.updateOrderStatus(orderId, req)
            if (response.isSuccessful) {
                orderDao.updateOrderStatus(orderId, nextStatus)
                Result.success(true)
            } else {
                Result.failure(Exception("स्थिति अपडेट विफल"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
