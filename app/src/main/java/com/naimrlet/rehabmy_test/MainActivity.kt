package com.naimrlet.rehabmy_test

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.naimrlet.rehabmy_test.auth.AuthViewModel
import com.naimrlet.rehabmy_test.navigation.AppNavigation
import com.naimrlet.rehabmy_test.ui.theme.AppTheme
import com.naimrlet.rehabmy_test.ui.theme.DarkModeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val darkModeViewModel: DarkModeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge design
        enableEdgeToEdge()
        
        setContent {
            val darkTheme = darkModeViewModel.isDarkThemeEnabled

            AppTheme(darkTheme = darkTheme) {
                // Configure system bars visibility within the theme
                ConfigureSystemBars(darkTheme)
                
                AppNavigation(
                    authViewModel = authViewModel,
                    darkModeViewModel = darkModeViewModel
                )
            }
        }
    }
}

@Composable
private fun ConfigureSystemBars(darkTheme: Boolean) {
    // Capture the LocalView.current within the composable context
    val view = LocalView.current
    
    DisposableEffect(darkTheme) {
        // Use the captured view reference
        val window = view.context.getActivity()?.window
            ?: return@DisposableEffect onDispose {}
        
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
        
        onDispose {}
    }
}

// Extension function to get the activity from a context
private fun android.content.Context.getActivity(): ComponentActivity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is ComponentActivity) {
            return context
        }
        context = context.baseContext
    }
    return null
}

