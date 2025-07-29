package com.naimrlet.rehabmy_test.auth.usertype

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.naimrlet.rehabmy_test.auth.components.*
import com.naimrlet.rehabmy_test.ui.theme.DarkModeViewModel

@Composable
fun UserTypeSelectionScreen(
    onUserTypeSelected: (UserType) -> Unit,
    onNavigateBack: () -> Unit,
    darkModeViewModel: DarkModeViewModel
) {
    var selectedUserType by remember { mutableStateOf<UserType?>(null) }

    AuthScaffold(
        title = "Choose Your Role",
        subtitle = "Select how you'll be using the app",
        darkModeViewModel = darkModeViewModel
    ) {
        // Main content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Description text
            Text(
                text = "Your role determines the features and interface you'll see in the app.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // User type selection cards
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                UserTypeCard(
                    userType = UserType.PATIENT,
                    isSelected = selectedUserType == UserType.PATIENT,
                    onClick = { selectedUserType = UserType.PATIENT },
                    modifier = Modifier.fillMaxWidth()
                )

                UserTypeCard(
                    userType = UserType.THERAPIST,
                    isSelected = selectedUserType == UserType.THERAPIST,
                    onClick = { selectedUserType = UserType.THERAPIST },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Continue button
            AuthButton(
                text = "CONTINUE",
                isLoading = false,
                onClick = {
                    selectedUserType?.let { userType ->
                        onUserTypeSelected(userType)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Back to login option
            AuthToggleRow(
                question = "Already have an account?",
                actionText = "Sign In",
                onActionClick = onNavigateBack
            )
        }
    }
}

@Composable
private fun EnhancedUserTypeCard(
    userType: UserType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
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
                .padding(24.dp)
                .clickable { onClick() },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            androidx.compose.material3.Icon(
                imageVector = if (userType == UserType.PATIENT)
                    Icons.Default.Person
                else
                    Icons.Default.LocalHospital,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = if (userType == UserType.PATIENT) "Patient" else "Physiotherapist",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = if (userType == UserType.PATIENT)
                    "I'm seeking rehabilitation therapy and want to track my progress"
                else
                    "I'm a healthcare professional providing physiotherapy services",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Features list
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                val features = if (userType == UserType.PATIENT) {
                    listOf(
                        "Track exercise progress",
                        "Chat with your therapist",
                        "Monitor your condition",
                        "Receive exercise assignments"
                    )
                } else {
                    listOf(
                        "Manage patient assignments",
                        "Create exercise programs",
                        "Monitor patient progress",
                        "Communicate with patients"
                    )
                }

                features.forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Select button
            if (isSelected) {
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Selected")
                }
            } else {
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select")
                }
            }
        }
    }
}
