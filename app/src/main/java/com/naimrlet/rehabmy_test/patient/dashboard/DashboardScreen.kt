package com.naimrlet.rehabmy_test.patient.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import com.naimrlet.rehabmy_test.patient.therapist.TherapistScreen
import com.naimrlet.rehabmy_test.ui.theme.DarkModeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDashboardScreen(
    onLogout: () -> Unit,
    darkModeViewModel: DarkModeViewModel = viewModel()
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val dashboardSections = listOf(
        DashboardSection.Therapist,
        DashboardSection.Dashboard,
        DashboardSection.About
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.8f)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Appearance",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

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
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } }
                        ) {
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

                    dashboardSections.forEach { section ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (currentDestination?.hierarchy?.any {
                                            it.route == section.route
                                        } == true) {
                                        section.selectedIcon
                                    } else {
                                        section.unselectedIcon
                                    },
                                    contentDescription = section.title
                                )
                            },
                            label = { Text(section.title) },
                            selected = currentDestination?.hierarchy?.any {
                                it.route == section.route
                            } == true,
                            onClick = {
                                navController.navigate(section.route) {
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
                startDestination = DashboardSection.Dashboard.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(DashboardSection.Therapist.route) { TherapistScreen() }
                composable(DashboardSection.Dashboard.route) { DashboardHomeScreen() }
                composable(DashboardSection.About.route) { AboutScreen() }
            }
        }
    }
}

sealed class DashboardSection(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Therapist : DashboardSection(
        route = "therapist",
        title = "Therapist",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )

    data object Dashboard : DashboardSection(
        route = "dashboard",
        title = "Dashboard",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object About : DashboardSection(
        route = "about",
        title = "About",
        selectedIcon = Icons.Filled.Info,
        unselectedIcon = Icons.Outlined.Info
    )
}
