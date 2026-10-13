// app/src/main/java/in/merakisan/app/core/network/model/UpdateOrderStatusRequest.kt
package `in`.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

/**
 * MERA KISAN — ऑर्डर स्थिति अद्यतन अनुरोध पेलोड
 * OrderRepository, Dispute, OrderDetail और OrdersViewModel के साथ 100% संरेखित
 */
data class UpdateOrderStatusRequest(
    @SerializedName("nextStatus", alternate = ["next_status"])
    val nextStatus: String = "",

    @SerializedName("status")
    val status: String = "",

    @SerializedName("notes", alternate = ["note", "reason"])
    val notes: String = ""
)
