package kang.mingu.remotecomposesample.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kang.mingu.remotecomposesample.ui.screen.RemoteScreen

enum class SampleScreen(val route: String, val title: String, val file: String) {
    Home("home", "Remote Compose", "config.rc"),
    Detail("detail", "Detail", "config_detail.rc"),
    Estimates("estimates", "Estimates", "config_estimates.rc"),
    EstimateDetail("estimate_detail", "Estimate Detail", "config_estimate_detail.rc");

    // Raw GitHub avoids API authentication for this public sample repository.
    val url: String
        get() = "https://raw.githubusercontent.com/kangmin1012/RemoteCompose_Sample_Web/master/$file"
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
