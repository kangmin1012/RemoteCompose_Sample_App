package kang.mingu.remotecomposesample

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import kang.mingu.remotecomposesample.data.remote.RemoteConfigFetcher
import kang.mingu.remotecomposesample.navigation.SampleScreen
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Opt-in public deployment smoke test; ordinary test runs stay offline. */
class PublishedScreenTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun publishedDocumentLoadsOnLaunchRefreshResumeAndRecreation() {
        val hash = InstrumentationRegistry.getArguments().getString("expectedDocumentSha256")
        assumeTrue("Pass expectedDocumentSha256 to test a real deployment", hash != null)
        val bytes = runBlocking { RemoteConfigFetcher.fetchPublishedDocument(SampleScreen.Home.url) }
        assertEquals(hash, RemoteConfigFetcher.sha256(bytes))
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            fun assertPublished() {
                compose.waitUntil(20000) {
                    compose.onAllNodesWithText("화면 ${hash!!.take(8)}", substring = true)
                        .fetchSemanticsNodes().isNotEmpty()
                }
                compose.waitForIdle()
            }
            assertPublished()
            compose.onNodeWithContentDescription("새로고침").performClick()
            compose.waitUntil(20000) {
                compose.onAllNodesWithText("최근 확인:", substring = true).fetchSemanticsNodes().isNotEmpty() &&
                    compose.onAllNodesWithContentDescription("화면 새로고침 중").fetchSemanticsNodes().isEmpty()
            }
            assertPublished()
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            assertPublished()
            scenario.recreate()
            assertPublished()
            val file = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "published-screen.png")
            file.outputStream().use {
                compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }
}
