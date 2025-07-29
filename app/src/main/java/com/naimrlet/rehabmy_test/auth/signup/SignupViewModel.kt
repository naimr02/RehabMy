package com.naimrlet.rehabmy_test.auth.signup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naimrlet.rehabmy_test.auth.AuthViewModel
import com.naimrlet.rehabmy_test.auth.components.UserType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SignUpViewModel(
    private val authViewModel: AuthViewModel = AuthViewModel()
) : ViewModel() {
    // User type will be passed from the previous screen
    private var _userType by mutableStateOf(UserType.PATIENT)
    val userType: UserType get() = _userType

    // Basic fields
    var name by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    var dateOfBirth by mutableStateOf("")

    // Patient-specific fields
    var condition by mutableStateOf("")

    // Error states
    var nameError by mutableStateOf("")
    var emailError by mutableStateOf("")
    var passwordError by mutableStateOf("")
    var confirmPasswordError by mutableStateOf("")
    var dateOfBirthError by mutableStateOf("")
    var conditionError by mutableStateOf("")

    val isLoading get() = authViewModel.isLoading
    var isSignedUp by mutableStateOf(false)

    fun setUserType(selectedUserType: UserType) {
        _userType = selectedUserType
        // Clear condition when switching to therapist
        if (selectedUserType == UserType.THERAPIST) {
            condition = ""
            conditionError = ""
        }
    }

    fun onNameChange(newName: String) {
        name = newName
        nameError = ""
    }

    fun onEmailChange(newEmail: String) {
        email = newEmail
        emailError = ""
    }

    fun onPasswordChange(newPassword: String) {
        password = newPassword
        passwordError = ""
    }

    fun onConfirmPasswordChange(newPassword: String) {
        confirmPassword = newPassword
        confirmPasswordError = ""
    }

    fun onDateOfBirthChange(newDate: String) {
        dateOfBirth = newDate
        dateOfBirthError = ""
    }

    fun onConditionChange(newCondition: String) {
        condition = newCondition
        conditionError = ""
    }

    private fun validateInput(): Boolean {
        var isValid = true

        // Validate name
        if (name.isBlank()) {
            nameError = "Name cannot be empty"
            isValid = false
        }

        // Validate email
        if (email.isBlank()) {
            emailError = "Email cannot be empty"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "Enter a valid email address"
            isValid = false
        }

        // Validate password
        if (password.isBlank()) {
            passwordError = "Password cannot be empty"
            isValid = false
        } else if (password.length < 6) {
            passwordError = "Password must be at least 6 characters"
            isValid = false
        }

        // Validate confirm password
        if (confirmPassword != password) {
            confirmPasswordError = "Passwords do not match"
            isValid = false
        }

        // Validate date of birth
        if (dateOfBirth.isBlank()) {
            dateOfBirthError = "Date of birth is required"
            isValid = false
        }

        // Validate condition for patients
        if (_userType == UserType.PATIENT && condition.isBlank()) {
            conditionError = "Medical condition is required for patients"
            isValid = false
        }

        return isValid
    }

    private fun calculateAge(dateOfBirth: String): Int {
        return try {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val birthDate = dateFormat.parse(dateOfBirth)
            val today = Calendar.getInstance()
            val birth = Calendar.getInstance()
            birth.time = birthDate

            var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)

            // Check if birthday has occurred this year
            if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
                age--
            }

            age
        } catch (e: Exception) {
            0 // Default age if parsing fails
        }
    }

    fun signUp(onSuccess: () -> Unit) {
        if (!validateInput()) return

        viewModelScope.launch {
            val age = calculateAge(dateOfBirth)
            val isTherapist = _userType == UserType.THERAPIST
            val medicalCondition = if (isTherapist) null else condition

            authViewModel.signUpWithEmailAndProfile(
                email = email,
                password = password,
                name = name,
                age = age,
                isTherapist = isTherapist,
                condition = medicalCondition
            )
                .onSuccess {
                    isSignedUp = true
                    onSuccess()
                }
                .onFailure { error ->
                    when {
                        error.message?.contains("password", ignoreCase = true) == true ->
                            passwordError = error.message ?: "Password issue"
                        error.message?.contains("email", ignoreCase = true) == true ->
                            emailError = error.message ?: "Email issue"
                        else ->
                            emailError = error.message ?: "Registration failed"
                    }
                }
        }
    }
}
