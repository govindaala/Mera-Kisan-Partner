// app/src/main/java/in/merakisan/app/ui/auth/SplashFragment.kt
package `in`.merakisan.app.ui.auth

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import `in`.merakisan.app.core.config.FeatureManager
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.security.SessionManager
import `in`.merakisan.app.ui.main.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * MERA KISAN — UNIFIED SPLASH & STARTUP GATEWAY
 * - Syncs remote Feature Flags & Parameters via ApiClient (Single Server Connection).
 * - Enforces Maintenance Mode & Force Update gates (Master Spec Sec 16, 23 & merged.txt Sec 35-36).
 * - Verifies existing Firebase Auth + One-Time Onboarding state so onboarded users go straight to Home.
 */
class SplashFragment : Fragment() {

    private lateinit var statusTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var retryButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#1B5E20"))
            setPadding(dp(28), dp(28), dp(28), dp(28))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val iconView = TextView(ctx).apply {
            text = "🌾"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 58f)
            gravity = Gravity.CENTER
        }

        val titleView = TextView(ctx).apply {
            text = "मेरा किसान (MERA KISAN)"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(6))
        }

        val taglineView = TextView(ctx).apply {
            text = "किसान से सीधे आपकी जरूरत तक"
            setTextColor(Color.parseColor("#E8F5E9"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(32))
        }

        progressBar = ProgressBar(ctx).apply {
            isIndeterminate = true
        }

        statusTextView = TextView(ctx).apply {
            text = "सर्वर कॉन्फ़िगरेशन और सुरक्षा जाँच हो रही है..."
            setTextColor(Color.parseColor("#C8E6C9"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
            setPadding(0, dp(18), 0, dp(16))
        }

        retryButton = Button(ctx).apply {
            text = "🔄 पुनः प्रयास करें"
            visibility = View.GONE
            setBackgroundColor(Color.parseColor("#BF360C"))
            setTextColor(Color.WHITE)
            setPadding(dp(20), dp(12), dp(20), dp(12))
            setOnClickListener {
                runStartupChecks()
            }
        }

        root.addView(iconView)
        root.addView(titleView)
        root.addView(taglineView)
        root.addView(progressBar)
        root.addView(statusTextView)
        root.addView(retryButton)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        runStartupChecks()
    }

    private fun runStartupChecks() {
        val ctx = context ?: return
        progressBar.visibility = View.VISIBLE
        retryButton.visibility = View.GONE
        statusTextView.text = "सर्वर से फीचर सेटिंग्स सिंक हो रही हैं..."

        viewLifecycleOwner.lifecycleScope.launch {
            // 1. लोकल कैश लोड करें और सर्वर से लाइव फीचर फ्लैग्स मंगाएँ
            FeatureManager.initFromLocalCache(ctx)
            FeatureManager.syncFromServer(ctx)

            if (!isAdded) return@launch

            // 2. Maintenance Mode की जाँच (merged.txt Sec 35 & 69)
            if (FeatureManager.isMaintenanceMode()) {
                progressBar.visibility = View.GONE
                statusTextView.text = FeatureManager.getMaintenanceMessageHi()
                retryButton.visibility = View.VISIBLE
                showMaintenanceDialog(FeatureManager.getMaintenanceMessageHi())
                return@launch
            }

            // 3. Force Update की जाँच (merged.txt Sec 36 & 70)
            if (FeatureManager.isForceUpdateRequired()) {
                progressBar.visibility = View.GONE
                statusTextView.text = "कृपया ऐप का नवीनतम संस्करण (${FeatureManager.getLatestAppVersion()}) अपडेट करें।"
                showForceUpdateDialog()
                return@launch
            }

            delay(300)
            if (!isAdded) return@launch

            // 4. प्रमाणीकरण व वन-टाइम ऑनबोर्डिंग की जाँच
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            if (firebaseUser != null) {
                statusTextView.text = "आपका खाता सत्यापित किया जा रहा है..."
                val profileResult = ApiClient.getMyProfile(ctx)

                if (!isAdded) return@launch

                if (profileResult.isSuccess && profileResult.dataObject != null) {
                    val data = profileResult.dataObject
                    val onboardingDone = data.optBoolean("onboarding_completed", false)
                    val phone = data.optString("phone", "")
                    val name = data.optString("name", firebaseUser.displayName ?: "")
                    val activeRole = data.optString("active_role", "farmer")
                    val verifStatus = data.optString("verification_status", "farmer_declared")
                    val village = data.optString("village", "")
                    val district = data.optString("district", "")
                    val state = data.optString("state", "")

                    val rolesArr = data.optJSONArray("roles")
                    val rolesList = mutableListOf<String>()
                    if (rolesArr != null) {
                        for (i in 0 until rolesArr.length()) {
                            val r = rolesArr.optString(i, "").trim().lowercase()
                            if (r.isNotEmpty()) rolesList.add(r)
                        }
                    }

                    if (onboardingDone && phone.isNotBlank() && rolesList.isNotEmpty()) {
                        SessionManager.saveOnboardedUserSession(
                            context = ctx,
                            uid = firebaseUser.uid,
                            name = name,
                            phone = phone,
                            roles = rolesList,
                            activeMode = activeRole,
                            village = village.takeIf { it.isNotBlank() && it != "null" },
                            district = district.takeIf { it.isNotBlank() && it != "null" },
                            state = state.takeIf { it.isNotBlank() && it != "null" },
                            verificationStatus = verifStatus
                        )
                        (activity as? MainActivity)?.onUserAuthenticatedAndOnboarded()
                        return@launch
                    }
                } else if (profileResult.statusCode == 0 && SessionManager.isLoggedInAndOnboarded(ctx)) {
                    // यदि ग्रामीण क्षेत्र में क्षणिक नेटवर्क अवरोध हो लेकिन यूज़र पहले से ऑनबोर्डेड है
                    (activity as? MainActivity)?.onUserAuthenticatedAndOnboarded()
                    return@launch
                }
            }

            // 5. यदि लॉगिन नहीं है या वन-टाइम ऑनबोर्डिंग बाकी है -> LoginFragment खोलें
            (activity as? MainActivity)?.openLoginAndOnboardingScreen()
        }
    }

    private fun showMaintenanceDialog(messageHi: String) {
        val ctx = context ?: return
        AlertDialog.Builder(ctx)
            .setTitle("🛠️ सिस्टम रखरखाव (Maintenance)")
            .setMessage(messageHi)
            .setCancelable(false)
            .setPositiveButton("पुनः जांचें") { dialog, _ ->
                dialog.dismiss()
                runStartupChecks()
            }
            .show()
    }

    private fun showForceUpdateDialog() {
        val ctx = context ?: return
        AlertDialog.Builder(ctx)
            .setTitle("📲 ऐप अपडेट आवश्यक है")
            .setMessage(
                "मेरा किसान ऐप का नया और अधिक सुरक्षित संस्करण (${FeatureManager.getLatestAppVersion()}) उपलब्ध है। " +
                    "कृपया जारी रखने के लिए ऐप अपडेट करें।"
            )
            .setCancelable(false)
            .setPositiveButton("अभी अपडेट करें") { _, _ ->
                try {
                    val pkg = ctx.packageName
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")))
                } catch (_: Exception) {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=${ctx.packageName}")
                        )
                    )
                }
            }
            .show()
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
}
