package kang.mingu.remotecomposesample

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import kang.mingu.remotecomposesample.ui.components.RemoteDocumentView
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Fixtures are produced by the web repository's JVM converter. */
class RemoteDocumentTest {
    @get:Rule val compose = createComposeRule()

    @Test fun rendersEverySampleBinary() {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val files = listOf("config.rc", "config_detail.rc", "config_estimates.rc", "config_estimate_detail.rc")
        val document = mutableStateOf(assets.open(files.first()).use { it.readBytes() })
        compose.setContent {
            RemoteDocumentView(document.value, Modifier.fillMaxSize())
        }
        files.forEach { file ->
            compose.runOnIdle { document.value = assets.open(file).use { it.readBytes() } }
            compose.waitForIdle()
            val pixels = compose.onRoot().captureToImage().toPixelMap()
            val colors = mutableSetOf<Int>()
            for (x in 0 until pixels.width step 16) {
                for (y in 0 until pixels.height step 16) colors += pixels[x, y].hashCode()
            }
            assertTrue("$file should draw content, not a blank surface", colors.size > 5)
        }
    }
}
