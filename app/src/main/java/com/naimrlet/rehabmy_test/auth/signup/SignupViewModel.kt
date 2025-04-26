package com.naimrlet.rehabmy_test.auth.signup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class SignUpViewModel : ViewModel() {
    var name by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")

    var nameError by mutableStateOf("")
    var emailError by mutableStateOf("")
    var passwordError by mutableStateOf("")
    var confirmPasswordError by mutableStateOf("")

    var isLoading by mutableStateOf(false)
    var isSignedUp by mutableStateOf(false)

    fun onNameChange(newName: String) {
        name = newName
        nameError = ""
    }

    fun onEmailChange(newEmail: String) {
        email = newEmail
        emailError = ""
    }

    fun onPasswordChange(newPassword: String) {
        password = newPassword
        passwordError = ""
    }

    fun onConfirmPasswordChange(newPassword: String) {
        confirmPassword = newPassword
        confirmPasswordError = ""
    }

    private fun validateName(): Boolean {
        return if (name.isBlank()) {
            nameError = "Name cannot be empty"
            false
        } else {
            true
        }
    }

    private fun validateEmail(): Boolean {
        return if (email.isBlank()) {
            emailError = "Email cannot be empty"
            false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "Enter a valid email address"
            false
        } else {
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
            true
        }
    }

    private fun validateConfirmPassword(): Boolean {
        return if (confirmPassword != password) {
            confirmPasswordError = "Passwords do not match"
            false
        } else {
            true
        }
    }

    fun signUp() {
        val isValidName = validateName()
        val isValidEmail = validateEmail()
        val isValidPassword = validatePassword()
        val isValidConfirmPassword = validateConfirmPassword()

        if (isValidName && isValidEmail && isValidPassword && isValidConfirmPassword) {
            isLoading = true
            // Simulate network call
            android.os.Handler().postDelayed({
                isLoading = false
                isSignedUp = true
            }, 1500)
        }
    }
}
