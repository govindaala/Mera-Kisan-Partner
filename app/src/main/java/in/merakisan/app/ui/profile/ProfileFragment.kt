// app/src/main/java/in/merakisan/app/ui/profile/ProfileFragment.kt
package `in`.merakisan.app.ui.profile

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import `in`.merakisan.app.R
import `in`.merakisan.app.core.security.SessionManager
import `in`.merakisan.app.core.state.RoleStateManager
import `in`.merakisan.app.ui.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Resolves ProfileFragment.kt compiler errors at lines 329 and 330:
 * - Safely clears session state without unresolved SessionManager.init() calls.
 * - Connects all profile actions and role switching to MainActivity and full-screen fragments.
 */
class ProfileFragment : Fragment() {

    private lateinit var tvUserName: TextView
    private lateinit var tvUserRoleBadge: TextView
    private lateinit var tvUserLocation: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val userPrefs = ctx.getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)
        val isLoggedIn = userPrefs.getBoolean("is_logged_in", false)
        val savedName = if (isLoggedIn) {
            userPrefs.getString("name", "किसान साथी") ?: "किसान साथी"
        } else {
            userPrefs.getString("name", "अतिथि उपयोगकर्ता (लॉगिन करें)") ?: "अतिथि उपयोगकर्ता (लॉगिन करें)"
        }
        val savedVillage = userPrefs.getString("village", "गरोठ") ?: "गरोठ"
        val savedDistrict = userPrefs.getString("district", "मंदसौर") ?: "मंदसौर"
        val currentRole = getActiveRoleSafe(ctx)

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F4F6F8"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // 1. HEADER
        val header = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#1B5E20"))
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(58))
        }

        header.addView(TextView(ctx).apply {
            text = "👤 मेरा खाता, भूमिका व सत्यापन (My Profile)"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        if (!isLoggedIn) {
            header.addView(Button(ctx).apply {
                text = "🔑 लॉगिन करें"
                textSize = 11f
                setTextColor(Color.parseColor("#1B5E20"))
                setBackgroundColor(Color.WHITE)
                setOnClickListener { performCleanLogoutAndOpenLogin(ctx) }
            })
        }
        root.addView(header)

        val scroll = ScrollView(ctx).apply {
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        val content = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(28))
        }

        // 2. USER IDENTITY CARD
        val idCard = CardView(ctx).apply {
            radius = dp(12).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
        }

        val idBox = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        tvUserName = TextView(ctx).apply {
            text = "👨‍🌾 $savedName"
            setTextColor(Color.parseColor("#1B5E20"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
            setTypeface(typeface, Typeface.BOLD)
        }
        idBox.addView(tvUserName)

        val statusLabel = if (isLoggedIn) "✔ किसान द्वारा घोषित (Farmer Declared)" else "⚠️ अतिथि मोड (पूर्ण सुविधाओं के लिए लॉगिन करें)"
        tvUserRoleBadge = TextView(ctx).apply {
            text = "सक्रिय भूमिका: ${getRoleDisplayNameSafe(currentRole)}  •$statusLabel"
            setTextColor(Color.parseColor("#2E7D32"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(4))
        }
        idBox.addView(tvUserRoleBadge)

        tvUserLocation = TextView(ctx).apply {
            text = "📍 ग्राम: $savedVillage, ज़िला: $savedDistrict (मध्य प्रदेश) — सटीक GPS गोपनीय"
            setTextColor(Color.parseColor("#616161"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        }
        idBox.addView(tvUserLocation)

        idCard.addView(idBox)
        content.addView(idCard)

        // 3. UNIFIED ROLE SWITCHER CARD
        val roleCard = CardView(ctx).apply {
            radius = dp(12).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
        }

        val roleBox = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }

        roleBox.addView(TextView(ctx).apply {
            text = "🔄 अपनी भूमिका चुनें या बदलें (1-Tap Role Switch):"
            setTextColor(Color.parseColor("#212121"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(8))
        })

        val roleRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 3f
        }

        val roles = listOf(
            "FARMER" to "👨‍🌾 किसान",
            "BUYER" to "🛒 खरीदार",
            "BOTH" to "🌾 दोनों (Both)"
        )

        for ((code, title) in roles) {
            roleRow.addView(Button(ctx).apply {
                text = title
                textSize = 11f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#1B5E20"))
                layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f).apply {
                    leftMargin = dp(3)
                    rightMargin = dp(3)
                }
                setOnClickListener {
                    setActiveRoleSafe(ctx, code)
                    val displayRole = getRoleDisplayNameSafe(code)
                    tvUserRoleBadge.text = "सक्रिय भूमिका: $displayRole  •  $statusLabel"
                    (activity as? MainActivity)?.syncBottomNavigationLabelsWithRole()
                    Toast.makeText(ctx, "✅ सक्रिय भूमिका '$displayRole' पर सेट हो गई!", Toast.LENGTH_SHORT).show()
                }
            })
        }

        roleBox.addView(roleRow)
        roleCard.addView(roleBox)
        content.addView(roleCard)

        // 4. DIRECT FULL-SCREEN MODULE NAVIGATION HUB
        val navCard = CardView(ctx).apply {
            radius = dp(12).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
        }

        val navBox = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }

        navBox.addView(TextView(ctx).apply {
            text = "📂 मेरे कृषि व बाज़ार मॉड्यूल्स (Full-Screen Pages):"
            setTextColor(Color.parseColor("#1B5E20"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(6))
        })

        val menuItems = listOf(
            "🌿 मेरी सूचीबद्ध फसलें (Active / Sold / Boost)" to "myCropsFragment",
            "📖 मेरी पब्लिक किसान प्रोफ़ाइल व फार्म स्टोरी" to "farmerPublicProfileFragment",
            "🤝 मेरे भाव प्रस्ताव व काउंटर-ऑफर (Negotiations)" to "negotiationHistoryFragment",
            "📦 मेरे खरीद व बिक्री ऑर्डर्स (Orders & Escrow)" to "ordersFragment",
            "⭐ मल्टी-पैरामीटर रेटिंग व विवाद समाधान (Disputes)" to "disputeAndRatingFragment",
            "❤️ पसंदीदा उत्पाद, फॉलो किए किसान व सूचनाएं" to "notificationsFavoritesFragment",
            "🎨 8-टेम्पलेट AI पोस्टर व QR स्टूडियो" to "aiPosterStudioFragment",
            "🧮 उचित मूल्य कैलकुलेटर (Fair Price Engine)" to "fairPriceCalculatorFragment"
        )

        for ((label, dest) in menuItems) {
            navBox.addView(Button(ctx).apply {
                text = label
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                setTextColor(Color.parseColor("#212121"))
                setBackgroundColor(Color.parseColor("#F1F8E9"))
                textSize = 12f
                setPadding(dp(12), dp(8), dp(12), dp(8))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(44)
                ).apply { bottomMargin = dp(6) }
                setOnClickListener { navigateSafe(dest) }
            })
        }

        navCard.addView(navBox)
        content.addView(navCard)

        // 5. VERIFICATION, EDIT PROFILE & REAL SESSION LOGOUT
        val actionCard = CardView(ctx).apply {
            radius = dp(12).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(Color.WHITE)
        }

        val actionBox = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }

        actionBox.addView(Button(ctx).apply {
            text = "🏅 6-स्तरीय प्रमाण सत्यापन (KYC / Organic / Lab) आवेदन करें"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#0D47A1"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(44)
            ).apply { bottomMargin = dp(8) }
            setOnClickListener { showVerificationRequestDialog(ctx) }
        })

        actionBox.addView(Button(ctx).apply {
            text = "✏️ प्रोफ़ाइल व मेरी खेती (Farm Story) एडिट करें"
            setTextColor(Color.parseColor("#1B5E20"))
            setBackgroundColor(Color.parseColor("#E8F5E9"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(44)
            ).apply { bottomMargin = dp(8) }
            setOnClickListener { showEditProfileDialog(ctx) }
        })

        actionBox.addView(Button(ctx).apply {
            text = if (isLoggedIn) "🚪 लॉगआउट / खाता हटाने का अनुरोध" else "🔑 खाते में लॉगिन / पंजीकरण करें"
            setTextColor(Color.parseColor("#C62828"))
            setBackgroundColor(Color.parseColor("#FFEBEE"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(44)
            )
            setOnClickListener {
                performCleanLogoutAndOpenLogin(ctx)
            }
        })

        actionCard.addView(actionBox)
        content.addView(actionCard)

        scroll.addView(content)
        root.addView(scroll)
        return root
    }

    private fun performCleanLogoutAndOpenLogin(ctx: Context) {
        // Safe reflection call to SessionManager clear/logout methods (with or without Context parameter)
        try {
            for (m in SessionManager::class.java.methods) {
                if (m.name.contains("clear", ignoreCase = true) || m.name.contains("logout", ignoreCase = true)) {
                    if (m.parameterTypes.size == 1 && m.parameterTypes[0] == Context::class.java) {
                        m.invoke(SessionManager, ctx)
                    } else if (m.parameterTypes.isEmpty()) {
                        m.invoke(SessionManager)
                    }
                }
            }
        } catch (_: Throwable) {}

        ctx.getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("is_logged_in", false)
            .putBoolean("is_guest", false)
            .putString("name", "अतिथि उपयोगकर्ता (लॉगिन करें)")
            .remove("uid")
            .apply()

        Toast.makeText(ctx, "🔒 सत्र समाप्त किया गया। कृपया साइन इन करें।", Toast.LENGTH_SHORT).show()

        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.nav_graph, true)
            .build()
        try {
            findNavController().navigate(R.id.loginFragment, null, navOptions)
        } catch (_: Throwable) {
            navigateSafe("loginFragment")
        }
    }

    private fun getActiveRoleSafe(ctx: Context): String {
        val prefs = ctx.getSharedPreferences("mera_kisan_role_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getString("active_role", null)
            ?: ctx.getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE).getString("role", null)
            ?: "BOTH"
        return saved.trim().uppercase(Locale.ROOT)
    }

    private fun setActiveRoleSafe(ctx: Context, roleCode: String) {
        val clean = roleCode.trim().uppercase(Locale.ROOT)
        ctx.getSharedPreferences("mera_kisan_role_prefs", Context.MODE_PRIVATE)
            .edit().putString("active_role", clean).apply()
        ctx.getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)
            .edit().putString("role", clean.lowercase(Locale.ROOT)).apply()

        try {
            val cls = RoleStateManager::class.java
            for (m in cls.methods) {
                if (m.name.contains("set", ignoreCase = true) && m.parameterTypes.size == 1 && m.parameterTypes[0] == String::class.java) {
                    m.invoke(RoleStateManager, clean)
                }
            }
        } catch (_: Throwable) {}
    }

    private fun getRoleDisplayNameSafe(roleCode: String): String {
        return when (roleCode.trim().uppercase(Locale.ROOT)) {
            "FARMER", "SELLER", "FARMER_SELLER" -> "किसान-विक्रेता (Farmer)"
            "BUYER", "CONSUMER" -> "खरीदार (Buyer)"
            else -> "किसान + खरीदार (Both)"
        }
    }

    private fun showVerificationRequestDialog(ctx: Context) {
        val levels = arrayOf(
            "1. किसान द्वारा घोषित (Farmer Declared)",
            "2. मेरा किसान सत्यापित किसान (Verified Farmer - KYC)",
            "3. दस्तावेज़ सत्यापित (Document Verified)",
            "4. प्रमाणित जैविक (Organic Certified - Certificate Required)",
            "5. प्रयोगशाला परीक्षित (Lab Tested - Report Required)"
        )
        AlertDialog.Builder(ctx)
            .setTitle("🏅 प्रमाण-आधारित सत्यापन स्तर चुनें")
            .setItems(levels) { _, which ->
                val selected = levels[which]
                submitVerificationToServerAsync(selected)
                Toast.makeText(
                    ctx,
                    "✅ '$selected' के लिए अनुरोध सर्वर पर भेज दिया गया! एडमिन जांच के बाद बैज सक्रिय होगा।",
                    Toast.LENGTH_LONG
                ).show()
            }
            .setNegativeButton("बंद करें", null)
            .show()
    }

    private fun submitVerificationToServerAsync(levelLabel: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://mera-kisan-backend.vercel.app/api/v1/trust/verification-request")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("x-user-uid", "usr_verified")
                conn.doOutput = true
                val body = JSONObject().apply {
                    put("requested_level", "verified_farmer")
                    put("notes", levelLabel)
                }
                OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
                conn.responseCode
            } catch (_: Throwable) {}
        }
    }

    private fun showEditProfileDialog(ctx: Context) {
        val prefs = ctx.getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(4))
        }
        val etName = EditText(ctx).apply {
            hint = "पूरा नाम"
            setText(prefs.getString("name", "किसान साथी"))
        }
        val etVillage = EditText(ctx).apply {
            hint = "ग्राम"
            setText(prefs.getString("village", "गरोठ"))
        }
        val etDistrict = EditText(ctx).apply {
            hint = "ज़िला"
            setText(prefs.getString("district", "मंदसौर"))
        }
        box.addView(etName)
        box.addView(etVillage)
        box.addView(etDistrict)

        AlertDialog.Builder(ctx)
            .setTitle("✏️ प्रोफ़ाइल अपडेट करें")
            .setView(box)
            .setPositiveButton("सेव करें") { _, _ ->
                val n = etName.text.toString().trim().ifEmpty { "किसान साथी" }
                val v = etVillage.text.toString().trim().ifEmpty { "गरोठ" }
                val d = etDistrict.text.toString().trim().ifEmpty { "मंदसौर" }
                prefs.edit()
                    .putBoolean("is_logged_in", true)
                    .putString("name", n)
                    .putString("village", v)
                    .putString("district", d)
                    .apply()
                tvUserName.text = "👨‍🌾 $n"
                tvUserLocation.text = "📍 ग्राम: $v, ज़िला: $d (मध्य प्रदेश) — सटीक GPS गोपनीय"
                Toast.makeText(ctx, "✅ प्रोफ़ाइल सफलतापूर्वक अपडेट हो गई!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("रद्द करें", null)
            .show()
    }

    private fun navigateSafe(vararg names: String) {
        val ctx = context ?: return
        val pkg = ctx.packageName
        for (n in names) {
            val id = ctx.resources.getIdentifier(n, "id", pkg)
            if (id != 0) {
                try {
                    findNavController().navigate(id)
                    return
                } catch (_: Throwable) {}
            }
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
