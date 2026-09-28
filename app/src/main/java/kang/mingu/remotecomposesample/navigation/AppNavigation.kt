package kang.mingu.remotecomposesample.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kang.mingu.remotecomposesample.ui.screen.RemoteScreen

enum class SampleScreen(val route: String, val title: String, val file: String) {
    Home("home", "리모트 컴포즈", "config.rc"),
    Detail("detail", "상세 화면", "config_detail.rc");

    // The manifest and its immutable document are published together by Pages.
    val url: String
        get() = "https://kangmin1012.github.io/RemoteCompose_Sample_Web/${file.removeSuffix(".rc")}.manifest.json"
}

@Composable
fun AppNavigation() {
    val navigation = rememberNavController()
    NavHost(navController = navigation, startDestination = SampleScreen.Home.route) {
        SampleScreen.entries.forEach { screen ->
            composable(screen.route) {
                RemoteScreen(
                    configUrl = screen.url,
                    title = screen.title,
                    showBack = screen != SampleScreen.Home,
                    onBack = { navigation.popBackStack() },
                    onNavigate = { route ->
                        if (SampleScreen.entries.any { it.route == route }) {
                            navigation.navigate(route) { launchSingleTop = true }
                        }
                    },
                )
            }
        }
    }
}
