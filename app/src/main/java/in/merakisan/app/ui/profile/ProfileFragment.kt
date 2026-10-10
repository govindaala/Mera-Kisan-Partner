// app/src/main/java/in/merakisan/app/ui/profile/ProfileFragment.kt
package in.merakisan.app.ui.profile

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import in.merakisan.app.R
import in.merakisan.app.databinding.FragmentProfileBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRoleRadioGroup()
        setupShortcuts()
        setupLogoutButton()
        observeProfileData()
    }

    private fun setupRoleRadioGroup() {
        binding.rgRoleSwitch.setOnCheckedChangeListener { _, checkedId ->
            val targetRole = when (checkedId) {
                R.id.rbRoleFarmer -> "FARMER"
                R.id.rbRoleBuyer -> "BUYER"
                R.id.rbRoleBoth -> "BOTH"
                else -> return@setOnCheckedChangeListener
            }

            val current = viewModel.userProfile.value?.currentRole
            if (current != targetRole) {
                viewModel.updateRole(targetRole)
                Toast.makeText(
                    requireContext(),
                    "भूमिका बदलकर '$targetRole' मोड कर दी गई।",
                    Toast.LENGTH_SHORT
                ).show()

                // मुख्य Activity को नेविगेशन मेन्यू दोबारा अडैप्ट करने के लिए रीक्रिएट या रीसेट संकेत देना
                requireActivity().recreate()
            }
        }
    }

    private fun setupShortcuts() {
        binding.rowMyOrders.setOnClickListener {
            findNavController().navigate(R.id.nav_orders)
        }

        binding.rowMyProduce.setOnClickListener {
            findNavController().navigate(R.id.nav_marketplace)
        }

        binding.rowMyRequests.setOnClickListener {
            findNavController().navigate(R.id.nav_buyer_requests)
        }

        binding.rowLanguage.setOnClickListener {
            showLanguageDialog()
        }
    }

    private fun setupLogoutButton() {
        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("लॉगआउट")
                .setMessage("क्या आप सच में वर्तमान सत्र से बाहर निकलना चाहते हैं?")
                .setPositiveButton("हाँ, लॉगआउट करें") { _, _ ->
                    viewModel.logout()
                    Toast.makeText(requireContext(), "सत्र समाप्त हो गया।", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.roleSelectionFragment)
                }
                .setNegativeButton("रद्द करें", null)
                .show()
        }
    }

    private fun observeProfileData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.userProfile.collectLatest { profile ->
                profile ?: return@collectLatest

                binding.tvUserName.text = profile.name
                binding.tvUserPhone.text = profile.phone
                binding.tvAvatarInitial.text = profile.name.take(1)
                binding.tvUserLocation.text = "📍 ${profile.village}, ${profile.district} (मध्य प्रदेश)"

                // रेडियो बटन को सक्रिय भूमिका पर सेट करना
                when (profile.currentRole) {
                    "FARMER" -> binding.rbRoleFarmer.isChecked = true
                    "BUYER" -> binding.rbRoleBuyer.isChecked = true
                    "BOTH" -> binding.rbRoleBoth.isChecked = true
                }

                // सत्यापन बैज
                when (profile.verificationStatus) {
                    "verified_farmer" -> {
                        binding.tvUserVerificationBadge.visibility = View.VISIBLE
                        binding.tvUserVerificationBadge.text = "✔ Mera Kisan Verified Farmer"
                    }
                    "organic_certified" -> {
                        binding.tvUserVerificationBadge.visibility = View.VISIBLE
                        binding.tvUserVerificationBadge.text = "🌿 Organic Certified"
                    }
                    else -> {
                        binding.tvUserVerificationBadge.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("हिंदी (Hindi)", "English")
        AlertDialog.Builder(requireContext())
            .setTitle("भाषा चुनें (Select Language)")
            .setSingleChoiceItems(languages, 0) { dialog, which ->
                binding.tvCurrentLanguage.text = if (which == 0) "हिंदी" else "English"
                dialog.dismiss()
                Toast.makeText(requireContext(), "भाषा अद्यतित की गई", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("रद्द करें", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
