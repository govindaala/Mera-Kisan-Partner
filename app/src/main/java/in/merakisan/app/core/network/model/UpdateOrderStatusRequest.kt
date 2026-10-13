// app/src/main/java/in/merakisan/app/core/network/model/UpdateOrderStatusRequest.kt
package `in`.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

data class UpdateOrderStatusRequest(
    @SerializedName("nextStatus", alternate = ["next_status"])
    val nextStatus: String = "",

    @SerializedName("status")
    val status: String = "",

    @SerializedName("notes", alternate = ["note", "reason"])
    val notes: String = ""
)
