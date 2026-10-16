package `in`.merakisan.app.ui.farmer

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import `in`.merakisan.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddProductFragment : Fragment() {

    private var etProductName: EditText? = null
    private var actCategory: AutoCompleteTextView? = null
    private var etVariety: EditText? = null
    private var etPrice: EditText? = null
    private var actUnit: AutoCompleteTextView? = null
    private var etTotalQuantity: EditText? = null
    private var etMinOrderQuantity: EditText? = null
    private var etHarvestDate: EditText? = null
    private var actVerificationStatus: AutoCompleteTextView? = null
    private var etVillage: EditText? = null
    private var etDistrict: EditText? = null
    private var etDescription: EditText? = null
    private var btnPublish: View? = null
    private var progressBar: ProgressBar? = null

    private val categories = arrayOf(
        "अनाज (Grains)",
        "दाल / तिलहन (Pulses & Oilseeds)",
        "सब्जियां (Fresh Vegetables)",
        "फल (Fresh Fruits)",
        "देशी / पारंपरिक (Desi Traditional)",
        "मसाले (Spices)",
        "गुड़ (Jaggery)",
        "कच्ची घानी तेल (Cold Pressed Oil)",
        "अन्य (Other)"
    )

    private val units = arrayOf(
        "किलो (kg)",
        "क्विंटल (quintal)",
        "लीटर (litre)",
        "दर्जन (dozen)",
        "नग (piece)",
        "बंडल (bundle)"
    )

    private val verificationLevels = arrayOf(
        "किसान द्वारा घोषित (Farmer Declared)",
        "प्राकृतिक खेती (Natural Farming)",
        "प्रमाणित जैविक (Organic Certified)",
        "दस्तावेज़ सत्यापित (Document Verified)"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_product, container, false)

        resolveAllViews(view)
        setupDropdowns()
        setupDatePicker()
        setupPublishAction()

        return view
    }

    private fun resolveAllViews(v: View) {
        // सेफ रिज़ॉल्वर: चाहे लेआउट में camelCase हो या snake_case, यह तुरंत ढूंढ लेगा
        etProductName = findViewSafe(v, "et_product_name", "etProductName", "etName")
        actCategory = findViewSafe(v, "act_category", "actCategory", "spinnerCategory", "autoCompleteCategory")
        etVariety = findViewSafe(v, "et_variety", "etVariety")
        etPrice = findViewSafe(v, "et_price", "etPrice")
        actUnit = findViewSafe(v, "act_unit", "actUnit", "spinnerUnit", "autoCompleteUnit")
        etTotalQuantity = findViewSafe(v, "et_total_quantity", "etTotalQuantity", "etQuantity")
        etMinOrderQuantity = findViewSafe(v, "et_min_order_quantity", "etMinOrderQuantity", "etMinQuantity")
        etHarvestDate = findViewSafe(v, "et_harvest_date", "etHarvestDate")
        actVerificationStatus = findViewSafe(v, "act_verification_status", "actVerificationStatus", "spinnerVerification")
        etVillage = findViewSafe(v, "et_village", "etVillage")
        etDistrict = findViewSafe(v, "et_district", "etDistrict")
        etDescription = findViewSafe(v, "et_description", "etDescription")
        btnPublish = findViewSafe(v, "btn_publish_crop", "btnPublishCrop", "btnPublish", "btnSubmit")
        progressBar = findViewSafe(v, "progressBarPublish", "progressBar", "progress_bar")

        val prefs = requireContext().getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)
        val defaultDistrict = prefs.getString("user_district", "मंदसौर")
        etDistrict?.setText(defaultDistrict)
    }

    private fun setupDropdowns() {
        // 1. श्रेणी ड्रॉपडाउन (टैप करते ही मेन्यू पॉपअप खोलना)
        actCategory?.let { act ->
            act.inputType = InputType.TYPE_NULL
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
            act.setAdapter(adapter)
            act.setOnClickListener { act.showDropDown() }
            act.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) act.showDropDown() }
        }

        // 2. इकाई ड्रॉपडाउन
        actUnit?.let { act ->
            act.inputType = InputType.TYPE_NULL
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, units)
            act.setAdapter(adapter)
            act.setOnClickListener { act.showDropDown() }
            act.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) act.showDropDown() }
        }

        // 3. सत्यापन स्तर ड्रॉपडाउन
        actVerificationStatus?.let { act ->
            act.inputType = InputType.TYPE_NULL
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, verificationLevels)
            act.setAdapter(adapter)
            act.setOnClickListener { act.showDropDown() }
            act.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) act.showDropDown() }
        }
    }

    private fun setupDatePicker() {
        etHarvestDate?.let { et ->
            et.inputType = InputType.TYPE_NULL
            et.isFocusable = false

            val calendar = Calendar.getInstance()
            val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                et.setText(sdf.format(calendar.time))
            }

            val showPicker = {
                DatePickerDialog(
                    requireContext(),
                    dateSetListener,
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }

            et.setOnClickListener { showPicker() }
        }
    }

    private fun setupPublishAction() {
        btnPublish?.setOnClickListener {
            val name = etProductName?.text?.toString()?.trim() ?: ""
            val category = actCategory?.text?.toString()?.trim() ?: ""
            val variety = etVariety?.text?.toString()?.trim() ?: ""
            val priceStr = etPrice?.text?.toString()?.trim() ?: ""
            val unitRaw = actUnit?.text?.toString()?.trim() ?: ""
            val totalQtyStr = etTotalQuantity?.text?.toString()?.trim() ?: ""
            val minOrderStr = etMinOrderQuantity?.text?.toString()?.trim() ?: ""
            val harvestDate = etHarvestDate?.text?.toString()?.trim() ?: ""
            val verification = actVerificationStatus?.text?.toString()?.trim() ?: "Farmer Declared"
            val village = etVillage?.text?.toString()?.trim() ?: ""
            val district = etDistrict?.text?.toString()?.trim() ?: "मध्य प्रदेश"
            val desc = etDescription?.text?.toString()?.trim() ?: ""

            if (name.isEmpty()) {
                etProductName?.error = "फसल का नाम दर्ज करें"
                etProductName?.requestFocus()
                return@setOnClickListener
            }
            if (category.isEmpty()) {
                actCategory?.error = "कृपया श्रेणी चुनें"
                actCategory?.showDropDown()
                return@setOnClickListener
            }
            if (priceStr.isEmpty()) {
                etPrice?.error = "मूल्य दर्ज करें"
                etPrice?.requestFocus()
                return@setOnClickListener
            }
            if (unitRaw.isEmpty()) {
                actUnit?.error = "इकाई चुनें"
                actUnit?.showDropDown()
                return@setOnClickListener
            }
            if (totalQtyStr.isEmpty()) {
                etTotalQuantity?.error = "कुल मात्रा दर्ज करें"
                etTotalQuantity?.requestFocus()
                return@setOnClickListener
            }

            val pricePaise = (priceStr.toDoubleOrNull()?.times(100))?.toLong() ?: 0L
            val totalQty = totalQtyStr.toDoubleOrNull() ?: 1.0
            val minQty = minOrderStr.toDoubleOrNull() ?: 1.0

            val unitCode = when {
                unitRaw.contains("kg", ignoreCase = true) -> "kg"
                unitRaw.contains("quintal", ignoreCase = true) -> "quintal"
                unitRaw.contains("litre", ignoreCase = true) -> "litre"
                else -> "kg"
            }

            val prefs = requireContext().getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)
            val farmerUid = prefs.getString("user_uid", "anonymous_farmer") ?: "anonymous_farmer"
            val farmerName = prefs.getString("user_name", "किसान साथी") ?: "किसान साथी"

            val payload = JSONObject().apply {
                put("name", name)
                put("category", category)
                put("variety", variety)
                put("price_paise", pricePaise)
                put("unit", unitCode)
                put("stock_quantity", totalQty)
                put("minimum_order_quantity", minQty)
                put("harvest_date", harvestDate)
                put("verification_status", verification)
                put("village", village)
                put("district", district)
                put("description", desc)
                put("farmer_uid", farmerUid)
                put("seller_name", farmerName)
            }

            publishProductToBackend(payload)
        }
    }

    private fun publishProductToBackend(payload: JSONObject) {
        progressBar?.visibility = View.VISIBLE
        btnPublish?.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://mera-kisan-backend.vercel.app/api/v1/products")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 12000
                conn.readTimeout = 12000

                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                val code = conn.responseCode

                withContext(Dispatchers.Main) {
                    progressBar?.visibility = View.GONE
                    btnPublish?.isEnabled = true

                    if (code in 200..299) {
                        Toast.makeText(requireContext(), "🌾 फसल सफलतापूर्वक प्रकाशित हो गई!", Toast.LENGTH_LONG).show()
                        findNavController().popBackStack()
                    } else {
                        Toast.makeText(requireContext(), "फसल प्रकाशित नहीं हो सकी (Code $code)", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar?.visibility = View.GONE
                    btnPublish?.isEnabled = true
                    Toast.makeText(requireContext(), "नेटवर्क त्रुटि: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun <T : View> findViewSafe(root: View, vararg idNames: String): T? {
        val context = root.context
        val packageName = context.packageName
        for (name in idNames) {
            val id = context.resources.getIdentifier(name, "id", packageName)
            if (id != 0) {
                val found = root.findViewById<T>(id)
                if (found != null) return found
            }
        }
        return null
    }
}
