package kang.mingu.remotecomposesample

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.toArgb
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
        val files = listOf("config.rc", "config_detail.rc")
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

    @Test fun cardBordersKeepTheirColorAndConfiguredThickness() {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val cases = listOf(
            "card-border-thin.rc" to 1,
            "card-border-thick.rc" to 4,
            "card-border-square.rc" to 4,
        )
        val document = mutableStateOf(assets.open(cases.first().first).use { it.readBytes() })
        compose.setContent { RemoteDocumentView(document.value, Modifier.fillMaxSize()) }
        cases.forEach { (file, widthDp) ->
            compose.runOnIdle { document.value = assets.open(file).use { it.readBytes() } }
            compose.waitForIdle()
            val pixels = compose.onRoot().captureToImage().toPixelMap()
            val y = pixels.height / 2
            val blue = (0 until pixels.width).filter { x ->
                val c = pixels[x, y]
                c.blue > 0.9f && c.red < 0.1f && c.green < 0.1f
            }
            // The converter currently records pixels at 2.625 px/dp. Sample
            // both straight edges, away from antialiased rounded corners.
            val expected = widthDp * 2.625f
            val left = blue.count { it < pixels.width / 2 }
            val right = blue.count { it > pixels.width / 2 }
            assertTrue("$file: left border $left px, expected ~$expected", kotlin.math.abs(left - expected) <= 1.5f)
            assertTrue("$file: right border $right px, expected ~$expected", kotlin.math.abs(right - expected) <= 1.5f)
            assertTrue("$file: background must remain red", pixels[pixels.width / 2, y].toArgb() == 0xFFFF0000.toInt())
        }
    }
}
