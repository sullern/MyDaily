package com.example.app_daily.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.app_daily.ui.calendar.CalendarScreen
import com.example.app_daily.ui.day.DayScreen
import com.example.app_daily.ui.search.SearchScreen

sealed class Screen(val route: String) {
    object Calendar : Screen("calendar")
    object Search : Screen("search")
    object Settings : Screen("settings")
    // 新增了可选的 cardId 参数
//    object Day : Screen("day/{epochDay}?cardId={cardId}") {
//        fun createRoute(epochDay: Long, cardId: Long? = null): String {
//            return "day/$epochDay?cardId=${cardId ?: -1L}"
//        }
//    }
    object Day : Screen("day/{epochDay}?cardId={cardId}&autoCamera={autoCamera}") {
        fun createRoute(epochDay: Long, cardId: Long? = null, autoCamera: Boolean = false): String {
            return "day/$epochDay?cardId=${cardId ?: -1L}&autoCamera=$autoCamera"
        }
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Calendar.route,
        modifier = modifier
    ) {
        composable(route = Screen.Calendar.route) {
            CalendarScreen(
                onDateClick = { epochDay ->
                    navController.navigate(Screen.Day.createRoute(epochDay))
                }
            )
        }

        composable(route = Screen.Search.route) {
            SearchScreen(
                onResultClick = { epochDay, cardId ->
                    navController.navigate(Screen.Day.createRoute(epochDay, cardId))
                }
            )
        }
        composable(route = Screen.Settings.route) {
            com.example.app_daily.ui.settings.SettingsScreen()
        }

        composable(
            route = Screen.Day.route,
            arguments = listOf(
                navArgument("epochDay") { type = NavType.LongType },
                navArgument("cardId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("autoCamera") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val epochDay = backStackEntry.arguments?.getLong("epochDay") ?: 0L
            val cardId = backStackEntry.arguments?.getLong("cardId") ?: -1L
            val autoCamera = backStackEntry.arguments?.getBoolean("autoCamera") ?: false

            DayScreen(
                epochDay = epochDay,
                initialCardId = if (cardId == -1L) null else cardId,
                autoLaunchCamera = autoCamera,
                onBack = { navController.popBackStack() }
            )
        }
    }
}