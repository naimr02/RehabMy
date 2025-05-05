package com.naimrlet.rehabmy_test.auth.signup

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.naimrlet.rehabmy_test.auth.components.*
import com.naimrlet.rehabmy_test.ui.theme.DarkModeViewModel

@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    darkModeViewModel: DarkModeViewModel,
    viewModel: SignUpViewModel = viewModel()
) {
    if (viewModel.isSignedUp) {
        onSignUpSuccess()
        return
    }

    AuthScaffold(
        title = "Create Account",
        subtitle = "Join our community",
        darkModeViewModel = darkModeViewModel
    ) {
        NameField(
            value = viewModel.name,
            onValueChange = viewModel::onNameChange,
            error = viewModel.nameError
        )

        Spacer(modifier = Modifier.height(16.dp))

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
            label = "Password",
            imeAction = ImeAction.Next
        )

        Spacer(modifier = Modifier.height(16.dp))

        PasswordField(
            value = viewModel.confirmPassword,
            onValueChange = viewModel::onConfirmPasswordChange,
            error = viewModel.confirmPasswordError,
            label = "Confirm Password"
        )

        Spacer(modifier = Modifier.height(24.dp))

        AuthButton(
            text = "SIGN UP",
            isLoading = viewModel.isLoading,
            onClick = { viewModel.signUp(onSuccess = onSignUpSuccess) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        AuthToggleRow(
            question = "Already have an account?",
            actionText = "Login",
            onActionClick = onNavigateToLogin
        )
    }
}
