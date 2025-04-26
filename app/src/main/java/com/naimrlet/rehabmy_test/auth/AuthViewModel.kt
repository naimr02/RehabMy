package com.naimrlet.rehabmy_test.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    var isAuthenticated by mutableStateOf(auth.currentUser != null)
        private set

    init {
        // Listen for auth state changes
        auth.addAuthStateListener { firebaseAuth ->
            isAuthenticated = firebaseAuth.currentUser != null
        }
    }

    fun login() {
        // No direct state change - let Firebase auth update trigger the state
    }

    fun logout() {
        auth.signOut()
    }
}