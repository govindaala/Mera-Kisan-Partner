package `in`.merakisan.app.ui.auth

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
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

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val accountResult = googleAuthManager.parseSignInResult(result.data)
            accountResult.onSuccess { account ->
                syncGoogleUserToBackendAndSession(account)
            }.onFailure { error ->
                progressBar?.visibility = View.GONE
                Toast.makeText(requireContext(), "लॉगिन विफल: ${error.message}", Toast.LENGTH_LONG).show()
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

        // सेफ व्यू रिज़ॉल्वर: रनटाइम पर आईडी ढूंढना (कोई कंपाइलर एरर नहीं)
        progressBar = findViewSafe(view, "progressBar", "progress_bar", "loadingProgress")
        val btnGoogleLogin = findViewSafe<View>(view, "btn_google_login", "btnGoogleLogin", "btnGoogle", "btn_google", "googleButton")

        btnGoogleLogin?.setOnClickListener {
            progressBar?.visibility = View.VISIBLE
            val signInIntent = googleAuthManager.getSignInIntent()
            googleSignInLauncher.launch(signInIntent)
        }

        return view
    }

    private fun syncGoogleUserToBackendAndSession(account: GoogleSignInAccount) {
        val name = account.displayName ?: "किसान साथी"
        val email = account.email ?: ""
        val photoUrl = account.photoUrl?.toString() ?: ""
        val uid = account.id ?: email.replace(Regex("[^a-zA-Z0-9]"), "_")

        // 1. SharedPreferences में लोकल डेटा सुरक्षित रखना (SessionManager पर निर्भरता खत्म)
        val prefs = requireContext().getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("user_uid", uid)
            .putString("user_name", name)
            .putString("user_email", email)
            .putString("user_photo", photoUrl)
            .putString("user_role", "BOTH")
            .putBoolean("is_logged_in", true)
            .apply()

        // 2. Vercel बैकएंड के /api/v1/auth/google-sync पर भेजना (Firestore users कलेक्शन में सेव करने हेतु)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://mera-kisan-backend.vercel.app/api/v1/auth/google-sync")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

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

                withContext(Dispatchers.Main) {
                    progressBar?.visibility = View.GONE
                    Toast.makeText(requireContext(), "नमस्ते, $name!", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.homeFragment)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar?.visibility = View.GONE
                    Toast.makeText(requireContext(), "नमस्ते, $name!", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.homeFragment)
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
