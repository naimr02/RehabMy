package com.naimrlet.rehabmy_test

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.naimrlet.rehabmy_test.auth.AuthViewModel
import com.naimrlet.rehabmy_test.navigation.AppNavigation
import com.naimrlet.rehabmy_test.ui.theme.DarkModeViewModel
import com.naimrlet.rehabmy_test.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val darkModeViewModel: DarkModeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val darkTheme = darkModeViewModel.isDarkThemeEnabled

            // Apply status bar color based on current theme
            StatusBarColorEffect(darkTheme)

            AppTheme(darkTheme = darkTheme) {
                AppNavigation(
                    authViewModel = authViewModel,
                    darkModeViewModel = darkModeViewModel
                )
            }
        }
    }
}

@Composable
private fun StatusBarColorEffect(darkTheme: Boolean) {
    val systemUiController = rememberSystemUiController()
    DisposableEffect(systemUiController, darkTheme) {
        systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = !darkTheme
        )
        onDispose {}
    }
}
