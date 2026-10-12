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
            if (isAdded) {
                checkAuthAndNavigate()
            }
        }, 1200)
    }

    private fun checkAuthAndNavigate() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val sessionManager = SessionManager(requireContext())
        val hasLocalSession = sessionManager.getUserId().isNotBlank()

        if (currentUser != null && hasLocalSession) {
            // User pehle se authenticated hai -> homeFragment ya roleSelectionFragment par safe navigation
            val homeId = resources.getIdentifier("homeFragment", "id", requireContext().packageName)
            val roleId = resources.getIdentifier("roleSelectionFragment", "id", requireContext().packageName)
            val targetId = if (homeId != 0) homeId else roleId
            if (targetId != 0) {
                try {
                    findNavController().navigate(targetId)
                } catch (_: Exception) {}
            }
        } else {
            // User authenticated nahi hai -> seedha loginFragment par navigate karein
            val loginId = resources.getIdentifier("loginFragment", "id", requireContext().packageName)
            if (loginId != 0) {
                try {
                    findNavController().navigate(loginId)
                } catch (_: Exception) {}
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
