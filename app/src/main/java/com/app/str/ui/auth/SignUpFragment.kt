package com.app.str.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.app.str.R
import com.app.str.data.model.Result
import com.app.str.data.model.SignUpRequest
import com.app.str.databinding.FragmentSignUpBinding
import com.app.str.utils.LoadingDialog
import com.app.str.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUpFragment : Fragment() {
    
    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var loadingDialog: LoadingDialog
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        loadingDialog = LoadingDialog(requireContext())
        
        setupObservers()
        setupClickListeners()
    }
    
    private fun setupObservers() {
        viewModel.signUpResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    loadingDialog.show("Creating your account...")
                    binding.btnSignUp.isEnabled = false
                }
                is Result.Success -> {
                    loadingDialog.updateMessage("Account created!")
                    binding.btnSignUp.isEnabled = true
                    
                    // Delay to show success message
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        loadingDialog.dismiss()
                        // Navigate to OTP screen with email as argument
                        val email = binding.etEmail.text.toString().trim()
                        val action = SignUpFragmentDirections.actionSignUpToOtp(email)
                        findNavController().navigate(action)
                    }, 500)
                }
                is Result.Error -> {
                    loadingDialog.dismiss()
                    binding.btnSignUp.isEnabled = true
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.btnSignUp.setOnClickListener {
            // Store first and last name for profile completion later
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = password // In our new UI, we don't have a separate confirm password field
            
            if (validateInput(email, password)) {
                val request = SignUpRequest(email, password, confirmPassword)
                viewModel.signUp(request)
            }
        }
        
        binding.tvLogin.setOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun validateInput(
        email: String,
        password: String
    ): Boolean {
        return when {
            email.isEmpty() -> {
                Toast.makeText(context, "Email is required", Toast.LENGTH_SHORT).show()
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                Toast.makeText(context, "Invalid email format", Toast.LENGTH_SHORT).show()
                false
            }
            password.isEmpty() -> {
                Toast.makeText(context, "Password is required", Toast.LENGTH_SHORT).show()
                false
            }
            password.length < 6 -> {
                Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        loadingDialog.destroy()
        _binding = null
    }
}