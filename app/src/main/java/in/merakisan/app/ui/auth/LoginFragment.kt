// app/src/main/java/in/merakisan/app/ui/auth/LoginFragment.kt
package in.merakisan.app.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import in.merakisan.app.R
import in.merakisan.app.core.security.GoogleAuthManager
import in.merakisan.app.core.security.SessionManager
import in.merakisan.app.databinding.FragmentLoginBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()
    private lateinit var googleAuthManager: GoogleAuthManager

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val parseResult = googleAuthManager.parseSignInResult(result.data)
            if (parseResult.isSuccess) {
                val account = parseResult.getOrNull()
                if (account != null) {
                    viewModel.onGoogleSignInSuccess(account)
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    parseResult.exceptionOrNull()?.message ?: "Google साइन-इन विफल",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        googleAuthManager = GoogleAuthManager(requireContext())

        setupActions()
        observeViewModel()
    }

    private fun setupActions() {
        binding.btnGoogleSignIn.setOnClickListener {
            val intent = googleAuthManager.getSignInIntent()
            googleSignInLauncher.launch(intent)
        }

        binding.btnContinueLogin.setOnClickListener {
            val phone = binding.etMobileNumber.text.toString().trim()
            viewModel.finalizeAuthentication(phone)
        }

        binding.tvSkipLogin.setOnClickListener {
            viewModel.continueAsGuest()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is AuthUiState.Idle -> {
                        binding.pbLoginLoading.visibility = View.GONE
                        binding.btnContinueLogin.isEnabled = true
                    }
                    is AuthUiState.Loading -> {
                        binding.pbLoginLoading.visibility = View.VISIBLE
                        binding.btnContinueLogin.isEnabled = false
                    }
                    is AuthUiState.GoogleLinked -> {
                        binding.pbLoginLoading.visibility = View.GONE
                        binding.btnContinueLogin.isEnabled = true
                        binding.llGoogleProfileSummary.visibility = View.VISIBLE
                        binding.tvGoogleUserName.text = "सत्यापित: ${state.account.displayName}"
                        binding.tvGoogleUserEmail.text = state.account.email
                        Toast.makeText(requireContext(), "Google खाता लिंक हो गया!", Toast.LENGTH_SHORT).show()
                    }
                    is AuthUiState.Success -> {
                        binding.pbLoginLoading.visibility = View.GONE
                        Toast.makeText(requireContext(), "स्वागत है, ${state.name}!", Toast.LENGTH_SHORT).show()

                        // भूमिका चयन स्क्रीन पर आगे बढ़ें (यदि पहली बार है)
                        val role = SessionManager.getUserRole(requireContext())
                        if (role.isBlank()) {
                            findNavController().navigate(R.id.action_login_to_roleSelection)
                        } else {
                            findNavController().navigate(R.id.action_login_to_home)
                        }
                    }
                    is AuthUiState.Error -> {
                        binding.pbLoginLoading.visibility = View.GONE
                        binding.btnContinueLogin.isEnabled = true
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetError()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
