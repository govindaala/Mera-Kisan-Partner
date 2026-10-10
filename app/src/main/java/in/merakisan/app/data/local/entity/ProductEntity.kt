// app/src/main/java/in/merakisan/app/data/local/entity/ProductEntity.kt
package `in`.merakisan.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import `in`.merakisan.app.core.network.model.ProductDto

/**
 * MERA KISAN Offline-First Cache Entity
 * Room Database के लिए पूर्णतः टाइप-सुरक्षित मॉडल
 */
@Entity(tableName = "products")
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
    fun toDto(): ProductDto = ProductDto(
        productId = productId,
        sellerUid = sellerUid,
        sellerName = sellerName,
        sellerPhone = sellerPhone,
        name = name,
        category = category,
        variety = variety,
        description = description,
        pricePaise = pricePaise,
        unit = unit,
        stockQuantity = stockQuantity,
        minOrderQuantity = minOrderQuantity,
        village = village,
        district = district,
        state = state,
        approxLat = approxLat,
        approxLng = approxLng,
        latitudeApprox = approxLat,
        longitudeApprox = approxLng,
        photos = photos,
        imageUrls = photos,
        videoUrl = videoUrl,
        harvestDate = harvestDate,
        farmingType = farmingType,
        verificationStatus = verificationStatus,
        status = status,
        isFeatured = isFeatured,
        boosted = boosted,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
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
}

fun ProductDto.toEntity(): ProductEntity = ProductEntity.fromDto(this)
