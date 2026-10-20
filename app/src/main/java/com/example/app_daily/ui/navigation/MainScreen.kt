package com.example.app_daily.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    // 暂时只保留两个标签
    val navItems = listOf(
        "日历" to Icons.Default.DateRange,
        "搜索" to Icons.Default.Search,
        "设置" to Icons.Default.Settings
    )

    val context = androidx.compose.ui.platform.LocalContext.current
    val prefsRepo = remember { com.example.app_daily.data.repository.UserPreferencesRepository(context) }
    val autoOpenCamera by prefsRepo.autoOpenCameraFlow.collectAsState(initial = false)

    // 新增：启动时检查
    LaunchedEffect(Unit) {
        if (autoOpenCamera) {
            val today = java.time.LocalDate.now().toEpochDay()
            // 延迟一点点，确保导航系统准备好了
            kotlinx.coroutines.delay(300)
            navController.navigate(Screen.Day.createRoute(today, autoCamera = true))
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val selectedTab = when (currentRoute) {
        Screen.Calendar.route -> 0
        Screen.Search.route -> 1
        Screen.Settings.route -> 2
        else -> 0
    }

    Scaffold(
        // 背景色使用画布底色
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // 只有在两个主页面才显示底部导航
            if (currentRoute in listOf(Screen.Calendar.route, Screen.Search.route)) {
                Column {
                    // GitHub 风格的核心：顶部加一条极细的分割线，代替阴影
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
                    NavigationBar(
                        // 导航栏底色用纯白/深灰，去掉默认的阴影
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        navItems.forEachIndexed { index, (title, icon) ->
                            NavigationBarItem(
                                icon = { Icon(icon, contentDescription = title) },
                                label = { Text(title) },
                                selected = selectedTab == index,
                                onClick = {
                                    if (selectedTab != index) {
                                        val targetRoute = when (index) {
                                            0 -> Screen.Calendar.route
                                            1 -> Screen.Search.route
                                            2 -> Screen.Settings.route // 加回了点击 2 的跳转
                                            else -> Screen.Calendar.route
                                        }
                                        navController.navigate(targetRoute) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                // 极简颜色配置：去掉默认的圆角背景框
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.background.copy(alpha = 0f)
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        AppNavigation(
            navController = navController,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        )
    }
}