package com.app.str.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.app.str.DashboardActivity
import com.app.str.ProfileCompleteActivity
import com.app.str.data.model.LoginRequest
import com.app.str.data.model.LoginNavigationResult
import com.app.str.data.model.Result
import com.app.str.databinding.FragmentLoginBinding
import com.app.str.utils.AuthState
import com.app.str.utils.NavigationEvent
import com.app.str.utils.LoadingDialog
import com.app.str.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {
    
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var loadingDialog: LoadingDialog
    
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
        
        loadingDialog = LoadingDialog(requireContext())
        
        setupObservers()
        setupClickListeners()
    }
    
    private fun setupObservers() {
        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                loadingDialog.show("Logging in...")
            } else {
                loadingDialog.dismiss()
            }
            binding.btnLogin.isEnabled = !isLoading
        }
        
        viewModel.loginNavigationResult.observe(viewLifecycleOwner) { result ->
            println("LoginFragment: Login navigation result received: $result")
            when (result) {
                is Result.Loading -> {
                    println("LoginFragment: Login loading state")
                    // Check if login was successful and now we're checking profile
                    if (viewModel.loginResult.value is Result.Success) {
                        loadingDialog.show("Checking profile status...")
                    } else {
                        loadingDialog.show("Logging in...")
                    }
                }
                is Result.Success -> {
                    val navigationData = result.data
                    println("LoginFragment: Login successful - profile complete: ${navigationData.profileComplete}")
                    loadingDialog.updateMessage("Login successful!")
                    Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                    
                    // Delay to show success message
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        loadingDialog.dismiss()
                        
                        if (navigationData.shouldNavigateToDashboard) {
                            // Profile completed - navigate to dashboard
                            println("LoginFragment: Profile is complete, navigating to dashboard")
                            try {
                                navigateToDashboard()
                            } catch (e: Exception) {
                                println("LoginFragment: Navigation failed, trying alternative method")
                                alternativeNavigation()
                            }
                        } else {
                            // Profile not completed - navigate to profile complete screen
                            println("LoginFragment: Profile is incomplete, navigating to profile completion")
                            try {
                                navigateToProfileComplete()
                            } catch (e: Exception) {
                                println("LoginFragment: Profile complete navigation failed")
                                alternativeProfileNavigation()
                            }
                        }
                    }, 500)
                }
                is Result.Error -> {
                    println("LoginFragment: Login error: ${result.message}")
                    loadingDialog.dismiss()
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
        
        // Note: Removing navigation event observers to prevent conflicts
        // Navigation will be handled directly in loginResult observer
    }
    
    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            println("LoginFragment: Login button clicked")
            // In our updated UI, the etEmail field actually contains a phone number
            // But since the API expects an email, we'll use the field as email
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            
            println("LoginFragment: Email: $email, Password length: ${password.length}")
            
            if (validateInput(email, password)) {
                println("LoginFragment: Validation passed, calling login")
                val request = LoginRequest(email, password)
                viewModel.login(request)
            } else {
                println("LoginFragment: Validation failed")
            }
        }
        
        binding.tvSignUp.setOnClickListener {
            val action = LoginFragmentDirections.actionLoginToSignUp()
            findNavController().navigate(action)
        }
        
        // Forgot Password click listener
        binding.tvForgotPassword.setOnClickListener {
            navigateToForgotPassword()
        }
        
        // Since we have a LinearLayout with two buttons for Google and Facebook
        // We'll set click listeners for both buttons

    }
    
    private fun navigateToForgotPassword() {
        try {
            val intent = Intent(requireContext(), ForgotPasswordActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            println("LoginFragment: Error navigating to forgot password: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun validateInput(email: String, password: String): Boolean {
        return when {
            email.isEmpty() -> {
                binding.etEmail.error = "Email is required"
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.etEmail.error = "Invalid email format"
                false
            }
            password.isEmpty() -> {
                binding.etPassword.error = "Password is required"
                false
            }
            password.length < 6 -> {
                binding.etPassword.error = "Password must be at least 6 characters"
                false
            }
            else -> true
        }
    }
    
    private fun navigateToDashboard() {
        println("LoginFragment: Starting navigation to DashboardActivity")
        try {
            val intent = Intent(requireContext(), DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
            println("LoginFragment: Navigation to DashboardActivity completed")
        } catch (e: Exception) {
            println("LoginFragment: Error navigating to dashboard: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun navigateToProfileComplete() {
        println("LoginFragment: Starting navigation to ProfileCompleteActivity")
        try {
            val intent = Intent(requireContext(), ProfileCompleteActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
            println("LoginFragment: Navigation to ProfileCompleteActivity completed")
        } catch (e: Exception) {
            println("LoginFragment: Error navigating to profile complete: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun alternativeProfileNavigation() {
        println("LoginFragment: Using alternative navigation method for profile complete")
        try {
            val context = activity ?: requireContext()
            val intent = Intent(context, ProfileCompleteActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
            
            activity?.let {
                it.finishAffinity()
            }
            println("LoginFragment: Alternative profile navigation succeeded")
        } catch (e: Exception) {
            println("LoginFragment: Alternative profile navigation failed: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun handleAuthState(authState: AuthState) {
        when (authState) {
            AuthState.AUTHENTICATED -> {
                // User is authenticated, navigation will be handled by NavigationEvent
            }
            AuthState.TOKEN_EXPIRED -> {
                // Token expired, show message or handle refresh
                Toast.makeText(context, "Session expired, please login again", Toast.LENGTH_SHORT).show()
            }
            AuthState.LOGGED_OUT -> {
                // User is logged out, ensure UI is ready for login
            }
            AuthState.ERROR -> {
                Toast.makeText(context, "Authentication error occurred", Toast.LENGTH_SHORT).show()
            }
            AuthState.UNKNOWN -> {
                // Initial state, no action needed
            }
        }
    }
    
    private fun alternativeNavigation() {
        println("LoginFragment: Using alternative navigation method")
        try {
            // Use activity's context directly
            val context = activity ?: requireContext()
            val intent = Intent(context, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
            
            // Finish activity in a safer way
            if (activity != null) {
                activity?.finish()
            }
            println("LoginFragment: Alternative navigation completed")
        } catch (e: Exception) {
            println("LoginFragment: Alternative navigation also failed: ${e.message}")
            e.printStackTrace()
            // Last resort - show message to user
            Toast.makeText(context, "Login successful! Please restart the app.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loadingDialog.destroy()
        _binding = null
    }
}