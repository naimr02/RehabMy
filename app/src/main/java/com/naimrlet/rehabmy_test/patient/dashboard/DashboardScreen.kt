package com.naimrlet.rehabmy_test.patient.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.naimrlet.rehabmy_test.R
import com.naimrlet.rehabmy_test.patient.about.AboutScreen
import com.naimrlet.rehabmy_test.patient.dashboard.home.DashboardHomeScreen
import com.naimrlet.rehabmy_test.patient.library.LibraryScreen
import com.naimrlet.rehabmy_test.patient.therapist.TherapistScreen
import com.naimrlet.rehabmy_test.ui.theme.DarkModeViewModel
import kotlinx.coroutines.launch

sealed class DashboardDestination(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Therapist : DashboardDestination(
        route = "therapist",
        title = "Therapist",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
    data object Home : DashboardDestination(
        route = "home",
        title = "Dashboard",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )
    data object Library : DashboardDestination(
        route = "library",
        title = "Library",
        selectedIcon = Icons.Filled.Book,
        unselectedIcon = Icons.Outlined.Book
    )
    data object Progress : DashboardDestination(
        route = "about",
        title = "Progress",
        selectedIcon = Icons.AutoMirrored.Filled.TrendingUp,
        unselectedIcon = Icons.AutoMirrored.Outlined.TrendingUp
    )
    companion object {
        val items = listOf(Therapist, Home, Library, Progress)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDashboardScreen(
    onLogout: () -> Unit,
    darkModeViewModel: DarkModeViewModel = viewModel()
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.8f)) {
                AppearanceDrawerContent(darkModeViewModel = darkModeViewModel)
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open settings menu"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onLogout) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Logout"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    DashboardDestination.items.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == destination.route
                        } == true

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) destination.selectedIcon
                                    else destination.unselectedIcon,
                                    contentDescription = destination.title
                                )
                            },
                            label = { Text(destination.title) },
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = DashboardDestination.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(DashboardDestination.Therapist.route) { TherapistScreen() }
                composable(DashboardDestination.Home.route) { DashboardHomeScreen() }
                composable(DashboardDestination.Library.route) { LibraryScreen() }
                composable(DashboardDestination.Progress.route) { AboutScreen() }
            }
        }
    }
}

@Composable
private fun AppearanceDrawerContent(darkModeViewModel: DarkModeViewModel) {
    val context = LocalContext.current
    var showCopiedMessage by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Copy UID Section
        OutlinedButton(
            onClick = {
                val currentUser = FirebaseAuth.getInstance().currentUser
                currentUser?.uid?.let { uid ->
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Firebase UID", uid)
                    clipboard.setPrimaryClip(clip)
                    showCopiedMessage = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ContentCopy,
                contentDescription = "Copy UID",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Copy UID")
        }

        // Show copied message
        if (showCopiedMessage) {
            Text(
                text = "UID copied to clipboard!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            LaunchedEffect(showCopiedMessage) {
                kotlinx.coroutines.delay(2000)
                showCopiedMessage = false
            }
        }

        // Dark Theme Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Dark Theme",
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = darkModeViewModel.isDarkThemeEnabled,
                onCheckedChange = { darkModeViewModel.toggleTheme() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        TextButton(
            onClick = { /* Add additional settings here */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Privacy Policy")
        }
    }
}
