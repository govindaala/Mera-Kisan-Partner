// app/src/main/java/in/merakisan/app/core/network/model/CreateBuyerRequestPayload.kt
package `in`.merakisan.app.core.network.model

data class CreateBuyerRequestPayload(
    val productName: String,
    val category: String,
    val variety: String? = null,
    val quantity: Double,
    val unit: String = "kg",
    val targetPricePaise: Long? = null,
    val district: String = "",
    val notes: String? = null
)
