package in.merakisan.app.core.config

object FeatureManager {
    private val flags = mutableMapOf<String, Boolean>(
        "payment" to false,
        "crop_boost" to true,
        "buyer_request" to false,
        "chat" to false,
        "auction" to false
    )

    fun updateFlags(newFlags: Map<String, Boolean>) {
        flags.putAll(newFlags)
    }

    fun isEnabled(featureKey: String): Boolean {
        return flags[featureKey] ?: false
    }

    fun getPaymentEnabled(): Boolean = isEnabled("payment")
    fun getCropBoostEnabled(): Boolean = isEnabled("crop_boost")
    fun getBuyerRequestEnabled(): Boolean = isEnabled("buyer_request")
}
