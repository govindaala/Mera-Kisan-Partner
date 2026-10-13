// app/src/main/java/in/merakisan/app/core/network/model/UpdateOrderStatusRequest.kt
package `in`.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

data class UpdateOrderStatusRequest(
    @SerializedName("next_status")
    val nextStatus: String,

    @SerializedName("reason")
    val reason: String? = null
)
