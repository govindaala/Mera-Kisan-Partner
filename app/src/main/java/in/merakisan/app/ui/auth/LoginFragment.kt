package `in`.merakisan.app.ui.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import `in`.merakisan.app.R
import `in`.merakisan.app.core.security.GoogleAuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class LoginFragment : Fragment() {

    private lateinit var googleAuthManager: GoogleAuthManager
    private var progressBar: ProgressBar? = null
    private var btnGoogleLogin: View? = null

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val accountResult = googleAuthManager.parseSignInResult(result.data)
            accountResult.onSuccess { account ->
                handleLoginSuccess(account)
            }.onFailure { error ->
                progressBar?.visibility = View.GONE
                Toast.makeText(requireContext(), "Google साइन इन विफल: ${error.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            progressBar?.visibility = View.GONE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)
        googleAuthManager = GoogleAuthManager(requireContext())

        // 1. प्रोग्रेस बार रिज़ॉल्व करना
        progressBar = findProgressBar(view)

        // 2. Google लॉगिन बटन रिज़ॉल्व करना (डीप स्कैनर द्वारा 100% बाइंडिंग)
        btnGoogleLogin = findGoogleSignInButton(view)

        btnGoogleLogin?.setOnClickListener {
            progressBar?.visibility = View.VISIBLE
            try {
                val signInIntent = googleAuthManager.getSignInIntent()
                googleSignInLauncher.launch(signInIntent)
            } catch (e: Exception) {
                progressBar?.visibility = View.GONE
                Toast.makeText(requireContext(), "लॉगिन शुरू नहीं हो सका: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. यदि डिवाइस में पहले से Google खाता मौजूद है, तो डेटा स्वतः सिंक करना
        checkAndSyncExistingAccount()

        return view
    }

    override fun onResume() {
        super.onResume()
        checkAndSyncExistingAccount()
    }

    private fun checkAndSyncExistingAccount() {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(requireContext())
            if (account != null && !account.email.isNullOrEmpty()) {
                saveUserDataLocally(account)
                syncWithBackendAsync(account)
            }
        } catch (_: Throwable) {}
    }

    private fun handleLoginSuccess(account: GoogleSignInAccount) {
        val name = account.displayName ?: "किसान साथी"
        
        // 1. स्थानीय स्टोरेज में तुरंत सेव करना
        saveUserDataLocally(account)

        // 2. बैकएंड को बैकग्राउंड में अपडेट करना (UI को रोके बिना)
        syncWithBackendAsync(account)

        progressBar?.visibility = View.GONE
        Toast.makeText(requireContext(), "नमस्ते, $name!", Toast.LENGTH_SHORT).show()

        // 3. अगली स्क्रीन पर नेविगेट करना
        navigateToNextScreen()
    }

    private fun saveUserDataLocally(account: GoogleSignInAccount) {
        val context = context ?: return
        val name = account.displayName ?: "किसान साथी"
        val email = account.email ?: ""
        val photoUrl = account.photoUrl?.toString() ?: ""
        val uid = account.id ?: email.replace(Regex("[^a-zA-Z0-9]"), "_")

        // 'mera_kisan_user' फ़ाइल में लिखना
        val userPrefs = context.getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)
        userPrefs.edit().apply {
            putString("user_uid", uid)
            putString("uid", uid)
            putString("user_name", name)
            putString("name", name)
            putString("displayName", name)
            putString("user_email", email)
            putString("email", email)
            putString("user_photo", photoUrl)
            putString("photo_url", photoUrl)
            putString("user_role", "BOTH")
            putString("role", "BOTH")
            putString("user_district", "मध्य प्रदेश")
            putBoolean("is_logged_in", true)
            apply()
        }

        // 'mera_kisan_session' फ़ाइल में लिखना
        val sessionPrefs = context.getSharedPreferences("mera_kisan_session", Context.MODE_PRIVATE)
        sessionPrefs.edit().apply {
            putString("user_uid", uid)
            putString("user_name", name)
            putString("user_email", email)
            putString("user_role", "BOTH")
            putBoolean("is_logged_in", true)
            apply()
        }

        // डिफ़ॉल्ट पैकेज प्रेफरेंसेस में लिखना
        val defaultPrefs = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
        defaultPrefs.edit().apply {
            putString("user_uid", uid)
            putString("user_name", name)
            putString("user_email", email)
            putString("user_role", "BOTH")
            putBoolean("is_logged_in", true)
            apply()
        }
    }

    private fun syncWithBackendAsync(account: GoogleSignInAccount) {
        val name = account.displayName ?: "किसान साथी"
        val email = account.email ?: ""
        val photoUrl = account.photoUrl?.toString() ?: ""
        val uid = account.id ?: email.replace(Regex("[^a-zA-Z0-9]"), "_")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://mera-kisan-backend.vercel.app/api/v1/auth/google-sync")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 8000
                conn.readTimeout = 8000

                val payload = JSONObject().apply {
                    put("uid", uid)
                    put("name", name)
                    put("email", email)
                    put("photo_url", photoUrl)
                    put("role", "both")
                    put("district", "मध्य प्रदेश")
                }

                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                conn.responseCode
            } catch (_: Exception) {}
        }
    }

    private fun navigateToNextScreen() {
        val context = context ?: return
        val pkg = context.packageName

        // 1. पहले होम स्क्रीन पर जाने का प्रयास
        val homeId = context.resources.getIdentifier("homeFragment", "id", pkg)
        if (homeId != 0) {
            try {
                findNavController().navigate(homeId)
                return
            } catch (_: Throwable) {}
        }

        // 2. फॉलबैक: रोल सेलेक्शन स्क्रीन
        val roleId = context.resources.getIdentifier("roleSelectionFragment", "id", pkg)
        if (roleId != 0) {
            try {
                findNavController().navigate(roleId)
                return
            } catch (_: Throwable) {}
        }

        try {
            findNavController().popBackStack()
        } catch (_: Throwable) {}
    }

    private fun findGoogleSignInButton(root: View): View? {
        val context = root.context
        val pkg = context.packageName

        // 1. मानक डायरेक्ट IDs
        val directIds = arrayOf(
            "btn_google_login", "btnGoogleLogin", "btnGoogleSignIn", "btn_google_sign_in",
            "googleSignInButton", "btnGoogle", "signInButton", "btn_google", "googleButton",
            "btnLogin", "btnLoginGoogle", "cardGoogle", "cardGoogleLogin", "layoutGoogle",
            "btn_sign_in", "btnSignIn", "loginButton"
        )
        for (name in directIds) {
            val resId = context.resources.getIdentifier(name, "id", pkg)
            if (resId != 0) {
                val v = root.findViewById<View>(resId)
                if (v != null) return v
            }
        }

        // 2. लेआउट ट्री का डीप-स्कैन (Google टेक्स्ट, क्लास या टैग के आधार पर)
        return scanViewHierarchyForGoogle(root)
    }

    private fun scanViewHierarchyForGoogle(view: View): View? {
        val className = view.javaClass.name.lowercase()
        if (className.contains("signinbutton")) {
            return view
        }

        val idName = try {
            if (view.id != View.NO_ID) view.resources.getResourceEntryName(view.id).lowercase() else ""
        } catch (_: Throwable) { "" }

        if (idName.contains("google") && (view.isClickable || view is ViewGroup || view is Button)) {
            return view
        }

        if (view is TextView) {
            val text = view.text?.toString()?.lowercase() ?: ""
            if (text.contains("google") || text.contains("साइन इन") || text.contains("लॉगिन")) {
                val parent = view.parent
                if (parent is View && parent.isClickable) return parent
                return view
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val found = scanViewHierarchyForGoogle(view.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }

    private fun findProgressBar(root: View): ProgressBar? {
        val context = root.context
        val pkg = context.packageName
        val names = arrayOf("progressBar", "progress_bar", "loadingProgress", "loginProgressBar")
        for (name in names) {
            val resId = context.resources.getIdentifier(name, "id", pkg)
            if (resId != 0) {
                val pb = root.findViewById<ProgressBar>(resId)
                if (pb != null) return pb
            }
        }
        return null
    }
}
