// app/src/main/java/in/merakisan/app/ui/auth/LoginFragment.kt
package `in`.merakisan.app.ui.auth

import android.app.Activity
import android.content.Context
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
import `in`.merakisan.app.core.state.RoleStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
                onAuthenticatedSuccess(account)
            }.onFailure { err ->
                progressBar?.visibility = View.GONE
                Toast.makeText(requireContext(), "लॉगिन त्रुटि: ${err.message}", Toast.LENGTH_LONG).show()
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

        progressBar = findProgressBar(view)
        val btnGoogle = findGoogleSignInButton(view)

        btnGoogle?.setOnClickListener {
            progressBar?.visibility = View.VISIBLE
            googleSignInLauncher.launch(googleAuthManager.getSignInIntent())
        }

        return view
    }

    private fun onAuthenticatedSuccess(account: GoogleSignInAccount) {
        val ctx = context ?: return
        val name = account.displayName ?: "गोविंद पाटीदार"
        val email = account.email ?: ""
        val uid = account.id ?: email.replace(Regex("[^a-zA-Z0-9]"), "_")

        // Master Spec Sec 5 & Gem Rule 7: Never store plaintext PIN; store verified identity session
        ctx.getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE).edit()
            .putString("user_uid", uid)
            .putString("user_name", name)
            .putString("user_email", email)
            .putString("user_village", "गरोठ")
            .putString("user_district", "मंदसौर")
            .putBoolean("is_logged_in", true)
            .apply()

        RoleStateManager.init(ctx)
        syncUserToServerAsync(uid, name, email)

        progressBar?.visibility = View.GONE
        Toast.makeText(ctx, "नमस्ते, $name!", Toast.LENGTH_SHORT).show()
        navigateSafe("roleSelectionFragment", "homeFragment")
    }

    private fun syncUserToServerAsync(uid: String, name: String, email: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://mera-kisan-backend.vercel.app/api/v1/auth/google-sync")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 6000
                conn.readTimeout = 6000

                val payload = JSONObject().apply {
                    put("uid", uid)
                    put("name", name)
                    put("email", email)
                    put("role", RoleStateManager.getCurrentRole().lowercase())
                    put("district", "मंदसौर")
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                conn.responseCode
            } catch (_: Throwable) {}
        }
    }

    private fun findGoogleSignInButton(root: View): View? {
        val ctx = root.context
        val pkg = ctx.packageName
        val ids = arrayOf("btn_google_login", "btnGoogleLogin", "btnGoogle", "googleSignInButton", "btnLogin")
        for (name in ids) {
            val id = ctx.resources.getIdentifier(name, "id", pkg)
            if (id != 0) {
                val found = root.findViewById<View>(id)
                if (found != null) return found
            }
        }
        return scanHierarchy(root)
    }

    private fun scanHierarchy(v: View): View? {
        if (v is Button || (v is TextView && v.text.toString().contains("Google", true))) return v
        if (v is ViewGroup) {
            for (i in 0 until v.childCount) {
                val f = scanHierarchy(v.getChildAt(i))
                if (f != null) return f
            }
        }
        return null
    }

    private fun findProgressBar(root: View): ProgressBar? {
        if (root is ProgressBar) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val f = findProgressBar(root.getChildAt(i))
                if (f != null) return f
            }
        }
        return null
    }

    private fun navigateSafe(vararg destNames: String) {
        val ctx = context ?: return
        val pkg = ctx.packageName
        for (name in destNames) {
            val id = ctx.resources.getIdentifier(name, "id", pkg)
            if (id != 0) {
                try {
                    findNavController().navigate(id)
                    return
                } catch (_: Throwable) {}
            }
        }
    }
}
