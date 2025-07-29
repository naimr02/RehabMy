package com.naimrlet.rehabmy_test.auth.signup

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.naimrlet.rehabmy_test.auth.components.*
import com.naimrlet.rehabmy_test.ui.theme.DarkModeViewModel

@Composable
fun SignUpScreen(
    userType: UserType,
    onSignUpSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    darkModeViewModel: DarkModeViewModel,
    viewModel: SignUpViewModel = viewModel()
) {
    // Set the user type when the screen loads
    LaunchedEffect(userType) {
        viewModel.setUserType(userType)
    }

    if (viewModel.isSignedUp) {
        onSignUpSuccess()
        return
    }

    AuthScaffold(
        title = "Create Account",
        subtitle = if (userType == UserType.PATIENT)
            "Join as a patient"
        else
            "Join as a physiotherapist",
        darkModeViewModel = darkModeViewModel
    ) {
        // Full name field
        NameField(
            value = viewModel.name,
            onValueChange = viewModel::onNameChange,
            error = viewModel.nameError
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Date of birth field
        DateOfBirthField(
            selectedDate = viewModel.dateOfBirth,
            onDateSelected = viewModel::onDateOfBirthChange,
            error = viewModel.dateOfBirthError
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Condition field (only for patients)
        if (viewModel.userType == UserType.PATIENT) {
            ConditionField(
                value = viewModel.condition,
                onValueChange = viewModel::onConditionChange,
                error = viewModel.conditionError
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Email field
        EmailField(
            value = viewModel.email,
            onValueChange = viewModel::onEmailChange,
            error = viewModel.emailError
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password fields
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

        // Sign up button
        AuthButton(
            text = "CREATE ACCOUNT",
            isLoading = viewModel.isLoading,
            onClick = { viewModel.signUp(onSuccess = onSignUpSuccess) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Navigation to login
        AuthToggleRow(
            question = "Already have an account?",
            actionText = "Sign In",
            onActionClick = onNavigateToLogin
        )
    }
}
