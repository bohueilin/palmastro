package com.palmastro.app.navigation

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.palmastro.app.ui.onboarding.OnboardingScreen
import com.palmastro.app.ui.results.ResultsScreen
import com.palmastro.app.ui.scan.ScanScreen
import com.palmastro.app.ui.settings.SettingsScreen

sealed class Route(val path: String) {
    data object Onboarding : Route("onboarding")
    data object Results : Route("results")
    data object Scan : Route("scan")
    data object Settings : Route("settings")
}

@Composable
fun AppNavigation(hasProfile: Boolean) {
    val navController = rememberNavController()
    val startRoute = if (hasProfile) Route.Results.path else Route.Onboarding.path

    NavHost(navController = navController, startDestination = startRoute) {
        composable(Route.Onboarding.path) {
            OnboardingScreen(onComplete = {
                navController.navigate(Route.Scan.path) {
                    popUpTo(Route.Onboarding.path) { inclusive = true }
                }
            })
        }
        composable(Route.Results.path) {
            ResultsScreen(
                onScanClick = { navController.navigate(Route.Scan.path) },
                onSettingsClick = { navController.navigate(Route.Settings.path) },
            )
        }
        composable(Route.Scan.path) {
            ScanScreen(onComplete = {
                navController.navigate(Route.Results.path) {
                    popUpTo(Route.Scan.path) { inclusive = true }
                }
            })
        }
        composable(Route.Settings.path) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onWipeComplete = {
                    navController.navigate(Route.Onboarding.path) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
