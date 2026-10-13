// app/src/main/java/in/merakisan/app/core/network/model/UpdateOrderStatusRequest.kt
package `in`.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

data class UpdateOrderStatusRequest(

    @SerializedName(
        value = "new_status",
        alternate = [
            "next_status",
            "nextStatus",
            "status"
        ]
    )
    val nextStatus: String = "",

    @Transient
    val status: String = "",

    @SerializedName(
        value = "reason",
        alternate = [
            "notes",
            "note"
        ]
    )
    val notes: String = ""
)
