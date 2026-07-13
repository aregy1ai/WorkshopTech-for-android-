package com.workshoptech.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.workshoptech.WorkshopTechApp
import com.workshoptech.viewmodel.ViewModelFactory
import com.workshoptech.ui.dashboard.DashboardScreen
import com.workshoptech.ui.cases.CaseListScreen
import com.workshoptech.ui.cases.CreateCaseScreen
import com.workshoptech.ui.cases.CaseDetailScreen
import com.workshoptech.ui.camera.CameraScreen
import com.workshoptech.ui.inspection.InspectionScreen
import com.workshoptech.ui.customers.CustomerScreen
import com.workshoptech.ui.inventory.InventoryScreen
import com.workshoptech.ui.settings.SettingsScreen
import com.workshoptech.ui.colors.ColorMatchScreen
import com.workshoptech.ui.help.HelpScreen
import com.workshoptech.ui.splash.SplashScreen
import com.workshoptech.ui.onboarding.OnboardingScreen

object Route {
    const val SPLASH         = "splash"
    const val ONBOARDING     = "onboarding"
    const val DASHBOARD      = "dashboard"
    const val CASE_LIST      = "cases"
    const val CASE_CREATE    = "cases/create"
    const val CASE_DETAIL    = "cases/{caseId}"
    const val CAMERA         = "camera/{caseId}?mode={mode}"
    const val INSPECTION     = "inspection/{caseId}"
    const val CUSTOMERS      = "customers"
    const val INVENTORY      = "inventory"
    const val SETTINGS       = "settings"
    const val COLOR_MATCH    = "colors"
    const val HELP           = "help"

    fun caseDetail(caseId: String)  = "cases/$caseId"
    fun camera(caseId: String, mode: String = "general") = "camera/$caseId?mode=$mode"
    fun inspection(caseId: String)  = "inspection/$caseId"
}

@Composable
fun NavGraph(
    deepLinkCaseId: String?   = null,
    deepLinkInspType: String? = null,
    onDarkModeToggle: (Boolean) -> Unit = {}
) {
    val navController = rememberNavController()
    val factory = remember {
        ViewModelFactory(WorkshopTechApp.get().container.repository)
    }

    val startDest = Route.SPLASH

    NavHost(navController = navController, startDestination = startDest) {

        composable(Route.SPLASH) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Route.DASHBOARD) {
                        popUpTo(Route.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.ONBOARDING) {
            OnboardingScreen(
                onOnboardingFinished = {
                    navController.navigate(Route.DASHBOARD) {
                        popUpTo(Route.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.DASHBOARD) {
            DashboardScreen(
                viewModel = viewModel(factory = factory),
                onNavigateToCases      = { navController.navigate(Route.CASE_LIST) },
                onNavigateToCase       = { id -> navController.navigate(Route.caseDetail(id)) },
                onNavigateToCustomers  = { navController.navigate(Route.CUSTOMERS) },
                onNavigateToInventory  = { navController.navigate(Route.INVENTORY) },
                onNavigateToSettings   = { navController.navigate(Route.SETTINGS) }
            )
        }

        composable(Route.CASE_LIST) {
            CaseListScreen(
                viewModel  = viewModel(factory = factory),
                onCreateCase  = { navController.navigate(Route.CASE_CREATE) },
                onOpenCase    = { id -> navController.navigate(Route.caseDetail(id)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Route.CASE_CREATE) {
            CreateCaseScreen(
                viewModel  = viewModel(factory = factory),
                onCaseCreated  = { id ->
                    navController.navigate(Route.caseDetail(id)) {
                        popUpTo(Route.CASE_CREATE) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Route.CASE_DETAIL,
            arguments = listOf(navArgument("caseId") { type = NavType.StringType })
        ) { backStack ->
            val caseId = backStack.arguments?.getString("caseId") ?: return@composable
            CaseDetailScreen(
                caseId         = caseId,
                viewModel      = viewModel(factory = factory),
                onTakePhoto    = { id, mode -> navController.navigate(Route.camera(id, mode)) },
                onInspect      = { id -> navController.navigate(Route.inspection(id)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "camera/{caseId}?mode={mode}",
            arguments = listOf(
                navArgument("caseId") { type = NavType.StringType },
                navArgument("mode")   { type = NavType.StringType; defaultValue = "general" }
            )
        ) { backStack ->
            val caseId = backStack.arguments?.getString("caseId") ?: return@composable
            val mode   = backStack.arguments?.getString("mode")   ?: "general"
            CameraScreen(
                caseId         = caseId,
                mode           = mode,
                onImageCaptured = { _ -> navController.popBackStack() },
                onBack         = { navController.popBackStack() }
            )
        }

        composable(
            route = Route.INSPECTION,
            arguments = listOf(navArgument("caseId") { type = NavType.StringType })
        ) { backStack ->
            val caseId = backStack.arguments?.getString("caseId") ?: return@composable
            InspectionScreen(
                caseId         = caseId,
                viewModel      = viewModel(factory = factory),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Route.CUSTOMERS) {
            CustomerScreen(
                viewModel      = viewModel(factory = factory),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Route.INVENTORY) {
            InventoryScreen(
                viewModel      = viewModel(factory = factory),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Route.SETTINGS) {
            SettingsScreen(
                onNavigateBack   = { navController.popBackStack() },
                onDarkModeToggle = onDarkModeToggle
            )
        }

        composable(Route.COLOR_MATCH) {
            ColorMatchScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Route.HELP) {
            HelpScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
