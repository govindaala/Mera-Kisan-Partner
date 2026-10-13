// app/src/main/java/in/merakisan/app/core/network/model/AppConfigDto.kt
package `in`.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

data class AppConfigDto(
    @SerializedName("features")
    val features: Map<String, Any> = emptyMap(),

    @SerializedName("maintenance_mode", alternate = ["maintenance"])
    val maintenanceMode: Boolean = false,

    @SerializedName("min_app_version", alternate = ["minAppVersion"])
    val minAppVersion: String = "1.0.0",

    @SerializedName("latest_app_version", alternate = ["latestAppVersion"])
    val latestAppVersion: String = "1.1.0",

    @SerializedName("banner_text", alternate = ["bannerText"])
    val bannerText: String? = null
)
