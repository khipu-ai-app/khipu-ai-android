package pe.khipuai.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import pe.khipuai.app.ui.screens.auth.LoginScreen
import pe.khipuai.app.ui.screens.auth.RegisterScreen
import pe.khipuai.app.ui.screens.home.HomeScreen
import pe.khipuai.app.ui.screens.capture.CaptureScreen
import pe.khipuai.app.ui.screens.planner.PlannerScreen
import pe.khipuai.app.ui.screens.maps.MapsScreen
import pe.khipuai.app.ui.screens.profile.ProfileScreen
import pe.khipuai.app.ui.screens.processing.ProcessingScreen

@Composable
fun KhipuNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToTab = { tabIndex ->
                    when (tabIndex) {
                        0 -> { /* Already on Home */ }
                        1 -> navController.navigate(Screen.Capture.route)
                        2 -> navController.navigate(Screen.Planner.route)
                        3 -> navController.navigate(Screen.Maps.route)
                        4 -> navController.navigate(Screen.Profile.route)
                    }
                }
            )
        }
        
        composable(Screen.Capture.route) {
            CaptureScreen(
                onNavigateToTab = { tabIndex ->
                    when (tabIndex) {
                        0 -> navController.navigate(Screen.Home.route)
                        1 -> { /* Already on Capture */ }
                        2 -> navController.navigate(Screen.Planner.route)
                        3 -> navController.navigate(Screen.Maps.route)
                        4 -> navController.navigate(Screen.Profile.route)
                    }
                },
                onNavigateToProcessing = {
                    navController.navigate(Screen.Processing.route)
                }
            )
        }
        
        composable(Screen.Planner.route) {
            PlannerScreen(
                onNavigateToTab = { tabIndex ->
                    when (tabIndex) {
                        0 -> navController.navigate(Screen.Home.route)
                        1 -> navController.navigate(Screen.Capture.route)
                        2 -> { /* Already on Planner */ }
                        3 -> navController.navigate(Screen.Maps.route)
                        4 -> navController.navigate(Screen.Profile.route)
                    }
                }
            )
        }
        
        composable(Screen.Maps.route) {
            MapsScreen(
                onNavigateToTab = { tabIndex ->
                    when (tabIndex) {
                        0 -> navController.navigate(Screen.Home.route)
                        1 -> navController.navigate(Screen.Capture.route)
                        2 -> navController.navigate(Screen.Planner.route)
                        3 -> { /* Already on Maps */ }
                        4 -> navController.navigate(Screen.Profile.route)
                    }
                }
            )
        }
        
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToTab = { tabIndex ->
                    when (tabIndex) {
                        0 -> navController.navigate(Screen.Home.route)
                        1 -> navController.navigate(Screen.Capture.route)
                        2 -> navController.navigate(Screen.Planner.route)
                        3 -> navController.navigate(Screen.Maps.route)
                        4 -> { /* Already on Profile */ }
                    }
                }
            )
        }
        
        composable(Screen.Processing.route) {
            ProcessingScreen(
                onProcessingComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Capture.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object Capture : Screen("capture")
    object Planner : Screen("planner")
    object Maps : Screen("maps")
    object Profile : Screen("profile")
    object Processing : Screen("processing")
}