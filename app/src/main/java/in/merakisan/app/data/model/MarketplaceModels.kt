// app/src/main/java/in/merakisan/app/data/model/MarketplaceModels.kt
package `in`.merakisan.app.data.model

import `in`.merakisan.app.core.utils.SafeDateParser
import org.json.JSONObject

/**
 * Typed Domain Models (merged.txt Sec 26 & 74–78)
 * Uses integer paise for financial accuracy and privacy-safe approximate location.
 */
data class ProductModel(
    val id: String,
    val name: String,
    val category: String,
    val variety: String,
    val description: String,
    val pricePaise: Long,
    val unit: String,
    val stockQuantity: Double,
    val minOrderQuantity: Double,
    val harvestDate: String,
    val verificationStatus: String,
    val village: String,
    val district: String,
    val approxDistanceKm: Int,
    val farmerUid: String,
    val farmerName: String,
    val farmerPhone: String,
    val farmerRating: Double,
    val deliveryOptions: String,
    val status: String,
    val boosted: Boolean,
    val videoStatus: String,
    val viewsCount: Int,
    val sharesCount: Int,
    val createdAtDisplay: String
) {
    val priceRupees: Double
        get() = pricePaise / 100.0

    val isSmallQuantityAvailable: Boolean
        get() = minOrderQuantity <= 25.0

    val permanentShareUrl: String
        get() = "https://merakisan.in/product/$id"

    companion object {
        fun fromJson(obj: JSONObject): ProductModel {
            val rawPaise = obj.optLong("price_paise", obj.optLong("pricePaise", 0L))
            val rawRupees = obj.optDouble("price", 38.0)
            val resolvedPaise = if (rawPaise > 0L) rawPaise else (rawRupees * 100).toLong()

            return ProductModel(
                id = obj.optString("id", "mk_prod_${System.currentTimeMillis()}"),
                name = obj.optString("name", obj.optString("cropName", "देशी कृषि उत्पाद")),
                category = obj.optString("category", "देशी अनाज"),
                variety = obj.optString("variety", "पारंपरिक देशी"),
                description = obj.optString("description", "खेत से सीधा ताज़ा और साफ उत्पाद।"),
                pricePaise = resolvedPaise,
                unit = obj.optString("unit", "kg"),
                stockQuantity = obj.optDouble("stock_quantity", obj.optDouble("stockKg", 50.0)),
                minOrderQuantity = obj.optDouble("minimum_order_quantity", obj.optDouble("minimumOrderKg", 1.0)),
                harvestDate = SafeDateParser.toDisplayDate(obj.opt("harvest_date") ?: obj.opt("harvestDate")),
                verificationStatus = obj.optString("verification_status", "✔ किसान द्वारा घोषित (Farmer Declared)"),
                village = obj.optString("village", "गरोठ"),
                district = obj.optString("district", "मंदसौर"),
                approxDistanceKm = obj.optInt("approx_distance_km", 7),
                farmerUid = obj.optString("farmer_uid", obj.optString("sellerId", "farmer_verified")),
                farmerName = obj.optString("seller_name", obj.optString("farmerName", "रामलाल पाटीदार")),
                farmerPhone = obj.optString("farmer_phone", "9876543210"),
                farmerRating = obj.optDouble("farmer_rating", 4.8),
                deliveryOptions = obj.optString("delivery_options", "खेत से पिकअप व लोकल डिलीवरी"),
                status = obj.optString("status", "ACTIVE").uppercase(),
                boosted = obj.optBoolean("boosted", false),
                videoStatus = obj.optString("video_status", "YOUTUBE"),
                viewsCount = obj.optInt("views_count", 124),
                sharesCount = obj.optInt("shares_count", 18),
                createdAtDisplay = SafeDateParser.toDisplayDate(obj.opt("created_at") ?: obj.opt("createdAt"))
            )
        }
    }
}

data class OfferModel(
    val id: String,
    val productId: String,
    val productName: String,
    val buyerId: String,
    val buyerName: String,
    val sellerId: String,
    val sellerName: String,
    val originalPricePaise: Long,
    var offeredPricePaise: Long,
    val quantity: Double,
    val unit: String,
    val message: String,
    var status: String,
    val createdAtDisplay: String
) {
    val offeredPriceRupees: Int
        get() = (offeredPricePaise / 100).toInt()

    val originalPriceRupees: Int
        get() = (originalPricePaise / 100).toInt()
}
