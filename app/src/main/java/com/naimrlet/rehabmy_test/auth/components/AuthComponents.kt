package com.naimrlet.rehabmy_test.auth.components

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.naimrlet.rehabmy_test.ui.theme.DarkModeViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AuthScaffold(
    title: String,
    subtitle: String,
    darkModeViewModel: DarkModeViewModel,
    content: @Composable ColumnScope.() -> Unit
) {
    // Wrap everything in a Surface to properly apply the background color
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Theme toggle button in the top right
            ThemeToggleButton(
                darkModeViewModel = darkModeViewModel,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )

            // Main content
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(32.dp))
                content()
            }
        }
    }
}

@Composable
fun ThemeToggleButton(
    darkModeViewModel: DarkModeViewModel,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = { darkModeViewModel.toggleTheme() },
        modifier = modifier
    ) {
        Icon(
            imageVector = if (darkModeViewModel.isDarkThemeEnabled)
                Icons.Default.LightMode else Icons.Default.DarkMode,
            contentDescription = "Toggle theme",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun EmailField(value: String, onValueChange: (String) -> Unit, error: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Email") },
        isError = error.isNotEmpty(),
        supportingText = {
            if (error.isNotEmpty()) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        ),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    error: String,
    label: String,
    imeAction: ImeAction = ImeAction.Done
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error.isNotEmpty(),
        supportingText = {
            if (error.isNotEmpty()) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction
        ),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
fun NameField(value: String, onValueChange: (String) -> Unit, error: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Full Name") },
        isError = error.isNotEmpty(),
        supportingText = {
            if (error.isNotEmpty()) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
fun ConditionField(value: String, onValueChange: (String) -> Unit, error: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Medical Condition") },
        isError = error.isNotEmpty(),
        supportingText = {
            if (error.isNotEmpty()) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
fun UserTypeSelector(
    selectedType: UserType,
    onTypeChange: (UserType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "I am registering as:",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            UserTypeCard(
                userType = UserType.PATIENT,
                isSelected = selectedType == UserType.PATIENT,
                onClick = { onTypeChange(UserType.PATIENT) },
                modifier = Modifier.weight(1f)
            )

            UserTypeCard(
                userType = UserType.THERAPIST,
                isSelected = selectedType == UserType.THERAPIST,
                onClick = { onTypeChange(UserType.THERAPIST) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun UserTypeCard(
    userType: UserType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (userType == UserType.PATIENT)
                    Icons.Default.Person
                else
                    Icons.Default.LocalHospital,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (userType == UserType.PATIENT) "Patient" else "Physiotherapist",
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (userType == UserType.PATIENT)
                    "Seeking rehabilitation"
                else
                    "Providing therapy",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun DateOfBirthField(
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    error: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    OutlinedTextField(
        value = selectedDate,
        onValueChange = { },
        label = { Text("Date of Birth") },
        isError = error.isNotEmpty(),
        supportingText = {
            if (error.isNotEmpty()) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        trailingIcon = {
            IconButton(
                onClick = {
                    showDatePicker(context) { date ->
                        onDateSelected(date)
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Select date"
                )
            }
        },
        readOnly = true,
        modifier = modifier.fillMaxWidth(),
        singleLine = true
    )
}

private fun showDatePicker(context: Context, onDateSelected: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    val datePickerDialog = DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDay ->
            val date = Calendar.getInstance()
            date.set(selectedYear, selectedMonth, selectedDay)
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            onDateSelected(dateFormat.format(date.time))
        },
        year,
        month,
        day
    )

    // Set maximum date to today (can't be born in the future)
    datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

    // Set minimum date to reasonable age limit (e.g., 120 years ago)
    val minCalendar = Calendar.getInstance()
    minCalendar.add(Calendar.YEAR, -120)
    datePickerDialog.datePicker.minDate = minCalendar.timeInMillis

    datePickerDialog.show()
}

enum class UserType {
    PATIENT,
    THERAPIST
}

@Composable
fun AuthButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier.fillMaxWidth()
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(text)
        }
    }
}

@Composable
fun AuthToggleRow(
    question: String,
    actionText: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = question,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        TextButton(onClick = onActionClick) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
