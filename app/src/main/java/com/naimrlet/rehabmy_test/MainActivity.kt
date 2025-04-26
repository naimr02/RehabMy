package com.naimrlet.rehabmy_test

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.naimrlet.rehabmy_test.auth.login.LoginScreen
import com.naimrlet.rehabmy_test.auth.signup.SignUpScreen
import com.naimrlet.rehabmy_test.patient.dashboard.DashboardScreen
import com.naimrlet.rehabmy_test.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }

    Surface(modifier = modifier.fillMaxSize()) {
        when (currentScreen) {
            is Screen.Login -> LoginScreen(
                onLoginSuccess = { currentScreen = Screen.Dashboard },
                onNavigateToSignUp = { currentScreen = Screen.SignUp }
            )
            is Screen.SignUp -> SignUpScreen(
                onSignUpSuccess = { currentScreen = Screen.Dashboard },
                onNavigateToLogin = { currentScreen = Screen.Login }
            )
            is Screen.Dashboard -> DashboardScreen(
                onLogout = {
                    currentScreen = Screen.Login
                    // Reset ViewModels if needed
                }
            )
        }
    }
}

sealed class Screen {
    data object Login : Screen()
    data object SignUp : Screen()
    data object Dashboard : Screen()
}