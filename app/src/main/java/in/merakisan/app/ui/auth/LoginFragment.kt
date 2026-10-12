// app/src/main/java/in/merakisan/app/ui/auth/LoginFragment.kt
package `in`.merakisan.app.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import `in`.merakisan.app.R
import `in`.merakisan.app.databinding.FragmentLoginBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                viewModel.firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                Toast.makeText(context, "Google sign-in fail hua: ${e.statusCode}", Toast.LENGTH_SHORT).show()
                viewModel.resetError()
            }
        } else {
            Toast.makeText(context, "Sign-in cancel kar diya gaya", Toast.LENGTH_SHORT).show()
            viewModel.resetError()
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

        setupGoogleSignInClient()
        setupListeners()
        observeAuthState()
    }

    private fun setupGoogleSignInClient() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    private fun setupListeners() {
        findView("btnGoogleSignIn")?.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent: Intent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }

        findView("btnContinueLogin")?.setOnClickListener {
            viewModel.continueAsGuest()
        }
    }

    private fun observeAuthState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    when (state) {
                        is AuthUiState.Loading -> {
                            findView("pbLoginLoading")?.isVisible = true
                        }
                        is AuthUiState.GoogleLinked -> {
                            findView("pbLoginLoading")?.isVisible = false
                            val roleId = resources.getIdentifier("roleSelectionFragment", "id", requireContext().packageName)
                            if (roleId != 0) {
                                try {
                                    findNavController().navigate(roleId)
                                } catch (_: Exception) {}
                            }
                        }
                        is AuthUiState.Success -> {
                            findView("pbLoginLoading")?.isVisible = false
                            Toast.makeText(requireContext(), "Swagat hai, ${state.name}!", Toast.LENGTH_SHORT).show()
                            val homeId = resources.getIdentifier("homeFragment", "id", requireContext().packageName)
                            val roleId = resources.getIdentifier("roleSelectionFragment", "id", requireContext().packageName)
                            val targetId = if (homeId != 0) homeId else roleId
                            if (targetId != 0) {
                                try {
                                    findNavController().navigate(targetId)
                                } catch (_: Exception) {}
                            }
                        }
                        is AuthUiState.Error -> {
                            findView("pbLoginLoading")?.isVisible = false
                            if (state.message.isNotBlank()) {
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                            }
                        }
                        else -> {
                            findView("pbLoginLoading")?.isVisible = false
                        }
                    }
                }
            }
        }
    }

    private fun findView(name: String): View? {
        val id = resources.getIdentifier(name, "id", requireContext().packageName)
        return if (id != 0) binding.root.findViewById(id) else null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
