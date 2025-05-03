package com.naimrlet.rehabmy_test.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth

    var isAuthenticated by mutableStateOf(auth.currentUser != null)
        private set
    var isLoading by mutableStateOf(false)
        private set

    init {
        auth.addAuthStateListener { firebaseAuth ->
            isAuthenticated = firebaseAuth.currentUser != null
        }
    }

    suspend fun loginWithEmail(email: String, password: String): Result<Unit> {
        return try {
            isLoading = true
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            val errorMessage = when (e) {
                is FirebaseAuthInvalidUserException -> "Account not found"
                is FirebaseAuthInvalidCredentialsException -> "Invalid email or password"
                else -> "Authentication failed: ${e.message}"
            }
            Result.failure(Exception(errorMessage))
        } finally {
            isLoading = false
        }
    }

    suspend fun signUpWithEmail(email: String, password: String): Result<Unit> {
        return try {
            isLoading = true
            auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            val errorMessage = when (e) {
                is FirebaseAuthWeakPasswordException -> "Password too weak (min 6 chars)"
                is FirebaseAuthUserCollisionException -> "Email already registered"
                is FirebaseAuthInvalidCredentialsException -> "Invalid email format"
                else -> "Registration failed: ${e.message}"
            }
            Result.failure(Exception(errorMessage))
        } finally {
            isLoading = false
        }
    }

    fun logout() {
        auth.signOut()
    }
}
