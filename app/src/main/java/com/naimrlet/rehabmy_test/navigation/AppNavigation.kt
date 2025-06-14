package com.naimrlet.rehabmy_test.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.naimrlet.rehabmy_test.BuildConfig // Import BuildConfig
import com.naimrlet.rehabmy_test.auth.AuthViewModel
import com.naimrlet.rehabmy_test.auth.login.LoginScreen
import com.naimrlet.rehabmy_test.auth.signup.SignUpScreen
import com.naimrlet.rehabmy_test.patient.dashboard.PatientDashboardScreen
import com.naimrlet.rehabmy_test.therapist.TherapistScreen
import com.naimrlet.rehabmy_test.ui.theme.DarkModeViewModel
import android.util.Log // Add this import

object AppRoute {
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val HOME = "home"
    const val THERAPIST = "therapist"
}

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    darkModeViewModel: DarkModeViewModel
) {
    val navController = rememberNavController()
    val startDestination by remember { mutableStateOf(determineStartDestination(authViewModel)) }
    
    // Effect to handle role changes
    LaunchedEffect(authViewModel.isAuthenticated, authViewModel.isTherapist) {
        if (authViewModel.isAuthenticated) {
            val destination = if (authViewModel.isTherapist) AppRoute.THERAPIST else AppRoute.HOME
            navController.navigate(destination) {
                popUpTo(0) { inclusive = true }
            }
        } else if (!authViewModel.isAuthenticated && navController.currentBackStackEntry?.destination?.route != AppRoute.LOGIN) {
            navController.navigate(AppRoute.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
    
    // Debug the BuildConfig API key directly
    LaunchedEffect(Unit) {
        Log.d("AppNavigation", "BuildConfig.GEMINI_API_KEY length: ${BuildConfig.GEMINI_API_KEY.length}")
        Log.d("AppNavigation", "First few chars of API key: ${BuildConfig.GEMINI_API_KEY.take(5)}...")
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(AppRoute.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    // Navigation handled by LaunchedEffect
                },
                onNavigateToSignUp = { navController.navigate(AppRoute.SIGNUP) },
                darkModeViewModel = darkModeViewModel
            )
        }
        
        composable(AppRoute.SIGNUP) {
            SignUpScreen(
                onSignUpSuccess = {
                    // Navigation handled by LaunchedEffect
                },
                onNavigateToLogin = { navController.navigate(AppRoute.LOGIN) },
                darkModeViewModel = darkModeViewModel
            )
        }
        
        composable(AppRoute.HOME) {
            PatientDashboardScreen(
                onLogout = {
                    authViewModel.logout()
                },
                darkModeViewModel = darkModeViewModel
            )
        }
        
        composable(AppRoute.THERAPIST) {
            // Use hardcoded API key as fallback if BuildConfig value is empty
            val apiKey = if (BuildConfig.GEMINI_API_KEY.isEmpty()) {
                Log.w("AppNavigation", "Using hardcoded API key as BuildConfig value is empty")
                "AIzaSyBzb3cUDw5eLpf1CxTvS2wDx2ypsvewaxQ" // From your local.properties
            } else {
                BuildConfig.GEMINI_API_KEY
            }
            
            TherapistScreen(
                navController = navController,
                onLogout = {
                    authViewModel.logout()
                },
                geminiApiKey = apiKey // Use the fallback if needed
            )
        }
    }
}

private fun determineStartDestination(authViewModel: AuthViewModel): String {
    return when {
        !authViewModel.isAuthenticated -> AppRoute.LOGIN
        authViewModel.isTherapist -> AppRoute.THERAPIST
        else -> AppRoute.HOME
    }
}

