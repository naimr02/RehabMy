package com.naimrlet.rehabmy_test.auth.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class LoginViewModel : ViewModel() {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var emailError by mutableStateOf("")
    var passwordError by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var isLoggedIn by mutableStateOf(false)

    fun onEmailChange(newEmail: String) {
        email = newEmail
        emailError = ""  // Clear error when typing
    }

    fun onPasswordChange(newPassword: String) {
        password = newPassword
        passwordError = ""  // Clear error when typing
    }

    private fun validateEmail(): Boolean {
        return if (email.isBlank()) {
            emailError = "Email cannot be empty"
            false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "Enter a valid email address"
            false
        } else {
            emailError = ""
            true
        }
    }

    private fun validatePassword(): Boolean {
        return if (password.isBlank()) {
            passwordError = "Password cannot be empty"
            false
        } else if (password.length < 6) {
            passwordError = "Password must be at least 6 characters"
            false
        } else {
            passwordError = ""
            true
        }
    }

    fun login() {
        if (validateEmail() && validatePassword()) {
            isLoading = true
            // In a real app, this would call a repository or service
            // For this example, we'll simulate a network delay
            android.os.Handler().postDelayed({
                isLoading = false
                isLoggedIn = true
            }, 1500)
        }
    }
}
