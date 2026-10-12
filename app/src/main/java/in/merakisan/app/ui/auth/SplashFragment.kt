// app/src/main/java/in/merakisan/app/ui/auth/SplashFragment.kt
package `in`.merakisan.app.ui.auth

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import `in`.merakisan.app.R
import `in`.merakisan.app.core.security.SessionManager
import `in`.merakisan.app.databinding.FragmentSplashBinding

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthAndNavigate()
        }, 1200)
    }

    private fun checkAuthAndNavigate() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val sessionManager = SessionManager(requireContext())
        val hasLocalSession = sessionManager.getUserId().isNotBlank()

        if (currentUser != null && hasLocalSession) {
            // यूज़र पहले से सत्यापित रूप से लॉग इन है -> सीधा मेन ऐप में
            try {
                findNavController().navigate(R.id.action_splash_to_main)
            } catch (_: Exception) {
                findNavController().navigate(R.id.mainActivity)
            }
        } else {
            // नया यूज़र या अन-ऑथेंटिकेटेड -> सीधा LoginFragment (Google Login स्क्रीन)
            try {
                findNavController().navigate(R.id.action_splash_to_login)
            } catch (_: Exception) {
                findNavController().navigate(R.id.loginFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
