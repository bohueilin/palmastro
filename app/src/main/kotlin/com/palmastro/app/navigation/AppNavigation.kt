package com.palmastro.app.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.palmastro.app.ui.detail.DomainDetailScreen
import com.palmastro.app.ui.explainability.EXPLAINABILITY_ROUTE
import com.palmastro.app.ui.explainability.ExplainabilityScreen
import com.palmastro.app.ui.explainability.explainabilityRoute
import com.palmastro.app.ui.history.HistoryScreen
import com.palmastro.app.ui.journal.JournalScreen
import com.palmastro.app.ui.legal.LEGAL_ROUTE
import com.palmastro.app.ui.legal.LegalViewerScreen
import com.palmastro.app.ui.legal.legalRoute
import com.palmastro.app.ui.onboarding.OnboardingScreen
import com.palmastro.app.ui.results.ResultsScreen
import com.palmastro.app.ui.scan.ScanScreen
import com.palmastro.app.ui.settings.SettingsScreen

sealed class Route(val path: String) {
    data object Onboarding : Route("onboarding")
    data object Results : Route("results?monthKey={monthKey}")
    data object Scan : Route("scan")
    data object Settings : Route("settings")
    data object DomainDetail : Route("domain_detail/{domain}/{monthKey}")
    data object History : Route("history")
    data object Journal : Route("journal/{monthKey}?domain={domain}")
}

@Composable
fun AppNavigation(
    hasProfile: Boolean,
    deepLink: DeepLinkDestination? = null,
    onDeepLinkConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    val startRoute = if (hasProfile) "results" else Route.Onboarding.path

    // Deep links only navigate once a profile exists; otherwise onboarding stays in front.
    LaunchedEffect(deepLink, hasProfile) {
        if (deepLink != null) {
            if (hasProfile) {
                navController.navigate(DeepLinkHandler.routeFor(deepLink)) {
                    launchSingleTop = true
                }
            }
            onDeepLinkConsumed()
        }
    }

    NavHost(navController = navController, startDestination = startRoute) {
        composable(Route.Onboarding.path) {
            OnboardingScreen(onComplete = {
                navController.navigate(Route.Scan.path) {
                    popUpTo(Route.Onboarding.path) { inclusive = true }
                }
            })
        }
        composable(
            route = "results?monthKey={monthKey}",
            arguments = listOf(navArgument("monthKey") {
                type = NavType.StringType; nullable = true; defaultValue = null
            }),
        ) {
            ResultsScreen(
                onScanClick = { navController.navigate(Route.Scan.path) },
                onSettingsClick = { navController.navigate(Route.Settings.path) },
                onDomainClick = { domain, monthKey ->
                    navController.navigate("domain_detail/$domain/$monthKey")
                },
                onHistoryClick = { navController.navigate("history") },
            )
        }
        composable(Route.Scan.path) {
            ScanScreen(onComplete = {
                navController.navigate("results") {
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
                onOpenLegal = { docType -> navController.navigate(legalRoute(docType)) },
            )
        }
        composable(
            route = "domain_detail/{domain}/{monthKey}",
            arguments = listOf(
                navArgument("domain") { type = NavType.StringType },
                navArgument("monthKey") { type = NavType.StringType },
            ),
        ) { entry ->
            val domain = entry.arguments?.getString("domain") ?: ""
            val monthKey = entry.arguments?.getString("monthKey") ?: ""
            DomainDetailScreen(
                onBack = { navController.popBackStack() },
                onJournalClick = { navController.navigate("journal/$monthKey?domain=$domain") },
                onExplainabilityClick = { navController.navigate(explainabilityRoute(domain, monthKey)) },
            )
        }
        composable(
            route = EXPLAINABILITY_ROUTE,
            arguments = listOf(
                navArgument("domain") { type = NavType.StringType },
                navArgument("monthKey") { type = NavType.StringType },
            ),
        ) {
            ExplainabilityScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = LEGAL_ROUTE,
            arguments = listOf(navArgument("docType") { type = NavType.StringType }),
        ) { entry ->
            LegalViewerScreen(
                docType = entry.arguments?.getString("docType") ?: com.palmastro.app.ui.legal.LEGAL_DOC_PRIVACY,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Route.History.path) {
            HistoryScreen(
                onBack = { navController.popBackStack() },
                onMonthClick = { monthKey -> navController.navigate("results?monthKey=$monthKey") },
            )
        }
        composable(
            route = "journal/{monthKey}?domain={domain}",
            arguments = listOf(
                navArgument("monthKey") { type = NavType.StringType },
                navArgument("domain") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) {
            JournalScreen(onBack = { navController.popBackStack() })
        }
    }
}
