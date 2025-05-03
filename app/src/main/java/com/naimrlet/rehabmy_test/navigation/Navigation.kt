package com.naimrlet.rehabmy_test.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.naimrlet.rehabmy_test.auth.AuthViewModel
import com.naimrlet.rehabmy_test.auth.login.LoginScreen
import com.naimrlet.rehabmy_test.auth.signup.SignUpScreen
import com.naimrlet.rehabmy_test.patient.dashboard.PatientDashboardScreen
import com.naimrlet.rehabmy_test.ui.theme.DarkModeViewModel

sealed class AppDestination(val route: String) {
    data object Login : AppDestination("login")
    data object SignUp : AppDestination("signup")
    data object Dashboard : AppDestination("dashboard")
}

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    darkModeViewModel: DarkModeViewModel,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = if (authViewModel.isAuthenticated)
            AppDestination.Dashboard.route else AppDestination.Login.route
    ) {
        composable(AppDestination.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(AppDestination.Dashboard.route) {
                        popUpTo(AppDestination.Login.route) { inclusive = true }
                    }
                },
                onNavigateToSignUp = { navController.navigate(AppDestination.SignUp.route) }
            )
        }
        composable(AppDestination.SignUp.route) {
            SignUpScreen(
                onSignUpSuccess = {
                    navController.navigate(AppDestination.Dashboard.route) {
                        popUpTo(AppDestination.SignUp.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
        composable(AppDestination.Dashboard.route) {
            PatientDashboardScreen(
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(AppDestination.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                darkModeViewModel = darkModeViewModel
            )
        }
    }
}
