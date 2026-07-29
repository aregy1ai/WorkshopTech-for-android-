package com.workshoptech.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.workshoptech.ui.dashboard.DashboardScreen
import com.workshoptech.ui.cases.CaseListScreen
import com.workshoptech.ui.cases.CreateCaseScreen
import com.workshoptech.ui.cases.CaseDetailScreen
import com.workshoptech.ui.customers.CustomerScreen
import com.workshoptech.ui.camera.CameraScreen
import com.workshoptech.ui.splash.SplashScreen
import com.workshoptech.ui.about.AboutScreen
import com.workshoptech.ui.reports.ReportsScreen

object Routes {
    const val SPLASH = "splash"
    const val DASHBOARD = "dashboard"
    const val CASE_LIST = "cases"
    const val CREATE_CASE = "cases/new"
    const val CASE_DETAIL = "cases/{caseId}"
    const val CUSTOMERS = "customers"
    const val CAMERA = "camera"
    const val REPORTS = "reports"
    const val ABOUT = "about"
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) { SplashScreen(onFinished = { navController.navigate(Routes.DASHBOARD) { popUpTo(Routes.SPLASH) { inclusive = true } } }) }
        composable(Routes.DASHBOARD) { DashboardScreen(onNavigateToCases = { navController.navigate(Routes.CASE_LIST) }, onNavigateToCustomers = { navController.navigate(Routes.CUSTOMERS) }, onNavigateToCreateCase = { navController.navigate(Routes.CREATE_CASE) }, onNavigateToReports = { navController.navigate(Routes.REPORTS) }, onNavigateToAbout = { navController.navigate(Routes.ABOUT) }) }
        composable(Routes.CASE_LIST) { CaseListScreen(onNavigateBack = { navController.popBackStack() }, onNavigateToCase = { navController.navigate("cases/$it") }, onNavigateToCreate = { navController.navigate(Routes.CREATE_CASE) }) }
        composable(Routes.CREATE_CASE) { CreateCaseScreen(onNavigateBack = { navController.popBackStack() }, onCaseCreated = { navController.navigate("cases/$it") { popUpTo(Routes.CASE_LIST) { inclusive = true } } }) }
        composable(Routes.CASE_DETAIL, arguments = listOf(navArgument("caseId") { type = NavType.StringType })) { CaseDetailScreen(caseId = it.arguments?.getString("caseId") ?: return@composable, onNavigateBack = { navController.popBackStack() }) }
        composable(Routes.CUSTOMERS) { CustomerScreen(onNavigateBack = { navController.popBackStack() }) }
        composable(Routes.CAMERA) { CameraScreen(onNavigateBack = { navController.popBackStack() }) }
        composable(Routes.REPORTS) { ReportsScreen(stats = com.workshoptech.ui.reports.ReportStats(), onExportPdf = {}, onNavigateBack = { navController.popBackStack() }) }
        composable(Routes.ABOUT) { AboutScreen(onNavigateBack = { navController.popBackStack() }) }
    }
}
