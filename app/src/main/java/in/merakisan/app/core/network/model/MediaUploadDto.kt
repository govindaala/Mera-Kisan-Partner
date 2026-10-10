// app/src/main/java/in/merakisan/app/core/network/model/MediaUploadDto.kt
package in.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

/**
 * MERA KISAN Free-First Media Upload Model
 * Photos के लिए Google Drive और Videos के लिए YouTube Storage Architecture
 */
data class MediaUploadDto(
    @SerializedName("media_id")
    val mediaId: String = "",

    @SerializedName("product_id")
    val productId: String = "",

    @SerializedName("media_type")
    val mediaType: String = "photo", // photo, video

    @SerializedName("storage_type")
    val storageType: String = "google_drive", // google_drive, youtube, firebase_fallback

    @SerializedName("original_url")
    val originalUrl: String = "",

    @SerializedName("thumbnail_url")
    val thumbnailUrl: String? = null,

    @SerializedName("external_file_id")
    val externalFileId: String? = null,

    @SerializedName("status")
    val status: String = "completed"
)

// ApiService.kt के साथ टाइप-संगतता
typealias MediaUploadResponse = MediaUploadDto
