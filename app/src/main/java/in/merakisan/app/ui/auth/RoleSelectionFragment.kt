// app/src/main/java/in/merakisan/app/ui/auth/RoleSelectionFragment.kt
package in.merakisan.app.ui.auth

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import in.merakisan.app.R
import in.merakisan.app.core.security.SessionManager
import in.merakisan.app.databinding.FragmentRoleSelectionBinding

class RoleSelectionFragment : Fragment() {

    private var _binding: FragmentRoleSelectionBinding? = null
    private val binding get() = _binding!!

    // चयनित भूमिका: "FARMER", "BUYER", अथवा "BOTH"
    private var selectedRole: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoleSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCardClicks()
        setupContinueButton()
    }

    private fun setupCardClicks() {
        binding.cardFarmer.setOnClickListener {
            selectRole("FARMER")
        }

        binding.cardBuyer.setOnClickListener {
            selectRole("BUYER")
        }

        binding.cardBoth.setOnClickListener {
            selectRole("BOTH")
        }
    }

    private fun selectRole(role: String) {
        selectedRole = role
        binding.btnContinue.isEnabled = true

        val activeColor = Color.parseColor("#1B5E20")
        val defaultColor = Color.parseColor("#C8E6C9")

        // कार्ड स्ट्रोक और बैकग्राउंड हाइलाइट्स को अपडेट करना
        binding.cardFarmer.strokeColor = if (role == "FARMER") activeColor else defaultColor
        binding.cardFarmer.strokeWidth = if (role == "FARMER") 4 else 2

        binding.cardBuyer.strokeColor = if (role == "BUYER") activeColor else defaultColor
        binding.cardBuyer.strokeWidth = if (role == "BUYER") 4 else 2

        binding.cardBoth.strokeColor = if (role == "BOTH") activeColor else defaultColor
        binding.cardBoth.strokeWidth = if (role == "BOTH") 4 else 2
    }

    private fun setupContinueButton() {
        binding.btnContinue.setOnClickListener {
            val role = selectedRole ?: return@setOnClickListener

            // चयनित भूमिका को एन्क्रिप्टेड स्टोरेज में सुरक्षित सहेजना
            val currentToken = SessionManager.getAuthToken(requireContext()) ?: "guest_session"
            val currentUid = SessionManager.getUserUid(requireContext()) ?: "temp_uid"

            SessionManager.saveSession(
                context = requireContext(),
                token = currentToken,
                uid = currentUid,
                role = role
            )

            // मुख्य बाज़ार/होम स्क्रीन पर जाना
            findNavController().navigate(R.id.action_roleSelection_to_home)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
