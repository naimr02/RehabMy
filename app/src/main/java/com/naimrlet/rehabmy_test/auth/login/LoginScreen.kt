package com.naimrlet.rehabmy_test.auth.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.naimrlet.rehabmy_test.auth.components.*
import com.naimrlet.rehabmy_test.ui.theme.DarkModeViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    darkModeViewModel: DarkModeViewModel,
    viewModel: LoginViewModel = viewModel()
) {
    AuthScaffold(
        title = "Welcome",
        subtitle = "Sign in to continue",
        darkModeViewModel = darkModeViewModel
    ) {
        EmailField(
            value = viewModel.email,
            onValueChange = viewModel::onEmailChange,
            error = viewModel.emailError
        )

        Spacer(modifier = Modifier.height(16.dp))

        PasswordField(
            value = viewModel.password,
            onValueChange = viewModel::onPasswordChange,
            error = viewModel.passwordError,
            label = "Password"
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            TextButton(
                onClick = { /* Handle forgot password */ },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Text("Forgot Password?")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AuthButton(
            text = "LOGIN",
            isLoading = viewModel.isLoading,
            onClick = { viewModel.login(onSuccess = onLoginSuccess) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        AuthToggleRow(
            question = "Don't have an account?",
            actionText = "Sign Up",
            onActionClick = onNavigateToSignUp
        )
    }
}
