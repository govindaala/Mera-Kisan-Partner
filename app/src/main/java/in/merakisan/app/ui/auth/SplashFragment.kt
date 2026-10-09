// app/src/main/java/in/merakisan/app/ui/auth/SplashFragment.kt
package in.merakisan.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import in.merakisan.app.R
import in.merakisan.app.core.config.FeatureManager
import in.merakisan.app.core.security.SessionManager
import in.merakisan.app.databinding.FragmentSplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
        initializeApp()
    }

    private fun initializeApp() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.tvStatusMessage.text = "रिमोट कॉन्फ़िगरेशन लोड हो रहा है..."

            // 1. रिमोट कॉन्फ़िगरेशन और 40 फ़ीचर्स का सिंक
            val configResult = FeatureManager.syncRemoteConfig(requireContext())
            
            // ब्रैंडिंग न्यूनतम 1 सेकंड तक प्रदर्शित रखना
            delay(1000)

            // 2. मेंटेनेंस मोड की जाँच
            if (FeatureManager.isMaintenanceMode(requireContext())) {
                binding.tvStatusMessage.text = FeatureManager.getMaintenanceMessage(requireContext())
                Toast.makeText(
                    requireContext(),
                    FeatureManager.getMaintenanceMessage(requireContext()),
                    Toast.LENGTH_LONG
                ).show()
                // मेंटेनेंस मोड सक्रिय होने पर आगे बढ़ने से रोकना
                return@launch
            }

            // 3. नेविगेशन निर्णय (रोल पहले से चयनित है या नया उपयोगकर्ता है)
            val currentRole = SessionManager.getUserRole(requireContext())
            val authToken = SessionManager.getAuthToken(requireContext())

            if (authToken.isNullOrBlank() || currentRole.isBlank()) {
                // प्रथम रन: भूमिका चयन स्क्रीन पर भेजें
                findNavController().navigate(R.id.action_splash_to_roleSelection)
            } else {
                // पंजीकृत उपयोगकर्ता: सीधे मुख्य बाज़ार में प्रवेश
                findNavController().navigate(R.id.action_splash_to_home)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
