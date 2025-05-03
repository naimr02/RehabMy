package com.naimrlet.rehabmy_test.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.naimrlet.rehabmy_test.auth.AuthViewModel
import com.naimrlet.rehabmy_test.auth.login.LoginScreen
import com.naimrlet.rehabmy_test.auth.signup.SignUpScreen
import com.naimrlet.rehabmy_test.patient.dashboard.PatientDashboardScreen
import com.naimrlet.rehabmy_test.ui.theme.DarkModeViewModel

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    darkModeViewModel: DarkModeViewModel
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = if (authViewModel.isAuthenticated) "dashboard" else "login"
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    // Navigate directly on success instead of calling non-existent method
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToSignUp = { navController.navigate("signup") }
            )
        }

        composable("signup") {
            SignUpScreen(
                onSignUpSuccess = {
                    // Navigate directly on success instead of calling non-existent method
                    navController.navigate("dashboard") {
                        popUpTo("signup") { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable("dashboard") {
            PatientDashboardScreen(
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                darkModeViewModel = darkModeViewModel
            )
        }
    }
}
