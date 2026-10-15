package in.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

data class ProductDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("productId") val productId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("variety") val variety: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("quantity") val quantity: Double = 0.0,
    @SerializedName("unit") val unit: String? = null,
    @SerializedName("price") val price: Double = 0.0,
    @SerializedName("minOrder") val minOrder: Double = 1.0,
    @SerializedName("farmerId") val farmerId: String? = null,
    @SerializedName("farmerName") val farmerName: String? = null,
    @SerializedName("village") val village: String? = null,
    @SerializedName("district") val district: String? = null,
    @SerializedName("harvestDate") val harvestDate: String? = null,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("verified") val verified: Boolean = false,
    @SerializedName("status") val status: String? = null
) {
    fun sanitize(): ProductDto {
        return this.copy(
            id = id ?: productId ?: "",
            name = name ?: "Untitled Product",
            category = category ?: "General",
            variety = variety ?: "",
            description = description ?: "",
            unit = unit ?: "kg",
            farmerId = farmerId ?: "",
            farmerName = farmerName ?: "Farmer",
            village = village ?: "",
            district = district ?: "",
            imageUrl = imageUrl ?: "",
            status = status ?: "active"
        )
    }
}
