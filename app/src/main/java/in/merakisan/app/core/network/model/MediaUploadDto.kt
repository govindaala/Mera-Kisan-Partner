// app/src/main/java/in/merakisan/app/core/network/model/MediaUploadDto.kt
package in.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

/**
 * MERA KISAN Media Queue Data Contracts
 * वीडियो और फोटो अपलोड कतार स्थिति प्रबंधन
 */
data class MediaJobDto(
    @SerializedName("job_id")
    val jobId: String,

    @SerializedName("product_id")
    val productId: String,

    @SerializedName("farmer_uid")
    val farmerUid: String,

    @SerializedName("media_type")
    val mediaType: String, // "PHOTO" or "VIDEO"

    @SerializedName("local_file_path")
    val localFilePath: String,

    @SerializedName("status")
    val status: String, // PENDING, COMPRESSING, UPLOADING, YOUTUBE, QUEUED_OVER_QUOTA, FAILED

    @SerializedName("remote_url")
    val remoteUrl: String? = null,

    @SerializedName("youtube_video_id")
    val youtubeVideoId: String? = null,

    @SerializedName("retry_count")
    val retryCount: Int = 0,

    @SerializedName("created_at")
    val createdAt: Long = System.currentTimeMillis()
)

data class MediaUploadResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("video_id")
    val videoId: String?,

    @SerializedName("storage_type")
    val storageType: String?, // "YOUTUBE" or "TEMPORARY_STORAGE"

    @SerializedName("message")
    val message: String?
)
