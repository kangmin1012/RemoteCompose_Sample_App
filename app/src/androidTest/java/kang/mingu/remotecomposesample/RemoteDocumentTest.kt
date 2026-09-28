package kang.mingu.remotecomposesample

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.platform.app.InstrumentationRegistry
import kang.mingu.remotecomposesample.ui.components.RemoteDocumentView
import kang.mingu.remotecomposesample.ui.screen.RemoteScreen
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Fixtures are produced by the web repository's JVM converter. */
class RemoteDocumentTest {
    @get:Rule val compose = createComposeRule()

    @Test fun refreshButtonReplacesPreviouslyRenderedDocument() {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val before = assets.open("refresh-before.rc").use { it.readBytes() }
        val after = assets.open("refresh-after.rc").use { it.readBytes() }
        val pending = CompletableDeferred<ByteArray>()
        var calls = 0
        lateinit var model: MainViewModel
        compose.runOnIdle {
            model = MainViewModel(fetchDocument = {
                if (calls++ == 0) before else pending.await()
            }, currentTimeMillis = { 1000L })
        }
        compose.setContent {
            RemoteScreen(configUrl = "https://example.test/config.rc", title = "새로고침 테스트", viewModel = model)
        }
        compose.waitForIdle()
        val oldPixels = compose.onRoot().captureToImage().toPixelMap()
        compose.onNodeWithContentDescription("새로고침").performClick()
        compose.onNodeWithContentDescription("화면 새로고침 중").assertIsDisplayed()
        compose.runOnIdle { pending.complete(after) }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("화면 새로고침 중").assertDoesNotExist()
        val newPixels = compose.onRoot().captureToImage().toPixelMap()
        var changed = 0
        for (x in 0 until newPixels.width step 4) {
            for (y in newPixels.height / 3 until newPixels.height * 5 / 6 step 4) {
                if (oldPixels[x, y] != newPixels[x, y]) changed++
            }
        }
        assertTrue("Newly deployed button must change the rendered content", changed > 100)
        assertTrue(model.uiState.value.documentBytes!!.contentEquals(after))
    }

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
