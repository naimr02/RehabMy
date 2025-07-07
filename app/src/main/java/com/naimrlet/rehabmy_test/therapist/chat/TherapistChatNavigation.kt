package com.naimrlet.rehabmy_test.therapist.chat

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation

/**
 * Constants for therapist chat navigation routes
 */
object TherapistChatDestinations {
    const val THERAPIST_CHAT_ROUTE = "therapist_chat"
    const val THERAPIST_CHAT_NESTED_ROUTE = "therapist_chat_nested"
    const val PATIENT_CHAT_LIST_ROUTE = "patient_chat_list"
    const val PATIENT_CHAT_DETAIL_ROUTE = "patient_chat_detail/{patientId}"
    
    // Helper function to create patient chat detail route with actual ID
    fun patientChatDetailRoute(patientId: String) = "patient_chat_detail/$patientId"
}

/**
 * Add therapist chat screen to navigation graph
 */
fun NavGraphBuilder.therapistChatGraph(
    navController: NavHostController,
    onNavigateBack: () -> Unit
) {
    // Create the main entry point to the chat feature
    composable(
        route = TherapistChatDestinations.THERAPIST_CHAT_ROUTE,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        }
    ) {
        // Use Hilt ViewModel instead of Factory
        val viewModel: TherapistChatViewModel = hiltViewModel()

        TherapistChatScreen(
            onBackClick = onNavigateBack
        )
    }
    
    // Nested navigation graph for direct navigation to specific patient chat
    navigation(
        startDestination = TherapistChatDestinations.PATIENT_CHAT_LIST_ROUTE,
        route = TherapistChatDestinations.THERAPIST_CHAT_NESTED_ROUTE
    ) {
        // Patient chat list
        composable(
            route = TherapistChatDestinations.PATIENT_CHAT_LIST_ROUTE
        ) {
            // Use Hilt ViewModel instead of Factory
            val viewModel: TherapistChatViewModel = hiltViewModel()

            TherapistChatScreen(
                onBackClick = onNavigateBack
            )
        }
        
        // Patient chat detail with specific patient ID
        composable(
            route = TherapistChatDestinations.PATIENT_CHAT_DETAIL_ROUTE,
            arguments = listOf(
                navArgument("patientId") { type = NavType.StringType }
            ),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) { backStackEntry ->
            val patientId = backStackEntry.arguments?.getString("patientId") ?: ""
            // Use Hilt ViewModel instead of Factory
            val viewModel: TherapistChatViewModel = hiltViewModel()

            // Auto-select the patient when navigating directly to chat
            LaunchPatientChat(
                patientId = patientId,
                viewModel = viewModel
            )
            
            TherapistChatScreen(
                onBackClick = onNavigateBack
            )
        }
    }
}

/**
 * Helper composable to automatically select a patient when navigating directly to chat
 */
@Composable
private fun LaunchPatientChat(patientId: String, viewModel: TherapistChatViewModel) {
    androidx.compose.runtime.LaunchedEffect(patientId) {
        if (patientId.isNotEmpty()) {
            viewModel.selectPatient(patientId)
        }
    }
}

/**
 * Class containing navigation actions for therapist chat
 */
class TherapistChatNavigationActions(navController: NavHostController) {
    val navigateToTherapistChat: () -> Unit = {
        navController.navigate(TherapistChatDestinations.THERAPIST_CHAT_ROUTE) {
            // Pop up to the start destination of the graph to avoid building up a large stack
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            // Avoid multiple copies of the same destination when reselecting the same item
            launchSingleTop = true
            // Restore state when reselecting a previously selected item
            restoreState = true
        }
    }
    
    val navigateToPatientChat: (String) -> Unit = { patientId ->
        navController.navigate(TherapistChatDestinations.patientChatDetailRoute(patientId)) {
            // Avoid multiple copies of the same destination
            launchSingleTop = true
        }
    }
    
    val navigateUp: () -> Unit = {
        navController.navigateUp()
    }
}

/**
 * Create and remember therapist chat navigation actions
 */
@Composable
fun rememberTherapistChatNavigationActions(
    navController: NavHostController
): TherapistChatNavigationActions {
    return remember(navController) {
        TherapistChatNavigationActions(navController)
    }
}
