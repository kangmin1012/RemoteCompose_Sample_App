package kang.mingu.remotecomposesample

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import kang.mingu.remotecomposesample.ui.screen.RemoteScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ScreenResumeTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun resumingExistingActivityFetchesAgain() {
        val bytes = InstrumentationRegistry.getInstrumentation().context.assets.open("config.rc").use { it.readBytes() }
        var calls = 0
        lateinit var model: MainViewModel
        compose.runOnIdle { model = MainViewModel(fetchDocument = { calls++; bytes }) }
        compose.setContent { RemoteScreen("https://example.test/config.manifest.json", "복귀 테스트", viewModel = model) }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(1, calls) }
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(2, calls) }
    }
}
