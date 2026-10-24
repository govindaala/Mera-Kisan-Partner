// app/src/main/java/in/merakisan/app/core/network/ApiClient.kt
package `in`.merakisan.app.core.network

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import `in`.merakisan.app.core.security.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * MERA KISAN — SINGLE POINT OF SERVER CONNECTION
 * नियम: पूरे एंड्रॉइड ऐप में सर्वर से जुड़ने के लिए केवल यही एक फाइल अधिकृत है।
 * भविष्य में सर्वर या डोमेन बदलने पर केवल नीचे दिए गए SERVER_BASE_URL को बदलें।
 */
object ApiClient {

    // भविष्य में सर्वर बदलने पर केवल यही 1 लाइन बदलनी होगी
    const val SERVER_ORIGIN: String = "https://mera-kisan-backend.vercel.app"
    const val SERVER_BASE_URL: String = "$SERVER_ORIGIN/api/v1"

    private const val CONNECT_TIMEOUT_MS = 15000
    private const val READ_TIMEOUT_MS = 20000

    data class ApiResult(
        val isSuccess: Boolean,
        val statusCode: Int,
        val dataObject: JSONObject? = null,
        val dataArray: JSONArray? = null,
        val errorCode: String? = null,
        val errorMessage: String? = null,
        val requestId: String? = null
    )

