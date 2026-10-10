// app/src/main/java/in/merakisan/app/core/network/model/OrderDtoUpdate.kt
package in.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

/**
 * CI/CD Verified Order Data Contracts
 */
data class UpdateOrderStatusRequest(
    @SerializedName("next_status")
    val nextStatus: String,

    @SerializedName("reason")
    val reason: String? = null
)
