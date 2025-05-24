package com.naimrlet.rehabmy_test.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore

    var isAuthenticated by mutableStateOf(auth.currentUser != null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isTherapist by mutableStateOf(false)
        private set

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val newAuthState = firebaseAuth.currentUser != null
            isAuthenticated = newAuthState
            
            if (newAuthState) {
                checkUserRole()
            } else {
                isTherapist = false
            }
        }
        
        // Check role at startup if already logged in
        if (isAuthenticated) {
            checkUserRole()
        }
    }

    private fun checkUserRole() {
        val userId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            try {
                val document = firestore.collection("users").document(userId).get().await()
                isTherapist = document.getBoolean("isTherapist") ?: false
            } catch (e: Exception) {
                // Default to patient if there's an error
                isTherapist = false
            }
        }
    }

    suspend fun loginWithEmail(email: String, password: String): Result<Unit> {
        return try {
            isLoading = true
            auth.signInWithEmailAndPassword(email, password).await()
            // Role will be checked via the auth state listener
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
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            
            // Create user document with default role (patient)
            result.user?.uid?.let { userId ->
                firestore.collection("users").document(userId)
                    .set(mapOf("isTherapist" to false))
                    .await()
            }
            
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
        isAuthenticated = false
        isTherapist = false
    }
}
