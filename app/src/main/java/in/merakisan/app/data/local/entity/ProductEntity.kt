// app/src/main/java/in/merakisan/app/data/local/entity/ProductEntity.kt
package `in`.merakisan.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import `in`.merakisan.app.core.network.model.ProductDto

/**
 * MERA KISAN Offline Cache Entity (cached_products टेबल)
 */
@Entity(tableName = "cached_products")
data class ProductEntity(
    @PrimaryKey
    val productId: String,
    val sellerUid: String = "",
    val sellerName: String = "",
    val sellerPhone: String? = null,
    val name: String = "",
    val category: String = "",
    val variety: String = "",
    val description: String = "",
    val pricePaise: Long = 0L,
    val unit: String = "kg",
    val stockQuantity: Double = 0.0,
    val minOrderQuantity: Double = 1.0,
    val village: String = "",
    val district: String = "",
    val state: String = "Madhya Pradesh",
    val approxLat: Double? = null,
    val approxLng: Double? = null,
    val photos: List<String> = emptyList(),
    val videoUrl: String? = null,
    val harvestDate: String = "",
    val farmingType: String = "conventional",
    val verificationStatus: String = "unverified",
    val status: String = "active",
    val isFeatured: Boolean = false,
    val boosted: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String = ""
) {
    fun toDto(): ProductDto = ProductEntityConverter.toDto(this)

    companion object {
        fun fromDto(dto: ProductDto): ProductEntity = ProductEntityConverter.fromDto(dto)
    }
}

object ProductEntityConverter {
    fun toDto(entity: ProductEntity): ProductDto = ProductDto(
        productId = entity.productId,
        sellerUid = entity.sellerUid,
        sellerName = entity.sellerName,
        sellerPhone = entity.sellerPhone,
        name = entity.name,
        category = entity.category,
        variety = entity.variety,
        description = entity.description,
        pricePaise = entity.pricePaise,
        unit = entity.unit,
        stockQuantity = entity.stockQuantity,
        minOrderQuantity = entity.minOrderQuantity,
        village = entity.village,
        district = entity.district,
        state = entity.state,
        approxLat = entity.approxLat,
        approxLng = entity.approxLng,
        latitudeApprox = entity.approxLat,
        longitudeApprox = entity.approxLng,
        photos = entity.photos,
        imageUrls = entity.photos,
        videoUrl = entity.videoUrl,
        harvestDate = entity.harvestDate,
        farmingType = entity.farmingType,
        verificationStatus = entity.verificationStatus,
        status = entity.status,
        isFeatured = entity.isFeatured,
        boosted = entity.boosted,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt
    )

    fun fromDto(dto: ProductDto): ProductEntity = ProductEntity(
        productId = dto.productId,
        sellerUid = dto.sellerUid,
        sellerName = dto.sellerName,
        sellerPhone = dto.sellerPhone,
        name = dto.name,
        category = dto.category,
        variety = dto.variety ?: "",
        description = dto.description ?: "",
        pricePaise = dto.pricePaise,
        unit = dto.unit,
        stockQuantity = dto.stockQuantity,
        minOrderQuantity = dto.minOrderQuantity,
        village = dto.village ?: "",
        district = dto.district ?: "",
        state = dto.state ?: "Madhya Pradesh",
        approxLat = dto.approxLat ?: dto.latitudeApprox,
        approxLng = dto.approxLng ?: dto.longitudeApprox,
        photos = (dto.photos.takeIf { it.isNotEmpty() } ?: dto.imageUrls) ?: emptyList(),
        videoUrl = dto.videoUrl,
        harvestDate = dto.harvestDate ?: "",
        farmingType = dto.farmingType ?: "conventional",
        verificationStatus = dto.verificationStatus ?: "unverified",
        status = dto.status ?: "active",
        isFeatured = dto.isFeatured,
        boosted = dto.boosted,
        createdAt = dto.createdAt ?: "",
        updatedAt = dto.updatedAt ?: ""
    )
}

// ProductRepository.kt:8:45 इम्पोर्ट के लिए आवश्यक टॉप-लेवल एक्सटेंशन
fun ProductEntity.toDto(): ProductDto = ProductEntityConverter.toDto(this)
fun ProductDto.toEntity(): ProductEntity = ProductEntityConverter.fromDto(this)
