package com.nothopeless.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nothopeless.app.ui.guidelines.GuidelinesScreen
import com.nothopeless.app.ui.home.HomeScreen
import com.nothopeless.app.ui.my.MyScreen
import com.nothopeless.app.ui.onboarding.OnboardingScreen
import com.nothopeless.app.ui.post.PostScreen
import com.nothopeless.app.ui.report.ReportBottomSheet
import com.nothopeless.app.ui.theme.NotHopelessTheme

private const val ONBOARDING = "onboarding"
private const val HOME = "home"
private const val POST = "post"
private const val MY = "my"
private const val GUIDELINES = "guidelines"

data class BottomTab(val route: String, val label: String, val icon: @Composable () -> Unit)

val bottomTabs = listOf(
    BottomTab(HOME, "ホーム") { Icon(Icons.Default.Home, null) },
    BottomTab(POST, "投稿") { Icon(Icons.Default.Edit, null) },
    BottomTab(MY, "マイ") { Icon(Icons.Default.Person, null) },
)

@Composable
fun AppRoot(startDestination: String = HOME) {
    NotHopelessTheme {
        val navController = rememberNavController()
        var reportPostId by remember { mutableStateOf<String?>(null) }
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val showBottomBar = currentRoute in listOf(HOME, POST, MY)

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        bottomTabs.forEach { tab ->
                            NavigationBarItem(
                                icon = tab.icon,
                                label = { Text(tab.label) },
                                selected = navBackStackEntry?.destination?.hierarchy
                                    ?.any { it.route == tab.route } == true,
                                onClick = {
                                    navController.navigate(tab.route) {
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
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(padding),
            ) {
                composable(ONBOARDING) {
                    OnboardingScreen(
                        onCompleted = {
                            navController.navigate(HOME) {
                                popUpTo(ONBOARDING) { inclusive = true }
                            }
                        }
                    )
                }
                composable(HOME) {
                    HomeScreen(
                        onNavigateToGuidelines = { navController.navigate(GUIDELINES) },
                        onReport = { postId -> reportPostId = postId },
                    )
                }
                composable(POST) {
                    PostScreen(
                        onPostSuccess = {
                            navController.navigate(HOME) {
                                popUpTo(HOME) { inclusive = false }
                            }
                        }
                    )
                }
                composable(MY) {
                    MyScreen(onReport = { postId -> reportPostId = postId })
                }
                composable(GUIDELINES) {
                    GuidelinesScreen(onBack = { navController.popBackStack() })
                }
            }
        }

        // ReportBottomSheet（グローバル）
        reportPostId?.let { postId ->
            ReportBottomSheet(
                postId = postId,
                onDismiss = { reportPostId = null },
            )
        }
    }
}
