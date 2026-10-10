// app/src/main/java/in/merakisan/app/data/local/entity/ProductEntity.kt
package in.merakisan.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import in.merakisan.app.core.network.model.ProductDto

/**
 * MERA KISAN Offline Produce Entity
 * Local cache schema with integer paise and privacy-first coordinates
 */
@Entity(tableName = "cached_products")
data class ProductEntity(
    @PrimaryKey
    @ColumnInfo(name = "product_id")
    val productId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "variety")
    val variety: String?,

    @ColumnInfo(name = "description")
    val description: String?,

    // Vittiye suraksha: Integer paise (Zero floating-point rounding error)
    @ColumnInfo(name = "price_paise")
    val pricePaise: Long,

    @ColumnInfo(name = "stock_quantity")
    val stockQuantity: Double,

    @ColumnInfo(name = "unit")
    val unit: String,

    @ColumnInfo(name = "min_order_quantity")
    val minOrderQuantity: Double,

    @ColumnInfo(name = "seller_uid")
    val sellerUid: String,

    @ColumnInfo(name = "seller_name")
    val sellerName: String,

    @ColumnInfo(name = "seller_phone")
    val sellerPhone: String?,

    @ColumnInfo(name = "village")
    val village: String?,

    @ColumnInfo(name = "district")
    val district: String,

    @ColumnInfo(name = "state")
    val state: String?,

    // Privacy-by-Design: Anumanit nirdeshank (Exact GPS never stored)
    @ColumnInfo(name = "latitude_approx")
    val latitudeApprox: Double?,

    @ColumnInfo(name = "longitude_approx")
    val longitudeApprox: Double?,

    @ColumnInfo(name = "verification_status")
    val verificationStatus: String,

    @ColumnInfo(name = "harvest_date")
    val harvestDate: String?,

    @ColumnInfo(name = "photo_url")
    val photoUrl: String?,

    @ColumnInfo(name = "is_featured")
    val isFeatured: Boolean,

    @ColumnInfo(name = "boosted")
    val boosted: Boolean,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()
)

fun ProductEntity.toDto(): ProductDto {
    return ProductDto(
        productId = productId,
        name = name,
        category = category,
        variety = variety,
        description = description,
        pricePaise = pricePaise,
        stockQuantity = stockQuantity,
        unit = unit,
        minOrderQuantity = minOrderQuantity,
        sellerUid = sellerUid,
        sellerName = sellerName,
        sellerPhone = sellerPhone,
        village = village,
        district = district,
        state = state,
        latitudeApprox = latitudeApprox,
        longitudeApprox = longitudeApprox,
        verificationStatus = verificationStatus,
        harvestDate = harvestDate,
        photos = if (!photoUrl.isNullOrBlank()) listOf(photoUrl) else emptyList(),
        isFeatured = isFeatured,
        boosted = boosted,
        status = status,
        createdAt = null
    )
}

fun ProductDto.toEntity(): ProductEntity {
    return ProductEntity(
        productId = productId,
        name = name,
        category = category,
        variety = variety,
        description = description,
        pricePaise = pricePaise,
        stockQuantity = stockQuantity,
        unit = unit,
        minOrderQuantity = minOrderQuantity ?: 1.0,
        sellerUid = sellerUid,
        sellerName = sellerName,
        sellerPhone = sellerPhone,
        village = village,
        district = district,
        state = state,
        latitudeApprox = latitudeApprox,
        longitudeApprox = longitudeApprox,
        verificationStatus = verificationStatus,
        harvestDate = harvestDate,
        photoUrl = photos.firstOrNull(),
        isFeatured = isFeatured,
        boosted = boosted,
        status = status,
        cachedAt = System.currentTimeMillis()
    )
}
