package in.merakisan.app.ui.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import in.merakisan.app.R
import in.merakisan.app.core.config.FeatureManager
import in.merakisan.app.core.security.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class HomeFragment : Fragment() {

    private lateinit var tvGreeting: TextView
    private lateinit var tvUserLocation: TextView
    private lateinit var btnAddCrop: Button
    private lateinit var cardMandiBhav: CardView
    private lateinit var cardDemandBoard: CardView
    private lateinit var tvActiveCropsCount: TextView
    private lateinit var tvPendingOrdersCount: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        tvGreeting = view.findViewById(R.id.tv_greeting)
        tvUserLocation = view.findViewById(R.id.tv_user_location)
        btnAddCrop = view.findViewById(R.id.btn_add_crop)
        cardMandiBhav = view.findViewById(R.id.card_mandi_bhav)
        cardDemandBoard = view.findViewById(R.id.card_demand_board)
        tvActiveCropsCount = view.findViewById(R.id.tv_active_crops_count)
        tvPendingOrdersCount = view.findViewById(R.id.tv_pending_orders_count)

        // 1. SessionManager या SharedPreferences से असली नाम लोड करना
        val userName = try {
            SessionManager.getUserName(requireContext())
        } catch (_: Throwable) {
            val prefs = requireContext().getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)
            prefs.getString("user_name", "किसान साथी")
        } ?: "किसान साथी"

        val district = try {
            SessionManager.getUserDistrict(requireContext())
        } catch (_: Throwable) {
            val prefs = requireContext().getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)
            prefs.getString("user_district", "मध्य प्रदेश")
        } ?: "मध्य प्रदेश"

        tvGreeting.text = "नमस्ते, $userName"
        tvUserLocation.text = district

        // 2. नई फसल जोड़ें बटन का क्लिक इवेंट
        btnAddCrop.setOnClickListener {
            try {
                findNavController().navigate(R.id.addProductFragment)
            } catch (_: Throwable) {
                try {
                    findNavController().navigate(R.id.addCropFragment)
                } catch (_: Throwable) {}
            }
        }

        // 3. मंडी भाव कार्ड का क्लिक इवेंट
        cardMandiBhav.setOnClickListener {
            try {
                findNavController().navigate(R.id.mandiPricesFragment)
            } catch (_: Throwable) {}
        }

        // 4. मांग बोर्ड कार्ड का क्लिक इवेंट
        cardDemandBoard.setOnClickListener {
            try {
                findNavController().navigate(R.id.buyerRequestsFragment)
            } catch (_: Throwable) {}
        }

        // 5. FeatureManager और सर्वर से स्थिति लोड करना
        loadFeatureStatesAndCounters()

        return view
    }

    private fun loadFeatureStatesAndCounters() {
        // FeatureManager की जांच
        val isMandiOn = try {
            FeatureManager.isFeatureEnabled("mandi_price") || FeatureManager.isFeatureEnabled("mandi_prices")
        } catch (_: Throwable) {
            true
        }
        val isDemandOn = try {
            FeatureManager.isFeatureEnabled("buyer_request") || FeatureManager.isFeatureEnabled("demand_board")
        } catch (_: Throwable) {
            true
        }

        cardMandiBhav.visibility = if (isMandiOn) View.VISIBLE else View.GONE
        cardDemandBoard.visibility = if (isDemandOn) View.VISIBLE else View.GONE

        // बैकएंड से लाइव काउंटर्स लोड करना
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val uid = try {
                    SessionManager.getUserUid(requireContext())
                } catch (_: Throwable) {
                    val prefs = requireContext().getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)
                    prefs.getString("user_uid", "") ?: ""
                }

                if (uid.isNotEmpty()) {
                    val url = URL("https://mera-kisan-backend.vercel.app/api/v1/products?farmer_uid=$uid")
                    val conn = url.openConnection() as HttpURLConnection
                    if (conn.responseCode == 200) {
                        val text = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(text)
                        val dataArr = json.optJSONArray("data")
                        val count = dataArr?.length() ?: 0
                        withContext(Dispatchers.Main) {
                            tvActiveCropsCount.text = count.toString()
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }
}
