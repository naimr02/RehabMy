package com.naimrlet.rehabmy_test.auth.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth

    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var emailError by mutableStateOf("")
    var passwordError by mutableStateOf("")
    var isLoading by mutableStateOf(false)
        private set

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

    fun login(onSuccess: () -> Unit) {
        if (validateEmail() && validatePassword()) {
            // Set loading state BEFORE launching coroutine
            isLoading = true
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                onSuccess()
            } catch(e: Exception) {
                    when (e) {
                        is FirebaseAuthInvalidUserException ->
                            emailError = "Account not found"
                        is FirebaseAuthInvalidCredentialsException ->
                            passwordError = "Invalid password"
                        else ->
                            emailError = "Authentication failed: ${e.message}"
                    }
                } finally {
                    isLoading = false
                }
            }
        }
    }
}
