package kang.mingu.remotecomposesample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kang.mingu.remotecomposesample.navigation.AppNavigation
import kang.mingu.remotecomposesample.ui.theme.RemoteComposeSampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RemoteComposeSampleTheme {
                AppNavigation()
            }
        }
    }
}
