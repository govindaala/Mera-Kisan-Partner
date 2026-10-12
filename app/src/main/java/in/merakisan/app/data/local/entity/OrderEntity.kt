// app/src/main/java/in/merakisan/app/data/local/entity/OrderEntity.kt
package `in`.merakisan.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import `in`.merakisan.app.core.network.model.OrderDto

/**
 * MERA KISAN — स्थानीय रूम डेटाबेस ऑर्डर एंटिटी
 * ऑफ़लाइन कैशिंग और वित्तीय अखंडता (Paise minor units) समर्थित
 */
@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey
    val orderId: String,
    val productId: String = "",
    val productName: String = "",
    val sellerUid: String = "",
    val sellerName: String = "",
    val buyerUid: String = "",
    val buyerName: String = "",
    val buyerPhone: String = "",
    val quantity: Double = 0.0,
    val unit: String = "kg",
    val pricePaise: Long = 0L,
    val totalAmountPaise: Long = 0L,
    val orderStatus: String = "CREATED",
    val paymentMethod: String = "cod",
    val deliveryAddress: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = true
) {
    fun toDto(): OrderDto {
        return OrderDto(
            orderId = orderId,
            productId = productId,
            productName = productName,
            sellerUid = sellerUid,
            buyerUid = buyerUid,
            quantity = quantity,
            unit = unit,
            totalAmountPaise = totalAmountPaise,
            orderStatus = orderStatus,
            deliveryAddress = deliveryAddress,
            createdAt = createdAt.toString()
        )
    }

    companion object {
        fun fromDto(dto: OrderDto, isSynced: Boolean = true): OrderEntity {
            return OrderEntity(
                orderId = dto.orderId,
                productId = dto.productId,
                productName = dto.productName,
                sellerUid = dto.sellerUid,
                buyerUid = dto.buyerUid,
                quantity = dto.quantity,
                unit = dto.unit,
                totalAmountPaise = dto.totalAmountPaise,
                orderStatus = dto.orderStatus,
                deliveryAddress = dto.deliveryAddress,
                createdAt = dto.createdAt.toLongOrNull() ?: System.currentTimeMillis(),
                isSynced = isSynced
            )
        }
    }
}
