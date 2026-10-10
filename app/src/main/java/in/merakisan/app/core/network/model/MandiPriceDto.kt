// app/src/main/java/in/merakisan/app/core/network/model/MandiPriceDto.kt
package in.merakisan.app.core.network.model

import com.google.gson.annotations.SerializedName

/**
 * MERA KISAN Mandi Price Data Contracts
 * सरकारी Agmarknet / Data.gov.in ओपन डेटा हेतु मानक डेटा मॉडल
 */
data class MandiPriceDto(
    @SerializedName("commodity")
    val commodity: String,

    @SerializedName("variety")
    val variety: String?,

    @SerializedName("market")
    val market: String, // मंडी का नाम (उदा. गरोठ, मंदसौर, नीमच)

    @SerializedName("district")
    val district: String,

    @SerializedName("state")
    val state: String,

    @SerializedName("arrival_date")
    val arrivalDate: String,

    // वित्तीय सुरक्षा: गणना हेतु पैसे या प्रति क्विंटल रुपये
    @SerializedName("min_price_paise")
    val minPricePaise: Long,

    @SerializedName("max_price_paise")
    val maxPricePaise: Long,

    @SerializedName("modal_price_paise")
    val modalPricePaise: Long,

    @SerializedName("cached_timestamp")
    val cachedTimestamp: Long = System.currentTimeMillis()
)

data class MandiPriceResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: List<MandiPriceDto>?,

    @SerializedName("source")
    val source: String?, // "CACHE" or "GOV_API"

    @SerializedName("cache_ttl_hours")
    val cacheTtlHours: Int = 6,

    @SerializedName("message")
    val message: String?
)
