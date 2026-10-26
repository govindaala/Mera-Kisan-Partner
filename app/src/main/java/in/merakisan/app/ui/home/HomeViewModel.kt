// app/src/main/java/in/merakisan/app/ui/home/HomeViewModel.kt
package `in`.merakisan.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import `in`.merakisan.app.core.config.FeatureManager
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.security.SessionManager
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * MERA KISAN — UNIFIED HOME VIEWMODEL (FARMER & BUYER MODES)
 * Exact Repository Path: app/src/main/java/in/merakisan/app/ui/home/HomeViewModel.kt
 *
 * Resolves `HomeViewModel.kt:54:43 Unresolved reference: consumeLastCrashSummary`
 * and synchronizes Home state with ApiClient, FeatureManager, and SessionManager.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val sessionManager = SessionManager(appContext)
    private val apiService = ApiClient.getApiService(appContext)

    data class HomeUiState(
        val isLoading: Boolean = false,
        val userName: String = "",
        val activeRole: String = "farmer",
        val isFarmerMode: Boolean = true,
        val district: String = "",
        val village: String = "",
        val bannerText: String = "",
        val announcementText: String = "",
        val freshTodayProducts: JSONArray = JSONArray(),
        val nearbyProducts: JSONArray = JSONArray(),
        val buyerRequests: JSONArray = JSONArray(),
        val activeProductsCount: Int = 0,
        val pendingOrdersCount: Int = 0,
        val lastCrashSummary: String? = null,
        val errorMessage: String? = null
    )

    private val _uiState = MutableLiveData(HomeUiState())
    val uiState: LiveData<HomeUiState> = _uiState

    private val _lastCrashReport = MutableLiveData<String?>(null)
    val lastCrashReport: LiveData<String?> = _lastCrashReport

    init {
        checkDiagnosticsAndLoadHome()
    }

    fun checkDiagnosticsAndLoadHome() {
        val crashSummary = SessionManager.consumeLastCrashSummary(appContext)
        _lastCrashReport.value = crashSummary
        refreshHomeDashboard()
    }

    fun refreshHomeDashboard() {
        val session = sessionManager.getUserSession()
        val isFarmer = sessionManager.isFarmerModeActive()

        _uiState.value = (_uiState.value ?: HomeUiState()).copy(
            isLoading = true,
            userName = session.name,
            activeRole = session.role,
            isFarmerMode = isFarmer,
            district = session.district,
            village = session.village,
            bannerText = FeatureManager.getHomeBannerText(),
            announcementText = FeatureManager.getAnnouncementText(),
            lastCrashSummary = _lastCrashReport.value,
            errorMessage = null
        )

        viewModelScope.launch {
            FeatureManager.syncFromServer(appContext)

            val productsRes = if (isFarmer && session.uid.isNotBlank()) {
                ApiClient.getProducts(appContext, farmerUid = session.uid)
            } else {
                ApiClient.getProducts(appContext, district = session.district.ifBlank { null })
            }

            val freshRes = ApiClient.getProducts(appContext, freshTodayOnly = true)
            val requestsRes = ApiClient.getBuyerRequests(appContext, district = session.district.ifBlank { null })

            val nearbyArr = productsRes.dataArray ?: JSONArray()
            val freshArr = freshRes.dataArray ?: JSONArray()
            val reqArr = requestsRes.dataArray ?: JSONArray()

            _uiState.postValue(
                HomeUiState(
                    isLoading = false,
                    userName = session.name,
                    activeRole = session.role,
                    isFarmerMode = isFarmer,
                    district = session.district,
                    village = session.village,
                    bannerText = FeatureManager.getHomeBannerText(),
                    announcementText = FeatureManager.getAnnouncementText(),
                    freshTodayProducts = freshArr,
                    nearbyProducts = nearbyArr,
                    buyerRequests = reqArr,
                    activeProductsCount = nearbyArr.length(),
                    pendingOrdersCount = 0,
                    lastCrashSummary = _lastCrashReport.value,
                    errorMessage = if (!productsRes.isSuccess) productsRes.errorMessage else null
                )
            )
        }
    }

    fun switchRoleMode(targetMode: String) {
        sessionManager.setActiveMode(targetMode)
        viewModelScope.launch {
            ApiClient.switchActiveRole(appContext, targetMode.lowercase())
            refreshHomeDashboard()
        }
    }

    fun clearCrashReportBanner() {
        _lastCrashReport.value = null
    }
}