    private fun resolveFreshFirebaseIdToken(context: Context?): String? {
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                val tokenResult = Tasks.await(currentUser.getIdToken(false), 6, TimeUnit.SECONDS)
                val liveToken = tokenResult?.token
                if (!liveToken.isNullOrBlank()) {
                    if (context != null) {
                        SessionManager.saveAuthToken(context, liveToken)
                    }
                    return liveToken
                }
            }
        } catch (_: Exception) {
            // यदि ऑफ़लाइन या टाइमआउट हो तो सेशन मैनेजर से अंतिम सुरक्षित टोकन लें
        }
        return if (context != null) SessionManager.getAuthToken(context) else null
    }

    private fun buildQueryString(params: Map<String, String?>): String {
        val parts = mutableListOf<String>()
        for ((k, v) in params) {
            if (!v.isNullOrBlank()) {
                val encKey = URLEncoder.encode(k.trim(), "UTF-8")
                val encVal = URLEncoder.encode(v.trim(), "UTF-8")
                parts.add("$encKey=$encVal")
            }
        }
        return if (parts.isEmpty()) "" else "?" + parts.joinToString("&")
    }

    /**
     * केंद्रीकृत HTTP इंजन: ऐप की सभी स्क्रीन केवल इसी के माध्यम से सर्वर से बात करेंगी।
     */
    suspend fun request(
        context: Context?,
        endpointPath: String,
        method: String = "GET",
        queryParams: Map<String, String?> = emptyMap(),
        bodyJson: JSONObject? = null,
        customIdempotencyKey: String? = null,
        useDealerToken: Boolean = false
    ): ApiResult = withContext(Dispatchers.IO) {
        val cleanPath = if (endpointPath.startsWith("/")) endpointPath else "/$endpointPath"
        val fullUrl = "$SERVER_BASE_URL$cleanPath${buildQueryString(queryParams)}"
        val upperMethod = method.uppercase()

        var connection: HttpURLConnection? = null
        try {
            val url = URL(fullUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = if (upperMethod == "PATCH") "POST" else upperMethod
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("x-request-id", "and_${UUID.randomUUID().toString().replace("-", "").take(16)}")

                if (upperMethod == "PATCH") {
                    setRequestProperty("X-HTTP-Method-Override", "PATCH")
                    try {
                        this.requestMethod = "PATCH"
                    } catch (_: Exception) {
                        // पुराने Android HttpURLConnection पर X-HTTP-Method-Override काम करेगा
                    }
                }

                // 1. प्रमाणित Firebase ID Token जोड़ें
                val idToken = resolveFreshFirebaseIdToken(context)
                if (!idToken.isNullOrBlank()) {
                    setRequestProperty("Authorization", "Bearer $idToken")
                }

                // 2. यदि अधिकृत कृषि दवाई विक्रेता (Dealer) कॉल है तो Dealer Token जोड़ें
                if (useDealerToken && context != null) {
                    val dealerToken = SessionManager.getDealerToken(context)
                    if (!dealerToken.isNullOrBlank()) {
                        setRequestProperty("x-dealer-token", dealerToken)
                    }
                }

                // 3. डुप्लीकेट ऑर्डर/राइट रोकने के लिए Idempotency-Key जोड़ें
                if (upperMethod in listOf("POST", "PUT", "PATCH")) {
                    val idemKey = customIdempotencyKey ?: UUID.randomUUID().toString()
                    setRequestProperty("Idempotency-Key", idemKey)
                }
            }

            if (bodyJson != null && upperMethod in listOf("POST", "PUT", "PATCH")) {
                connection.doOutput = true
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(bodyJson.toString())
                    writer.flush()
                }
            }

            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
            val rawResponse = if (stream != null) {
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            } else {
                ""
            }

            if (rawResponse.isBlank()) {
                return@withContext ApiResult(
                    isSuccess = false,
                    statusCode = statusCode,
                    errorCode = "EMPTY_SERVER_RESPONSE",
                    errorMessage = "सर्वर से कोई उत्तर प्राप्त नहीं हुआ। कृपया इंटरनेट कनेक्शन जांचें।"
                )
            }

            val rootJson = JSONObject(rawResponse)
            val isOk = statusCode in 200..299 && rootJson.optBoolean("success", false)
            val reqId = rootJson.optString("requestId", "")

            if (isOk) {
                val rawData = rootJson.opt("data")
                return@withContext ApiResult(
                    isSuccess = true,
                    statusCode = statusCode,
                    dataObject = rawData as? JSONObject,
                    dataArray = rawData as? JSONArray,
                    requestId = reqId
                )
            } else {
                val errObj = rootJson.optJSONObject("error")
                val errCode = errObj?.optString("code", "API_ERROR") ?: "API_ERROR"
                val errMsg = errObj?.optString("message", "अनुरोध पूरा करने में समस्या आई।")
                    ?: "अनुरोध पूरा करने में समस्या आई।"

                return@withContext ApiResult(
                    isSuccess = false,
                    statusCode = statusCode,
                    errorCode = errCode,
                    errorMessage = errMsg,
                    requestId = reqId
                )
            }
        } catch (e: Exception) {
            return@withContext ApiResult(
                isSuccess = false,
                statusCode = 0,
                errorCode = "NETWORK_UNREACHABLE",
                errorMessage = "इंटरनेट या सर्वर कनेक्शन उपलब्ध नहीं है। कृपया नेटवर्क जांचकर पुनः प्रयास करें।"
            )
        } finally {
            connection?.disconnect()
        }
    }

    // ========================================================================
    // A. AUTH & ONE-TIME ONBOARDING (`users`, `farmers`, `buyers` अलग टेबल्स)
    // ========================================================================

    suspend fun getMyProfile(context: Context): ApiResult =
        request(context, "/auth/me", method = "GET")

    suspend fun completeOneTimeOnboarding(
        context: Context,
        name: String,
        phone: String,
        roleSelection: String,
        village: String?,
        district: String?,
        state: String?,
        farmingMethod: String? = null
    ): ApiResult {
        val body = JSONObject().apply {
            put("name", name.trim())
            put("phone", phone.trim())
            put("role_selection", roleSelection.lowercase())
            if (!village.isNullOrBlank()) put("village", village.trim())
            if (!district.isNullOrBlank()) put("district", district.trim())
            if (!state.isNullOrBlank()) put("state", state.trim())
            if (!farmingMethod.isNullOrBlank()) put("farming_method", farmingMethod.trim())
        }
        return request(context, "/auth/onboarding", method = "POST", bodyJson = body)
    }

    suspend fun switchActiveRole(context: Context, targetMode: String): ApiResult {
        val body = JSONObject().apply {
            put("active_role", targetMode.lowercase())
        }
        return request(context, "/auth/role", method = "PATCH", bodyJson = body)
    }

    suspend fun updateProfile(context: Context, updates: JSONObject): ApiResult =
        request(context, "/auth/profile", method = "PATCH", bodyJson = updates)

    // ========================================================================
    // B. ZERO-DEFAULT LIVE GOVERNMENT MANDI SYSTEM (Cascading + Graph + AI)
    // ========================================================================

    /** स्टेप 1: सरकारी सोर्स (data.gov.in) से लाइव राज्यों की सूची लाना (0% डिफ़ॉल्ट) */
    suspend fun getMandiStates(context: Context?, forceRefresh: Boolean = false): ApiResult =
        request(
            context = context,
            endpointPath = "/mandi-prices/states",
            method = "GET",
            queryParams = mapOf("refresh" to if (forceRefresh) "true" else null)
        )

    /** स्टेप 2: राज्य चुनने पर उस राज्य के सभी ज़िलों की लाइव सूची लाना */
    suspend fun getMandiDistricts(context: Context?, state: String, forceRefresh: Boolean = false): ApiResult =
        request(
            context = context,
            endpointPath = "/mandi-prices/districts",
            method = "GET",
            queryParams = mapOf(
                "state" to state,
                "refresh" to if (forceRefresh) "true" else null
            )
        )

    /** स्टेप 3: ज़िला चुनने पर उस ज़िले की सभी मंडियाँ, उपलब्ध फसलें और ताज़ा भाव लाना */
    suspend fun getMandisAndCommoditiesByDistrict(
        context: Context?,
        state: String?,
        district: String
    ): ApiResult =
        request(
            context = context,
            endpointPath = "/mandi-prices/mandis",
            method = "GET",
            queryParams = mapOf(
                "state" to state,
                "district" to district
            )
        )

    /** स्टेप 4: चयनित राज्य/ज़िला/मंडी/फसल के लाइव भाव (स्पेलिंग ऑटो-करेक्शन सहित) */
    suspend fun getLiveMandiRates(
        context: Context?,
        state: String? = null,
        district: String? = null,
        mandi: String? = null,
        commodity: String? = null,
        limit: Int = 100,
        forceRefresh: Boolean = false
    ): ApiResult =
        request(
            context = context,
            endpointPath = "/mandi-prices",
            method = "GET",
            queryParams = mapOf(
                "state" to state,
                "district" to district,
                "mandi" to mandi,
                "commodity" to commodity,
                "limit" to limit.toString(),
                "refresh" to if (forceRefresh) "true" else null
            )
        )

    /** स्टेप 5: 7 दिन से लेकर 1 साल (1y) और 2 साल (2y) तक का भाव उतार-चढ़ाव व ग्राफ डेटा */
    suspend fun getMandiPriceTrends(
        context: Context?,
        state: String? = null,
        district: String? = null,
        mandi: String? = null,
        commodity: String,
        range: String = "1y"
    ): ApiResult =
        request(
            context = context,
            endpointPath = "/mandi-prices/trends",
            method = "GET",
            queryParams = mapOf(
                "state" to state,
                "district" to district,
                "mandi" to mandi,
                "commodity" to commodity,
                "range" to range
            )
        )

    /** स्टेप 6: दो मंडियों (Mandi A vs Mandi B) की रेंज अनुसार भाव तुलना */
    suspend fun compareTwoMandis(
        context: Context?,
        state: String? = null,
        mandiA: String,
        mandiB: String,
        commodity: String,
        range: String = "1y"
    ): ApiResult =
        request(
            context = context,
            endpointPath = "/mandi-prices/compare",
            method = "GET",
            queryParams = mapOf(
                "state" to state,
                "mandi_a" to mandiA,
                "mandi_b" to mandiB,
                "commodity" to commodity,
                "range" to range
            )
        )

    /** स्टेप 7: किसान की लोकेशन और भाड़े के अनुसार सर्वोत्तम मंडी का AI सुझाव */
    suspend fun getAiMandiSellingAdvice(
        context: Context?,
        state: String?,
        farmerDistrict: String?,
        commodity: String,
        quantityQuintals: Double = 10.0
    ): ApiResult {
        val body = JSONObject().apply {
            if (!state.isNullOrBlank()) put("state", state.trim())
            if (!farmerDistrict.isNullOrBlank()) put("farmer_district", farmerDistrict.trim())
            put("commodity", commodity.trim())
            put("quantity_quintals", quantityQuintals)
        }
        return request(context, "/mandi-prices/ai-advisor", method = "POST", bodyJson = body)
    }

    /** स्टेप 8: स्पेलिंग मिस्टेक को सरकारी डेटा सेंटर की सही स्पेलिंग से मैच करना */
    suspend fun matchMandiSpelling(
        context: Context?,
        term: String,
        type: String = "commodity",
        state: String? = null,
        district: String? = null
    ): ApiResult {
        val body = JSONObject().apply {
            put("term", term.trim())
            put("type", type.lowercase())
            if (!state.isNullOrBlank()) put("state", state.trim())
            if (!district.isNullOrBlank()) put("district", district.trim())
        }
        return request(context, "/mandi-prices/match-spelling", method = "POST", bodyJson = body)
    }

    // ========================================================================
    // C. PRODUCTS, SEEDS, CROP PHOTOS & MARKETPLACE
    // ========================================================================

    suspend fun getProducts(
        context: Context?,
        category: String? = null,
        district: String? = null,
        farmerUid: String? = null,
        smallQuantityOnly: Boolean = false,
        freshTodayOnly: Boolean = false,
        searchQuery: String? = null
    ): ApiResult =
        request(
            context = context,
            endpointPath = "/products",
            method = "GET",
            queryParams = mapOf(
                "category" to category,
                "district" to district,
                "farmer_uid" to farmerUid,
                "small_quantity" to if (smallQuantityOnly) "true" else null,
                "fresh_today" to if (freshTodayOnly) "true" else null,
                "q" to searchQuery
            )
        )

    suspend fun getProductById(context: Context?, productId: String): ApiResult =
        request(context, "/products/$productId", method = "GET")

    suspend fun createProduct(context: Context, payload: JSONObject): ApiResult =
        request(context, "/products", method = "POST", bodyJson = payload)

    suspend fun updateProduct(context: Context, productId: String, updates: JSONObject): ApiResult =
        request(context, "/products/$productId", method = "PATCH", bodyJson = updates)

    suspend fun deleteProduct(context: Context, productId: String): ApiResult =
        request(context, "/products/$productId", method = "DELETE")

    suspend fun getCropImageByName(context: Context?, cropName: String, categoryHint: String? = null): ApiResult =
        request(
            context = context,
            endpointPath = "/media/crop-image",
            method = "GET",
            queryParams = mapOf("name" to cropName, "category" to categoryHint)
        )

    // ========================================================================
    // D. ORDERS, OFFERS (NEGOTIATION), BUYER REQUESTS & DISPUTES
    // ========================================================================

    suspend fun getOrders(context: Context, viewRole: String? = null): ApiResult =
        request(context, "/orders", method = "GET", queryParams = mapOf("view" to viewRole))

    suspend fun createOrder(context: Context, payload: JSONObject, idempotencyKey: String): ApiResult =
        request(context, "/orders", method = "POST", bodyJson = payload, customIdempotencyKey = idempotencyKey)

    suspend fun updateOrderStatus(context: Context, orderId: String, nextStatus: String): ApiResult {
        val body = JSONObject().apply { put("status", nextStatus) }
        return request(context, "/orders/$orderId/status", method = "PATCH", bodyJson = body)
    }

    suspend fun getOffers(context: Context): ApiResult =
        request(context, "/offers", method = "GET")

    suspend fun createOffer(context: Context, payload: JSONObject): ApiResult =
        request(context, "/offers", method = "POST", bodyJson = payload)

    suspend fun respondToOffer(
        context: Context,
        offerId: String,
        action: String,
        counterPriceRupees: Double? = null
    ): ApiResult {
        val body = JSONObject().apply {
            put("action", action.uppercase())
            if (counterPriceRupees != null && counterPriceRupees > 0) {
                put("counter_price_paise", Math.round(counterPriceRupees * 100))
            }
        }
        return request(context, "/offers/$offerId/respond", method = "PATCH", bodyJson = body)
    }

    suspend fun getBuyerRequests(context: Context?, district: String? = null): ApiResult =
        request(context, "/requests", method = "GET", queryParams = mapOf("district" to district))

    suspend fun createBuyerRequest(context: Context, payload: JSONObject): ApiResult =
        request(context, "/requests", method = "POST", bodyJson = payload)

    suspend fun respondToBuyerRequest(
        context: Context,
        requestId: String,
        offeredPriceRupees: Double,
        availableQty: Double,
        message: String
    ): ApiResult {
        val body = JSONObject().apply {
            put("offered_price_paise", Math.round(offeredPriceRupees * 100))
            put("available_quantity", availableQty)
            put("message", message.trim())
        }
        return request(context, "/requests/$requestId/respond", method = "POST", bodyJson = body)
    }

    suspend fun createDispute(context: Context, payload: JSONObject): ApiResult =
        request(context, "/disputes", method = "POST", bodyJson = payload)

    suspend fun getMyDisputes(context: Context): ApiResult =
        request(context, "/disputes/mine", method = "GET")

    suspend fun submitReview(context: Context, payload: JSONObject): ApiResult =
        request(context, "/trust/reviews", method = "POST", bodyJson = payload)

    // ========================================================================
    // E. AGRI-INPUTS (FUNGICIDE / PESTICIDE / FERTILIZER / SEEDS), DEALER & PICKUP
    // ========================================================================

    suspend fun getAgriInputs(
        context: Context?,
        category: String? = null,
        company: String? = null,
        district: String? = null,
        searchQuery: String? = null
    ): ApiResult =
        request(
            context = context,
            endpointPath = "/agri-inputs",
            method = "GET",
            queryParams = mapOf(
                "category" to category,
                "company" to company,
                "district" to district,
                "q" to searchQuery
            )
        )

    suspend fun getPickupPoints(context: Context?, district: String? = null): ApiResult =
        request(context, "/agri-inputs/pickup-points", method = "GET", queryParams = mapOf("district" to district))

    suspend fun bookAgriInputAtPickupPoint(context: Context, payload: JSONObject): ApiResult =
        request(context, "/agri-inputs/orders", method = "POST", bodyJson = payload)

    suspend fun dealerLogin(context: Context, loginId: String, password: String): ApiResult {
        val body = JSONObject().apply {
            put("login_id", loginId.trim())
            put("password", password)
        }
        return request(context, "/agri-inputs/dealer/login", method = "POST", bodyJson = body)
    }

    suspend fun dealerAddProduct(context: Context, payload: JSONObject): ApiResult =
        request(context, "/agri-inputs/dealer/products", method = "POST", bodyJson = payload, useDealerToken = true)

    suspend fun dealerGetOrders(context: Context): ApiResult =
        request(context, "/agri-inputs/dealer/orders", method = "GET", useDealerToken = true)

    suspend fun dealerUpdateOrderDispatch(context: Context, orderId: String, status: String): ApiResult {
        val body = JSONObject().apply { put("status", status) }
        return request(
            context,
            "/agri-inputs/dealer/orders/$orderId/dispatch",
            method = "PATCH",
            bodyJson = body,
            useDealerToken = true
        )
    }
}
