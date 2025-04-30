package com.naimrlet.rehabmy_test

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
            AppTheme(darkTheme = darkModeViewModel.isDarkThemeEnabled) {
                AppNavigation(
                    authViewModel = authViewModel,
                    darkModeViewModel = darkModeViewModel
                )
            }
        }
    }
}
