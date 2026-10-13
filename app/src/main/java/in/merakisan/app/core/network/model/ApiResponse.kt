package `in`.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

data class ApiError(
    @SerializedName("code")
    val code: String = "UNKNOWN_ERROR",

    @SerializedName("message")
    val message: String = "अज्ञात त्रुटि हुई।"
)

data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("data")
    val data: T? = null,

    @SerializedName("error")
    val error: ApiError? = null,

    @SerializedName("timestamp")
    val timestamp: Long? = null,

    @SerializedName("requestId")
    val requestId: String? = null
)
