package com.naimrlet.rehabmy_test.auth.signup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naimrlet.rehabmy_test.auth.AuthViewModel
import kotlinx.coroutines.launch

class SignUpViewModel(
    private val authViewModel: AuthViewModel = AuthViewModel()
) : ViewModel() {
    var name by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")

    var nameError by mutableStateOf("")
    var emailError by mutableStateOf("")
    var passwordError by mutableStateOf("")
    var confirmPasswordError by mutableStateOf("")

    val isLoading get() = authViewModel.isLoading
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

    private fun validateInput(): Boolean {
        var isValid = true

        if (name.isBlank()) {
            nameError = "Name cannot be empty"
            isValid = false
        }

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

        if (confirmPassword != password) {
            confirmPasswordError = "Passwords do not match"
            isValid = false
        }

        return isValid
    }

    fun signUp(onSuccess: () -> Unit) {
        if (!validateInput()) return

        viewModelScope.launch {
            authViewModel.signUpWithEmail(email, password)
                .onSuccess {
                    isSignedUp = true
                    onSuccess()
                }
                .onFailure { error ->
                    when {
                        error.message?.contains("password", ignoreCase = true) == true ->
                            passwordError = error.message ?: "Password issue"
                        error.message?.contains("email", ignoreCase = true) == true ->
                            emailError = error.message ?: "Email issue"
                        else ->
                            emailError = error.message ?: "Registration failed"
                    }
                }
        }
    }
}
