package com.naimrlet.rehabmy_test.auth.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naimrlet.rehabmy_test.auth.AuthViewModel
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authViewModel: AuthViewModel = AuthViewModel()
) : ViewModel() {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var emailError by mutableStateOf("")
    var passwordError by mutableStateOf("")

    val isLoading get() = authViewModel.isLoading

    fun onEmailChange(newEmail: String) {
        email = newEmail
        emailError = ""
    }

    fun onPasswordChange(newPassword: String) {
        password = newPassword
        passwordError = ""
    }

    private fun validateInput(): Boolean {
        var isValid = true

        if (email.isBlank()) {
            emailError = "Email cannot be empty"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "Enter a valid email address"
            isValid = false
        }

        if (password.isBlank()) {
            passwordError = "Password cannot be empty"
            isValid = false
        } else if (password.length < 6) {
            passwordError = "Password must be at least 6 characters"
            isValid = false
        }

        return isValid
    }

    fun login(onSuccess: () -> Unit) {
        if (!validateInput()) return

        viewModelScope.launch {
            authViewModel.loginWithEmail(email, password)
                .onSuccess { onSuccess() }
                .onFailure { error ->
                    if (error.message?.contains("password", ignoreCase = true) == true) {
                        passwordError = error.message ?: "Invalid password"
                    } else {
                        emailError = error.message ?: "Login failed"
                    }
                }
        }
    }
}
