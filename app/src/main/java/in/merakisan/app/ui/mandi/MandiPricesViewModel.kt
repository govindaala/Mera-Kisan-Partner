// app/src/main/java/in/merakisan/app/ui/mandi/MandiPricesViewModel.kt
package `in`.merakisan.app.ui.mandi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.network.model.MandiPriceDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MandiPricesViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = ApiClient.getApiService(application)

    private val _mandiPrices = MutableLiveData<List<MandiPriceDto>>()
    val mandiPrices: LiveData<List<MandiPriceDto>> get() = _mandiPrices

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    private var allFetchedPrices: List<MandiPriceDto> = emptyList()

    fun loadMandiPrices(district: String? = null) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getMandiPrices(district)
                if (response.isSuccessful && response.body()?.success == true) {
                    val list = response.body()?.data ?: emptyList()
                    allFetchedPrices = list

                    withContext(Dispatchers.Main) {
                        _mandiPrices.value = list
                        _isLoading.value = false
                        if (list.isEmpty()) {
                            _errorMessage.value = "इस क्षेत्र के लिए आज का कोई मंडी भाव उपलब्ध नहीं है।"
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        allFetchedPrices = emptyList()
                        _mandiPrices.value = emptyList()
                        _isLoading.value = false
                        _errorMessage.value = "मंडी भाव सर्वर से प्राप्त नहीं हो सके।"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    allFetchedPrices = emptyList()
                    _mandiPrices.value = emptyList()
                    _isLoading.value = false
                    _errorMessage.value = "नेटवर्क कनेक्शन अनुपलब्ध है। कृपया इंटरनेट चालू करें।"
                }
            }
        }
    }

    fun filterPrices(query: String) {
        val q = query.trim().lowercase()
        if (q.isBlank()) {
            _mandiPrices.value = allFetchedPrices
            return
        }

        val filtered = allFetchedPrices.filter { item ->
            item.commodity.lowercase().contains(q) ||
            item.market.lowercase().contains(q) ||
            item.district.lowercase().contains(q) ||
            item.variety.orEmpty().lowercase().contains(q)
        }
        _mandiPrices.value = filtered
    }
}
